"""Make side-by-side sheets without modifying the engine output.

Usage: python3 tools/review_visual_bank.py PATH_TO_EXTRACTED_ARTIFACT
Ratings are intentionally entered separately after visual inspection.
"""
import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageOps

parser = argparse.ArgumentParser()
parser.add_argument('artifact', type=Path)
parser.add_argument('--destination', type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
manifest = json.loads((root / 'quality-bank/manifest.json').read_text())
records = {r['id']: r for r in json.loads((args.artifact / 'results.json').read_text())}
destination = args.destination or root / 'quality-bank/review'
destination.mkdir(parents=True, exist_ok=True)
font_path = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
font = ImageFont.truetype(font_path, 21)
small = ImageFont.truetype(font_path, 17)
cell, header = 640, 68
rows = []
for case in manifest['cases']:
    ident = case['id']
    original = args.artifact / 'inputs' / case['file']
    if not original.exists():
        original = root / 'quality-bank/inputs' / case['file']
    assert hashlib.sha256(original.read_bytes()).hexdigest() == case['sha256'], ident
    row = Image.new('RGB', (cell * 2, cell + header), '#e2e8f0')
    draw = ImageDraw.Draw(row)
    record = records[ident]
    actual_mode = record.get('mode', case['mode'])
    draw.text((16, 8), f'{ident}  {case["category"]} / {actual_mode}', fill='#101b2b', font=font)
    draw.text((16, 38), 'Original (sin recorte)', fill='#101b2b', font=small)
    draw.text((cell+16, 38), f'Fourier 400 / {record.get("kind", "ERROR")} / {record.get("strokes", 0)} trazos', fill='#101b2b', font=small)
    for index, path in enumerate([original, args.artifact / f'{ident}.png']):
        if path.exists():
            im = ImageOps.contain(Image.open(path).convert('RGB'), (cell, cell))
            row.paste(im, (index*cell + (cell-im.width)//2, header+(cell-im.height)//2))
        else:
            draw.rectangle((cell, header, cell*2, cell+header), fill='#fffbf2')
            draw.text((cell+30, header+cell//2), 'Sin dibujo: ' + record.get('kind', record.get('error', '')), fill='#a3224c', font=small)
    row.save(destination / f'{ident}.jpg', quality=92, subsampling=0)
    rows.append(row)
for start in range(0, len(rows), 2):
    page = Image.new('RGB', (cell*2, (cell+header)*2), '#e2e8f0')
    for index, row in enumerate(rows[start:start+2]):
        page.paste(row, (0, index*(cell+header)))
    page.save(destination / f'sheet-{start//2+1:02}.jpg', quality=92, subsampling=0)
print(f'Created {len(rows)} pairs and {len(rows)//2} sheets in {destination}')
