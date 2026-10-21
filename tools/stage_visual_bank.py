"""Reproduce the frozen input bytes; abort on any changed remote source."""
import concurrent.futures
import hashlib
import io
import json
from pathlib import Path
import shutil
import time
import urllib.request
from PIL import Image, ImageOps

root = Path(__file__).resolve().parents[1]
bank = root / 'quality-bank'
manifest = json.loads((bank / 'manifest.json').read_text())
destination = root / 'app/src/androidTest/assets/visual-bank'
destination.mkdir(parents=True, exist_ok=True)
shutil.copy(bank / 'manifest.json', destination)

def stage(case):
    local = bank / 'inputs' / case['file']
    if local.exists():
        raw_input = local.read_bytes()
    else:
        for attempt in range(6):
            try:
                req = urllib.request.Request(case['download_url'], headers={'User-Agent': 'FourierArtQualityReview/1.0'})
                with urllib.request.urlopen(req, timeout=90) as response:
                    raw = response.read()
                break
            except Exception:
                if attempt == 5:
                    raise
                time.sleep(3 * (attempt + 1))
        assert hashlib.sha256(raw).hexdigest() == case['download_sha256'], case['id'] + ': source changed'
        image = ImageOps.exif_transpose(Image.open(io.BytesIO(raw))).convert('RGBA')
        image.thumbnail((1024, 1024), Image.Resampling.LANCZOS)
        white = Image.new('RGB', image.size, 'white')
        white.paste(image, mask=image.getchannel('A'))
        stream = io.BytesIO()
        white.save(stream, format='JPEG', quality=95, subsampling=0)
        raw_input = stream.getvalue()
    assert hashlib.sha256(raw_input).hexdigest() == case['sha256'], case['id'] + ': normalized input changed'
    (destination / case['file']).write_bytes(raw_input)
    print(case['id'] + ' verified', flush=True)

assert len(manifest['cases']) == 30
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    list(pool.map(stage, manifest['cases']))
