#!/usr/bin/env python3
"""Translate compatible source milling recipes to 1.12 item IDs/metadata."""
from pathlib import Path
import json
import sys

source = Path(sys.argv[1]) / 'src/generated/resources/data/create/recipe/milling'
root = Path(__file__).resolve().parents[1]
aliases = {
    'allium': ('red_flower', 2), 'azure_bluet': ('red_flower', 3), 'blue_orchid': ('red_flower', 1),
    'bone_meal': ('dye', 15), 'charcoal': ('coal', 1), 'cocoa_beans': ('dye', 3),
    'dandelion': ('yellow_flower', 0), 'fern': ('tallgrass', 2), 'granite': ('stone', 1),
    'andesite': ('stone', 5), 'ink_sac': ('dye', 0), 'lapis_lazuli': ('dye', 4),
    'large_fern': ('double_plant', 3), 'lilac': ('double_plant', 1), 'orange_tulip': ('red_flower', 5),
    'oxeye_daisy': ('red_flower', 8), 'peony': ('double_plant', 5), 'pink_tulip': ('red_flower', 7),
    'poppy': ('red_flower', 0), 'red_tulip': ('red_flower', 4), 'rose_bush': ('double_plant', 4),
    'short_grass': ('tallgrass', 1), 'sugar_cane': ('reeds', 0), 'sunflower': ('double_plant', 0),
    'tall_grass': ('double_plant', 2), 'terracotta': ('hardened_clay', 0), 'white_tulip': ('red_flower', 6),
    'red_sand': ('sand', 1),
}
for color, meta in {'black':0,'red':1,'green':2,'brown':3,'blue':4,'purple':5,'cyan':6,'light_gray':7,
                    'gray':8,'pink':9,'lime':10,'yellow':11,'light_blue':12,'magenta':13,'orange':14,'white':15}.items():
    aliases[color + '_dye'] = ('dye', meta)
absent = {'calcite','cornflower','dripstone_block','lily_of_the_valley','pink_petals','pitcher_plant','sea_pickle','torchflower','wither_rose'}
def item(value):
    namespace, name = value.split(':')
    if namespace == 'create':
        if name != 'wheat_flour': raise ValueError('Unported Create item ' + name)
        return {'item': 'createport:' + name, 'meta': 0}
    if name in absent: raise ValueError('No 1.12 counterpart for ' + name)
    name, meta = aliases.get(name, (name, 0))
    return {'item': 'minecraft:' + name, 'meta': meta}
recipes, skipped = [], []
for path in sorted(source.glob('*.json')):
    data = json.loads(path.read_text())
    try:
        ingredient = data['ingredients'][0]
        input_item = {'item': 'minecraft:wool', 'meta': 32767} if ingredient.get('tag') == 'minecraft:wool' else item(ingredient['item'])
        results = [{**item(result['id']), 'count': result.get('count', 1), 'chance': result.get('chance', 1)} for result in data['results']]
        if any(r['item'] == input_item['item'] and r['meta'] == input_item['meta'] for r in results):
            raise ValueError('Legacy dye alias would turn the input into more of itself')
        recipes.append({'id': path.stem, 'input': input_item, 'time': data['processing_time'], 'results': results})
    except (KeyError, ValueError) as e: skipped.append({'id': path.stem, 'reason': str(e)})
out = root / 'src/main/resources/assets/createport/gameplay/milling.json'
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(recipes, indent=2) + '\n')
(root / 'docs/MILLING-IMPORT.json').write_text(json.dumps({'supported':len(recipes),'skipped':skipped},indent=2)+'\n')
print('Imported', len(recipes), 'milling recipes;', len(skipped), 'unavailable/unsafe legacy mappings recorded')
