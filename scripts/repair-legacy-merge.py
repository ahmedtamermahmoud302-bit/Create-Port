#!/usr/bin/env python3
"""Rebuild the legacy MCP merge without the merge tool's annotation stubs.

The plain MCP 1.12.2 configuration injects the tool's three-value Side enum;
Forge's dated userdev configuration explicitly uses --inject false. Preserve
the original downloads and cached output, then apply that supported tool flag.
"""
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

cache = Path(os.environ.get('GRADLE_USER_HOME', '/workspace/.gradle')) / 'caches/forge_gradle'
pipeline = cache / 'mcp_repo/de/oceanlabs/mcp/mcp_config/1.12.2/joined'
merged = pipeline / 'merge/output.jar'
key = 'net/minecraftforge/fml/relauncher/Side.class'
if not merged.exists():
    print('MCP merge not generated yet')
    raise SystemExit(0)
with zipfile.ZipFile(merged) as archive:
    needs_repair = key in archive.namelist()
if not needs_repair:
    print('MCP merge already excludes annotation stubs')
    raise SystemExit(0)

tool = cache / 'maven_downloader/net/minecraftforge/mergetool/0.2.3.2/mergetool-0.2.3.2-fatjar.jar'
checksum = tool.with_name(tool.name + '.md5')
assert hashlib.md5(tool.read_bytes()).hexdigest() == checksum.read_text().strip().split()[0]
backup = Path('/workspace/scratch') / ('legacy-merge-' + datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%f'))
backup.mkdir(parents=True)
shutil.copyfile(merged, backup / 'original-merged.jar')
fixed = backup / 'merged-without-injected-stubs.jar'
arguments = ['--client', str(pipeline / 'stripClient/output.jar'),
             '--server', str(pipeline / 'stripServer/output.jar'),
             '--ann', '1.12.2', '--output', str(fixed), '--inject', 'false']
subprocess.run(['/workspace/toolchains/jdk8u504-b01/bin/java', '-jar', str(tool), *arguments], check=True)
with zipfile.ZipFile(fixed) as archive:
    assert key not in archive.namelist(), 'Annotation stub still present'
    assert archive.testzip() is None
shutil.copyfile(fixed, merged)
(backup / 'recipe.json').write_text(json.dumps({'tool': str(tool), 'arguments': arguments,
    'output_sha256': hashlib.sha256(fixed.read_bytes()).hexdigest()}, indent=2))
for index, directory in enumerate([
    cache / 'mcp_repo/net/minecraft/joined/1.12.2',
    cache / 'minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860',
    cache / 'minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12',
]):
    if directory.exists():
        shutil.move(str(directory), str(backup / ('derived-' + str(index))))
print('Rebuilt MCP merge with --inject false; preserved previous outputs at', backup)
