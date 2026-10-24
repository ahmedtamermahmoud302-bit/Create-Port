#!/usr/bin/env python3
"""Convert the user-supplied Create resource tree; never edit the source tree."""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('source', type=Path)
parser.add_argument('--minecraft-jar', type=Path, required=True)
parser.add_argument('--modern-minecraft-jar', type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
target = root / 'src/main/resources/assets/create'
target.mkdir(parents=True, exist_ok=True)
models, states = {}, {}
with zipfile.ZipFile(args.minecraft_jar) as jar:
    vanilla_files = set(jar.namelist())
modern = zipfile.ZipFile(args.modern_minecraft_jar) if args.modern_minecraft_jar else None
if modern:
    assert hashlib.sha1(args.modern_minecraft_jar.read_bytes()).hexdigest() == '30c73b1c5da787909b2f73340419fdf13b9def88', 'Unexpected modern client artifact'
compat = root / 'src/main/resources/assets/createport'
modern_files = set(modern.namelist()) if modern else set()
copied_modern = set()

def modern_texture(path):
    key = 'assets/minecraft/textures/' + path + '.png'
    out = compat / 'textures/compat/minecraft' / (path + '.png')
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(modern.read(key))
    if key + '.mcmeta' in modern_files:
        out.with_name(out.name + '.mcmeta').write_bytes(modern.read(key + '.mcmeta'))
    copied_modern.add(key)
    return 'createport:compat/minecraft/' + path

for scope in ('main', 'generated'):
    assets = args.source / 'src' / scope / 'resources/assets/create'
    for p in (assets / 'models').rglob('*'):
        if not p.is_file():
            continue
        relative = p.relative_to(assets)
        if p.suffix == '.json':
            models[p.relative_to(assets / 'models').as_posix()[:-5]] = json.loads(p.read_text())
        else:
            out = target / relative
            out.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(p, out)
    for p in (assets / 'blockstates').glob('*.json'):
        states[p.stem] = json.loads(p.read_text())
    for p in (assets / 'textures').rglob('*'):
        if p.is_file():
            out = target / p.relative_to(assets)
            out.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(p, out)

renames = {
    'piston_top': 'piston_top_normal', 'powered_rail': 'rail_golden',
    'powered_rail_on': 'rail_golden_powered', 'rail': 'rail_normal',
    'activator_rail': 'rail_activator', 'activator_rail_on': 'rail_activator_powered',
    'detector_rail': 'rail_detector', 'detector_rail_on': 'rail_detector_powered',
    'redstone_torch': 'redstone_torch_on', 'glass_pane_top': 'glass_pane_top',
    'polished_andesite': 'stone_andesite_smooth', 'anvil': 'anvil_base',
}
substitutions = {
    'stripped_spruce_log': 'planks_spruce', 'stripped_spruce_log_top': 'log_spruce_top',
    'stripped_oak_log': 'planks_oak', 'stripped_dark_oak_log': 'planks_big_oak',
    'stripped_dark_oak_log_top': 'log_big_oak_top', 'stonecutter_saw': 'iron_bars',
    'stonecutter_bottom': 'furnace_top', 'campfire_fire': 'fire_layer_0',
    'soul_campfire_fire': 'fire_layer_0', 'copper_block': 'gold_block',
    'smooth_stone': 'stone_slab_top', 'smooth_stone_slab_side': 'stone_slab_side',
    'warped_planks': 'planks_jungle', 'crimson_planks': 'planks_acacia',
    'mangrove_planks': 'planks_big_oak', 'cherry_planks': 'planks_birch',
    'bamboo_planks': 'planks_birch',
}
used_substitutions = {}
missing_textures = set()

def location(value):
    return value.split(':', 1) if ':' in value else ('minecraft', value)

def texture(value):
    if value.startswith('#'):
        return value
    domain, path = location(value)
    if domain != 'minecraft':
        if value == 'create:block/track':
            return 'create:block/standard_track'
        if domain == 'create' and not (target / 'textures' / (path + '.png')).exists():
            missing_textures.add(value)
        return value
    if modern and 'assets/minecraft/textures/' + path + '.png' in modern_files:
        return modern_texture(path)
    if path.startswith('block/'):
        name = path[6:]
        old = renames.get(name, substitutions.get(name, name))
        if name in substitutions:
            used_substitutions['minecraft:' + path] = 'minecraft:blocks/' + old
        for wood, legacy in [('dark_oak', 'big_oak'), ('oak', 'oak'), ('spruce', 'spruce'),
                             ('birch', 'birch'), ('jungle', 'jungle'), ('acacia', 'acacia')]:
            if name == wood + '_planks': old = 'planks_' + legacy
            if name == wood + '_log': old = 'log_' + legacy
            if name == wood + '_log_top': old = 'log_' + legacy + '_top'
        if name.endswith('_stained_glass'): old = 'glass_' + name[:-14]
        if name.endswith('_concrete_powder'): old = 'concrete_powder_' + name[:-16]
        path = 'blocks/' + old
    elif path.startswith('item/'):
        path = 'items/' + path[5:]
    if 'assets/minecraft/textures/' + path + '.png' not in vanilla_files:
        missing_textures.add('minecraft:' + path)
    return 'minecraft:' + path

parent_renames = {
    'block/template_wall_post': 'block/wall_post',
    'block/template_wall_side': 'block/wall_side',
    'block/template_wall_side_tall': 'create:block/compat/wall_side_tall',
    **{'block/template_glass_pane_' + name: 'block/pane_' + name
       for name in ('post', 'side', 'side_alt', 'noside', 'noside_alt')},
}

def parent(value):
    domain, path = location(value)
    if domain == 'minecraft':
        if path == 'block/air':
            out = compat / 'models/compat/minecraft/block/air.json'
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(json.dumps({'elements': [], 'textures': {'particle': 'create:block/axis'}}))
            return 'createport:compat/minecraft/block/air'
        key = 'assets/minecraft/models/' + path + '.json'
        if modern and key not in vanilla_files and key in modern_files:
            out = compat / 'models/compat/minecraft' / (path + '.json')
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(json.dumps(clean(json.loads(modern.read(key))), indent=2) + '\n')
            copied_modern.add(key)
            return 'createport:compat/minecraft/' + path
        path = parent_renames.get(path, path)
        return path if ':' in path else 'minecraft:' + path
    return value

def inherited(key, stack=()):
    if key in stack:
        raise ValueError('Cyclic model parents: ' + str(stack + (key,)))
    data = copy.deepcopy(models[key])
    p = data.get('parent', '')
    if p.startswith('create:') and p[7:] in models:
        base = inherited(p[7:], stack + (key,))
        for field in ('textures', 'display'):
            data[field] = {**base.get(field, {}), **data.get(field, {})}
        for field in ('elements', 'ambientocclusion'):
            if field not in data and field in base:
                data[field] = base[field]
        # Legacy ModelBlock's alias traversal cannot reproduce all modern parent
        # retexturing chains. Materialize the inherited geometry and root parent.
        data['parent'] = base.get('parent', 'minecraft:block/block')
        if base.get('loader') in ('neoforge:obj', 'neoforge:composite') and 'loader' not in data:
            for field in ('loader', 'model', 'flip_v', 'children'):
                if field in base: data[field] = copy.deepcopy(base[field])
            data['parent'] = base.get('parent', 'minecraft:block/block')
    return data

def resolve_aliases(textures):
    def resolve(value):
        seen = set()
        while value.startswith('#') and value[1:] in textures:
            if value in seen: return value
            seen.add(value)
            value = textures[value[1:]]
        return value
    return {key: resolve(value) for key, value in textures.items()}

def clean(data):
    if isinstance(data, list):
        return [clean(v) for v in data]
    if not isinstance(data, dict):
        return data
    data = {k: clean(v) for k, v in data.items()
            if k not in ('neoforge_data', 'render_type', 'gui_light', 'texture_size', 'groups')}
    if 'textures' in data:
        data['textures'] = {k: texture(v) for k, v in resolve_aliases(data['textures']).items()}
    if 'parent' in data:
        data['parent'] = parent(data['parent'])
    return data

if modern:
    atlas = json.loads((args.source / 'src/main/resources/assets/minecraft/atlases/blocks.json').read_text())
    jobs = []
    for source in atlas['sources']:
        if source['type'] != 'paletted_permutations':
            continue
        def palette_path(resource):
            domain, path = location(resource)
            modern_texture(path)
            return compat / 'textures/compat/minecraft' / (path + '.png')
        key = palette_path(source['palette_key'])
        for name, palette in source['permutations'].items():
            palette = palette_path(palette)
            for resource in source['textures']:
                domain, path = location(resource)
                base = target / 'textures' / (path + '.png')
                out = target / 'textures' / (path + '_' + name + '.png')
                jobs.extend(map(str, (base, key, palette, out)))
    tools = root / 'build/palette-tool'
    tools.mkdir(parents=True, exist_ok=True)
    java = Path('/workspace/toolchains/jdk8u504-b01/bin')
    subprocess.run([str(java / 'javac'), '-d', str(tools), str(root / 'scripts/PaletteTextures.java')], check=True)
    subprocess.run([str(java / 'java'), '-Djava.awt.headless=true', '-cp', str(tools), 'PaletteTextures', *jobs], check=True)

obj_models, composites, abstract_models = [], [], []
for key, original in list(models.items()):
    data = inherited(key)
    bindings = resolve_aliases(data.get('textures', {}))
    missing_slots = any(v.startswith('#') for v in bindings.values())
    for element in data.get('elements', []):
        for face in element.get('faces', {}).values():
            ref = face.get('texture', '')
            if ref.startswith('#') and bindings.get(ref[1:], ref).startswith('#'): missing_slots = True
    if key.startswith('block/') and missing_slots: abstract_models.append('create:' + key)
    if data.get('loader') == 'neoforge:composite':
        composites.append(key)
        parts = {}
        for name, child in data['children'].items():
            child = copy.deepcopy(child)
            child['textures'] = {**child.get('textures', {}), **data.get('textures', {})}
            part = 'block/_legacy_parts/' + key.removeprefix('block/') + '/' + name
            out = target / 'models' / (part + '.json')
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(json.dumps(clean(child), indent=2) + '\n')
            parts[name] = {'model': 'create:' + part,
                           'layer': child.get('render_type', 'minecraft:solid').split(':')[-1].upper()}
        data['children'] = parts
        data['loader'] = 'createport:composite'
    if data.get('loader') == 'neoforge:obj':
        obj_models.append(key)
        data['loader'] = 'createport:obj'
        # The old OBJ loader does not implement NeoForge's inherited texture slots.
        defaults = {'log': 'block/spruce_log', 'planks': 'block/spruce_planks',
                    'log_top': 'block/spruce_log_top'}
        data['textures'] = {**defaults, **data.get('textures', {})}
    out = target / 'models' / (key + '.json')
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(clean(data), indent=2) + '\n')

