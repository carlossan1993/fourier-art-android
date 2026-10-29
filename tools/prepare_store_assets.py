#!/usr/bin/env python3
"""Generate repo-native icon/feature graphics and matching offline/public privacy text."""
import html
import json
import math
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
NAVY, AQUA, WHITE = '#06131E', '#35D4DE', '#F4FAFF'

def write(path, text):
    path = ROOT / path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')

def curve(cx, cy, scale, steps=420):
    # A closed curve defined by three rotating vectors: z=e^it+.34e^-3it+.16e^5it.
    points=[]
    for i in range(steps + 1):
        t=2*math.pi*i/steps
        x=math.cos(t)+.34*math.cos(-3*t)+.16*math.cos(5*t)
        y=math.sin(t)+.34*math.sin(-3*t)+.16*math.sin(5*t)
        points.append((cx+scale*x,cy+scale*y))
    return 'M'+' L'.join(f'{x:.3f},{y:.3f}' for x,y in points)

def generate():
    import cairosvg
    # The approved PNG is authoritative; never recreate the retired cyan star.
    import sys
    sys.path.insert(0, str(ROOT / 'tools'))
    from prepare_launcher_icon import generate as generate_launcher_icon
    from PIL import Image
    generate_launcher_icon()
    for lang,tagline in [('es','Tus fotos. Dibujos en movimiento.'),('en','Your photos. Drawings in motion.')]:
        svg=f'''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500"><rect width="1024" height="500" fill="{NAVY}"/><circle cx="790" cy="250" r="178" fill="#102532"/><path d="{curve(790,250,100)}" fill="none" stroke="{AQUA}" stroke-width="5" stroke-linejoin="round"/><text x="68" y="190" fill="{WHITE}" font-size="66" font-family="DejaVu Sans" font-weight="bold">FOURIER</text><text x="68" y="262" fill="{AQUA}" font-size="66" font-family="DejaVu Sans" font-weight="bold">ART</text><text x="72" y="338" fill="#AFBFCD" font-size="24" font-family="DejaVu Sans">{tagline}</text><text x="72" y="380" fill="#AFBFCD" font-size="20" font-family="DejaVu Sans">PNG · GIF · MP4</text></svg>'''
        write(f'store/graphics/feature-{lang}.svg',svg)
        dest=ROOT/f'store/graphics/feature-{lang}-1024x500.png'
        cairosvg.svg2png(bytestring=svg.encode(),write_to=str(dest))
        Image.open(dest).convert('RGB').save(dest)
    data=json.loads((ROOT/'store/privacy.json').read_text())
    for lang,content in data['languages'].items():
        resources=ET.Element('resources')
        ET.SubElement(resources,'string',name='privacy_policy_title').text=content['title']
        ET.SubElement(resources,'string',name='sdk_privacy_summary').text=content['summary']
        ET.SubElement(resources,'string',name='privacy_policy_date').text=data['date']
        for name,index in [('privacy_section_titles',0),('privacy_section_bodies',1)]:
            arr=ET.SubElement(resources,'string-array',name=name)
            for section in content['sections']:
                # Android resource syntax treats apostrophes specially, independently of XML escaping.
                ET.SubElement(arr,'item').text=section[index].replace("'", "\\'")
        ET.indent(resources,space='    ')
        values='values-es' if lang=='es' else 'values'
        write(f'app/src/main/res/{values}/privacy.xml','<?xml version="1.0" encoding="utf-8"?>\n'+ET.tostring(resources,encoding='unicode')+'\n')
        sections=''.join(f'<section><h2>{html.escape(t)}</h2><p>{html.escape(b)}</p></section>' for t,b in content['sections'])
        page=f'''<!doctype html><html lang="{lang}"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>FOURIER ART — {content['title']}</title><style>body{{max-width:760px;margin:40px auto;padding:0 22px;background:{NAVY};color:{WHITE};font:17px/1.65 system-ui}}h1,h2,a{{color:{AQUA}}}h2{{font-size:1.2em}}nav{{margin-bottom:2em}}footer{{margin-top:3em;color:#AFBFCD}}</style><nav><a href="privacy-es.html">Español</a> · <a href="privacy-en.html">English</a></nav><main><h1>FOURIER ART</h1><h2>{content['title']}</h2><p>{data['date']}</p>{sections}</main><footer>FOURIER ART · Android</footer></html>'''
        write(f'docs/site/privacy-{lang}.html',page)
    write('docs/site/index.html','<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>FOURIER ART · Privacidad / Privacy</title><h1>FOURIER ART</h1><p><a href="privacy-es.html">Política de privacidad</a></p><p><a href="privacy-en.html">Privacy policy</a></p></html>\n')

if __name__=='__main__':
    generate()
