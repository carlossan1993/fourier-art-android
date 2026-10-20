#!/usr/bin/env python3
"""Independent host validation of synthetic Stage 6 evidence (requires Pillow, ffprobe and ffmpeg)."""
import argparse
import json
import subprocess
from pathlib import Path
from PIL import Image


def verify(folder):
    results = []
    for frame, png_size, gif_size, mp4_size in [
        ("square", (1080, 1080), (480, 480), (720, 720)),
        ("vertical", (1080, 1920), (360, 640), (720, 1280)),
    ]:
        base = folder / f"stage6-{frame}"
        with Image.open(base.with_suffix(".png")) as image:
            assert image.size == png_size and image.format == "PNG"
            assert len(image.convert("RGB").getcolors(image.width * image.height)) > 10
            results.append(dict(format="PNG", frame=frame, size=image.size))
        with Image.open(base.with_suffix(".gif")) as image:
            assert image.size == gif_size and image.n_frames == 72
            assert image.info["loop"] == 0
            duration = 0
            first = last = None
            for index in range(image.n_frames):
                image.seek(index)
                duration += image.info["duration"]
                rgb = image.convert("RGB").tobytes()
                if index == 0:
                    first = rgb
                if index >= 63:
                    if last is not None:
                        assert rgb == last, "Final hold frame changed"
                    last = rgb
            assert duration == 6000 and first != last
            results.append(dict(format="GIF", frame=frame, size=image.size, frames=image.n_frames, durationMs=duration))
        video = base.with_suffix(".mp4")
        data = json.loads(subprocess.check_output([
            "ffprobe", "-v", "error", "-count_frames", "-show_streams", "-show_format", "-of", "json", str(video)
        ]))
        assert len(data["streams"]) == 1, "Unexpected audio or extra track"
        stream = data["streams"][0]
        assert stream["codec_name"] == "h264"
        assert (stream["width"], stream["height"]) == mp4_size
        assert stream["r_frame_rate"] == "24/1" and int(stream["nb_read_frames"]) == 144
        assert abs(float(data["format"]["duration"]) - 6) < .1
        subprocess.run(["ffmpeg", "-v", "error", "-xerror", "-i", str(video), "-f", "null", "-"], check=True)
        results.append(dict(format="MP4", frame=frame, size=mp4_size, frames=int(stream["nb_read_frames"]),
                            duration=data["format"]["duration"], codec=stream["codec_name"]))
    return results


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("folder", type=Path, help="quality/exports directory from CI evidence")
    print(json.dumps(verify(parser.parse_args().folder), indent=2))