def fix_variants(value):
    if isinstance(value, list): return [fix_variants(v) for v in value]
    if not isinstance(value, dict): return value
    value = {k: fix_variants(v) for k, v in value.items()}
    if 'model' in value:
        domain, path = location(value['model'])
        if domain == 'minecraft' and 'assets/minecraft/models/' + path + '.json' not in vanilla_files:
            imported = parent(domain + ':' + path)
            if imported.startswith('createport:'):
                alias = '_vanilla/' + path.removeprefix('block/')
                out = target / 'models/block' / (alias + '.json')
                out.parent.mkdir(parents=True, exist_ok=True)
                out.write_text(json.dumps({'parent': imported}) + '\n')
                domain, path = 'create', alias
        # 1.12's Variant deserializer adds block/ itself.
        value['model'] = domain + ':' + (path[6:] if path.startswith('block/') else path)
    return value

for key, data in states.items():
    out = target / 'blockstates' / (key + '.json')
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(fix_variants(data), indent=2) + '\n')

# Tall wall side did not exist as a vanilla template in 1.12.2.
faces = {d: {'texture': '#wall'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}
out = target / 'models/block/compat/wall_side_tall.json'
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps({'parent': 'minecraft:block/block', 'textures': {'particle': '#wall'},
                          'elements': [{'from': [5, 0, 0], 'to': [11, 16, 8], 'faces': faces}]}, indent=2))

