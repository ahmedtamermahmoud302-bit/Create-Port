#!/usr/bin/env python3
"""Check converted model graphs against the shipped resources and target client."""
import argparse
import json
from pathlib import Path
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--minecraft-jar', type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets'
catalog = json.loads((root / 'createport/render_catalog.json').read_text())
with zipfile.ZipFile(args.minecraft_jar) as archive:
    vanilla = set(archive.namelist())
errors = []
checks = 0

def require(resource, folder, extension):
    global checks
    checks += 1
    namespace, path = resource.split(':', 1) if ':' in resource else ('minecraft', resource)
    if path.startswith('builtin/') or resource == 'missingno': return
    relative = 'assets/' + namespace + '/' + folder + '/' + path + extension
    if namespace == 'minecraft':
        exists = relative in vanilla
    else:
        exists = (root.parent / relative).exists()
    if not exists: errors.append('Missing ' + relative)

for resource in catalog['models']:
    require(resource, 'models', '.json')
for p in root.rglob('models/**/*.json'):
    model = json.loads(p.read_text())
    if 'parent' in model: require(model['parent'], 'models', '.json')
    for texture in model.get('textures', {}).values():
        if not texture.startswith('#'): require(texture, 'textures', '.png')
    if model.get('loader') == 'createport:obj':
        namespace, obj = model['model'].split(':', 1)
        require(namespace + ':' + obj.removeprefix('models/'), 'models', '')

def variants(data):
    if isinstance(data, list):
        for entry in data: variants(entry)
    elif isinstance(data, dict):
        for key, value in data.items():
            if key == 'model':
                namespace, path = value.split(':', 1) if ':' in value else ('minecraft', value)
                require(namespace + ':block/' + path, 'models', '.json')
            else: variants(value)

for name in catalog['blockstates']:
    path = root / 'create/blockstates' / (name + '.json')
    variants(json.loads(path.read_text()))
if catalog['missing_textures']: errors.extend(catalog['missing_textures'])
if errors: raise SystemExit('\n'.join(sorted(set(errors))))
print('Passed', checks, 'model, parent, texture, OBJ and blockstate reference checks')
