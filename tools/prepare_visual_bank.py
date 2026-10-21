"""Fetch a fixed, licensed Commons bank. Selection precedes any engine output.

Run once to freeze normalized inputs and provenance, not as part of Android CI.
Only CC0 / public-domain images are accepted. Pillow is required.
"""
import concurrent.futures
import hashlib
import html
import io
import json
from pathlib import Path
import re
import time
import urllib.parse
import urllib.request
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
BANK = ROOT / 'quality-bank'
SELECTION = [
    ('P01', 'personas', 'AUTO', 'Charles Bolden astronaut photo.jpg'),
    ('P02', 'personas', 'AUTO', 'Official portrait of astronaut Linda M. Godwin.jpg'),
    ('P03', 'personas', 'AUTO', 'Susan J. Helms - Official portrait of astronaut candidate.jpg'),
    ('P04', 'personas', 'AUTO', 'Janice Voss.jpg'),
    ('P05', 'personas', 'AUTO', 'Marsha Ivins.jpg'),
    ('P06', 'personas', 'AUTO', 'Samantha Cristoforetti official portrait in an EMU spacesuit.jpg'),
    ('P07', 'personas', 'AUTO', 'Jim Lovell official 1964 portrait.jpg'),
    ('P08', 'personas', 'AUTO', 'The official portrait of the Expedition 73 crew (iss073-s-002).jpg'),
    ('P09', 'personas', 'AUTO', 'Mark and Scott Kelly at the Johnson Space Center, Houston Texas - profile.jpg'),
    ('P10', 'personas', 'AUTO', 'Tracy Caldwell Dyson in Cupola ISS.jpg'),
    ('M01', 'mascotas', 'SUBJECT', 'Boxer (dog) , Iran 08.jpg'),
    ('M02', 'mascotas', 'SUBJECT', 'Dog at Nørre Vorupør Strand.jpg'),
    ('M03', 'mascotas', 'SUBJECT', 'Dog in a muzzle.jpg'),
    ('M04', 'mascotas', 'SUBJECT', 'Mixed-breed dogs in Serles.jpg'),
    ('M05', 'mascotas', 'SUBJECT', 'Panting-dog-No.3.jpg'),
    ('M06', 'mascotas', 'SUBJECT', 'Closeup photo of a cat.jpg'),
    ('M07', 'mascotas', 'SUBJECT', 'Cat - Moomin.jpg'),
    ('M08', 'mascotas', 'SUBJECT', 'Cat in towel.jpg'),
    ('M09', 'mascotas', 'SUBJECT', 'Cat-on-couch.jpg'),
    ('M10', 'mascotas', 'SUBJECT', 'Cat in backyard.jpg'),
    ('O01', 'objetos', 'SUBJECT', 'Coffee cup seen from above.jpg'),
    ('O02', 'objetos', 'SUBJECT', 'Leonardo\'s bicycle.jpg'),
    ('O03', 'objetos', 'SUBJECT', 'Large photographic lens with a camera.jpg'),
    ('O04', 'objetos', 'SUBJECT', 'Arm Chair MET cds164235.jpg'),
    ('O05', 'objetos', 'SUBJECT', 'Consumer\'s Rest chair, V&A Museum 01.jpg'),
    ('O06', 'objetos', 'DRAWING', 'Honey Bee Line Drawing.jpg'),
    ('O07', 'objetos', 'DRAWING', 'A line drawing of a lion with wings (public domain).jpg'),
    ('O08', 'objetos', 'DRAWING', 'Bouquet (flowers).jpg'),
    ('O09', 'objetos', 'DRAWING', 'Bicycle (453586) - The Noun Project.svg'),
    ('O10', 'objetos', 'DRAWING', 'Camera by donchico.svg'),
]

def fetch(url):
    for attempt in range(4):
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'FourierArtQualityReview/1.0 (public image benchmark)'})
            with urllib.request.urlopen(req, timeout=90) as response:
                return response.read()
        except Exception:
            if attempt == 3:
                raise
            time.sleep(2 ** attempt)

def plain(value):
    return html.unescape(re.sub('<[^>]+>', '', value)).strip()

def prepare(case):
    ident, category, mode, name = case
    cache = BANK / 'sources' / f'{ident}.json'
    if cache.exists() and (BANK / 'inputs' / f'{ident}.jpg').exists():
        return json.loads(cache.read_text())
    query = urllib.parse.urlencode(dict(action='query', format='json', titles='File:' + name,
        prop='imageinfo|categories', cllimit='max', iiprop='url|extmetadata|sha1|size', iiurlwidth=1024))
    data = json.loads(fetch('https://commons.wikimedia.org/w/api.php?' + query))
    page = next(iter(data['query']['pages'].values()))
    info = page['imageinfo'][0]
    meta = info['extmetadata']
    license_name = plain(meta.get('LicenseShortName', {}).get('value', ''))
    if license_name not in ('CC0', 'Public domain'):
        raise ValueError(f'{ident}: license {license_name!r} requires review')
    categories = [x['title'] for x in page.get('categories', [])]
    if any('AI-generated' in x or 'Artificial intelligence' in x for x in categories):
        raise ValueError(f'{ident}: AI-generated candidate')
    url = info.get('thumburl', info['url'])
    raw = fetch(url)
    image = ImageOps.exif_transpose(Image.open(io.BytesIO(raw))).convert('RGBA')
    image.thumbnail((1024, 1024), Image.Resampling.LANCZOS)
    white = Image.new('RGB', image.size, 'white')
    white.paste(image, mask=image.getchannel('A'))
    dest = BANK / 'inputs' / f'{ident}.jpg'
    white.save(dest, quality=95, subsampling=0)
    record = dict(id=ident, category=category, mode=mode, title=page['title'],
        source=info['descriptionurl'], original_url=info['url'], download_url=url,
        license=license_name, license_url=meta.get('LicenseUrl', {}).get('value', ''),
        author=plain(meta.get('Artist', {}).get('value', '')), description=plain(meta.get('ImageDescription', {}).get('value', '')),
        commons_sha1=info['sha1'], download_sha256=hashlib.sha256(raw).hexdigest(),
        file=f'{ident}.jpg', sha256=hashlib.sha256(dest.read_bytes()).hexdigest(),
        width=white.width, height=white.height, categories=categories)
    cache.write_text(json.dumps(record, ensure_ascii=False, indent=2) + '\n')
    print(f'{ident}: {license_name}, {white.size}, {name}', flush=True)
    return record

if __name__ == '__main__':
    (BANK / 'inputs').mkdir(parents=True, exist_ok=True)
    (BANK / 'sources').mkdir(exist_ok=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(prepare, SELECTION))
    manifest = dict(frozen_before_processing=True, selected_on='2026-10-20',
        engine='0.12.0', production_commit='5d2f3c8f4679d637a6a17642e41a49e43540ab36',
        settings=dict(sensitivity=50, detail='BALANCED', style='STRUCTURE', components=400),
        normalization='Commons thumbnail <=1024, EXIF transpose, alpha on white, JPEG quality95 subsampling0; no cropping', cases=results)
    (BANK / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
