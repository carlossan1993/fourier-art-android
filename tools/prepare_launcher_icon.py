#!/usr/bin/env python3
"""Derive Android launcher assets from the approved, unchanged Play PNG."""
from pathlib import Path
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'store/graphics/icon-512.png'
BACKGROUND = '#000D2E'


def generate():
    with Image.open(SOURCE) as source:
        assert source.size == (512, 512), 'Approved icon must be 512 x 512'
        image = source.convert('RGBA')
    res = ROOT / 'app/src/main/res'
    for density, size in [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96),
                          ('xxhdpi', 144), ('xxxhdpi', 192)]:
        dest = res / f'mipmap-{density}/ic_launcher.png'
        dest.parent.mkdir(parents=True, exist_ok=True)
        image.resize((size, size), Image.Resampling.LANCZOS).save(dest)

    # Android adaptive foregrounds use a 108 dp canvas. Fit the complete art
    # in its central 72 dp; the luminous circle stays inside the 66 dp safe zone.
    nodpi = res / 'drawable-nodpi'
    nodpi.mkdir(parents=True, exist_ok=True)
    image.save(nodpi / 'ic_fourier_art.png')
    foreground = res / 'drawable/ic_fourier.xml'
    foreground.write_text('''<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:inset="16.666667%">
    <bitmap android:src="@drawable/ic_fourier_art" android:gravity="fill" android:filter="true"/>
</inset>
''')

    # Preserve the approved profile and epicycles in themed monochrome mode.
    alpha = image.convert('L').point(lambda value: max(0, min(255, (value - 40) * 3)))
    alpha = ImageChops.multiply(alpha, image.getchannel('A'))
    mono = Image.new('RGBA', (432, 432), (255, 255, 255, 0))
    symbol = Image.new('RGBA', image.size, 'white')
    symbol.putalpha(alpha)
    mono.alpha_composite(symbol.resize((288, 288), Image.Resampling.LANCZOS), (72, 72))
    mono.save(nodpi / 'ic_fourier_monochrome.png')
    (res / 'values/icon_colors.xml').write_text(
        f'<resources><color name="icon_background">{BACKGROUND}</color></resources>\n')

    # Remove obsolete resources that would override density-specific PNGs.
    for old in [res / 'mipmap-anydpi/ic_launcher.xml',
                res / 'drawable/ic_fourier_monochrome.xml',
                ROOT / 'store/graphics/icon.svg']:
        old.unlink(missing_ok=True)


if __name__ == '__main__':
    generate()