# Replace the four prototype item/block references with the supplied Create geometry.
prototype = root / 'src/main/resources/assets/createport'
for name in ('shaft', 'cogwheel', 'large_cogwheel'):
    (prototype / 'models/item' / (name + '.json')).write_text(json.dumps({'parent': 'create:block/' + name}) + '\n')
    variants = {'axis=y': {'model': 'create:' + name},
                'axis=x': {'model': 'create:' + name, 'x': 90, 'y': 90},
                'axis=z': {'model': 'create:' + name, 'x': 90, 'y': 180}}
    (prototype / 'blockstates' / (name + '.json')).write_text(json.dumps({'variants': variants}, indent=2))
(prototype / 'models/item/creative_motor.json').write_text(json.dumps({'parent': 'create:block/creative_motor/block'}) + '\n')
(prototype / 'blockstates/creative_motor.json').write_text(json.dumps({'variants': {
    'axis=x': {'model': 'create:creative_motor/block', 'y': 90},
    'axis=y': {'model': 'create:creative_motor/block', 'x': 90},
    'axis=z': {'model': 'create:creative_motor/block'}}}, indent=2))

catalog = {'models': ['create:' + k for k in sorted(models)], 'obj_models': obj_models,
           'blockstates': sorted(states), 'composite_models': composites,
           'texture_substitutions': used_substitutions, 'missing_textures': sorted(missing_textures)}
catalog['modern_minecraft_resources'] = sorted(copied_modern)
catalog['abstract_models'] = abstract_models
(prototype / 'render_catalog.json').write_text(json.dumps(catalog, indent=2) + '\n')
notice = root / 'src/main/resources/META-INF/CREATE-SOURCE-LICENSE.md'
notice.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(args.source / 'LICENSE.md', notice)
(root / 'docs/MODEL-IMPORT.json').write_text(json.dumps({
    **catalog, 'source': str(args.source), 'model_count': len(models),
    'blockstate_count': len(states), 'texture_count': len(list((target / 'textures').rglob('*.png'))),
    'limitations': ['Resource availability does not register unported blocks.',
                    'Composite children retain their render layers; future blocks must opt into those chunk render layers.',
                    'Modern vanilla textures require the verified 1.21.1 client input for exact import.',
                    'Specialized connected textures, fluid and contraption renderers are not implemented.']
}, indent=2) + '\n')
print('Converted', len(models), 'models;', len(states), 'blockstates;', len(obj_models), 'OBJ wrappers;',
      len(composites), 'composites. Missing textures:', sorted(missing_textures))
