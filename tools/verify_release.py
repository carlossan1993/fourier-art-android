#!/usr/bin/env python3
"""Audit actual release contents. Static ELF checks do not replace a 16 KB runtime test."""
import argparse
import hashlib
import json
import struct
import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import ZipFile

ANDROID = '{http://schemas.android.com/apk/res/android}'
ALLOWED_PERMISSIONS = {
    'android.permission.CAMERA', 'android.permission.INTERNET',
    'android.permission.ACCESS_NETWORK_STATE', 'android.permission.WAKE_LOCK',
}

def audit(bundle, manifest, config):
    root=ET.parse(manifest).getroot()
    app=root.find('application')
    assert app is not None
    assert root.get('package') == 'com.fourierart.app'
    assert int(root.find('uses-sdk').get(ANDROID+'targetSdkVersion')) >= 36
    assert app.get(ANDROID+'debuggable', 'false') == 'false', 'Release must not be debuggable'
    assert app.get(ANDROID+'allowBackup') == 'false'
    assert app.get(ANDROID+'usesCleartextTraffic') == 'false'
    assert app.get(ANDROID+'icon'), 'Launcher icon missing'
    permissions=sorted(x.get(ANDROID+'name') for x in root.findall('uses-permission'))
    receiver_permission='com.fourierart.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
    own_permissions={x.get(ANDROID+'name'):x.get(ANDROID+'protectionLevel') for x in root.findall('permission')}
    if receiver_permission in permissions:
        assert own_permissions.get(receiver_permission) in {'signature', '0x00000002', '0x2', '2'}, 'Receiver permission must be signature-only'
    unknown=set(permissions)-ALLOWED_PERMISSIONS-{receiver_permission}
    assert not unknown, f'Unreviewed permissions: {sorted(unknown)}'
    assert 'android.permission.CAMERA' in permissions
    features={x.get(ANDROID+'name'):x.get(ANDROID+'required','true') for x in root.findall('uses-feature')}
    assert features.get('android.hardware.camera.any') == 'false'
    assert 'PAGE_ALIGNMENT_16K' in Path(config).read_text(), 'AAB must request 16 KB ZIP alignment'
    libraries=[]
    with ZipFile(bundle) as z:
        assert not any(n.upper().startswith('META-INF/') and n.upper().endswith(('.RSA','.DSA','.EC')) for n in z.namelist()), 'Validation AAB must be unsigned'
        for name in sorted(n for n in z.namelist() if n.startswith('base/lib/') and n.endswith('.so')):
            data=z.read(name)
            assert data[:4] == b'\x7fELF', name
            elfclass,byteorder=data[4:6]
            endian='<' if byteorder==1 else '>'
            aligned=None
            if elfclass==2:
                phoff=struct.unpack_from(endian+'Q',data,32)[0]
                entsize,num=struct.unpack_from(endian+'HH',data,54)
                loads=[]
                for i in range(num):
                    fields=struct.unpack_from(endian+'IIQQQQQQ',data,phoff+i*entsize)
                    ptype,_,offset,vaddr,_,_,_,align=fields
                    if ptype==1:
                        loads.append({'alignment':align,'offset':offset,'virtualAddress':vaddr})
                assert loads, f'No load segments: {name}'
                aligned=all(x['alignment']>=16384 and (x['offset']-x['virtualAddress'])%16384==0 for x in loads)
                assert aligned, f'64-bit ELF incompatible with 16 KB: {name}: {loads}'
            libraries.append({'path':name,'bits':64 if elfclass==2 else 32,'elf16kAligned':aligned})
    return {'bundle':str(bundle),'sha256':hashlib.sha256(Path(bundle).read_bytes()).hexdigest(),
            'applicationId':root.get('package'),'versionCode':root.get(ANDROID+'versionCode'),
            'versionName':root.get(ANDROID+'versionName'),'permissions':permissions,
            'nativeLibraries':libraries,'aabZipAlignment':'PAGE_ALIGNMENT_16K','signed':False,
            'limits':'Unsigned validation AAB, not uploadable to Play. Static ELF/packaging checks only; no 16 KB runtime or Play Console acceptance claimed.'}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('bundle');p.add_argument('manifest');p.add_argument('config')
    a=p.parse_args();print(json.dumps(audit(a.bundle,a.manifest,a.config),indent=2))
