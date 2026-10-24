#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME=/workspace/toolchains/jdk8u504-b01
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME=/workspace/.gradle
./gradlew build --console=plain
verify_runtime() {
python3 - <<'PY'
from pathlib import Path
import zipfile
folder = Path('/workspace/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12')
p = folder / ('forge-' + folder.name + '.jar')
with zipfile.ZipFile(p) as archive:
    assert b'BUKKIT' not in archive.read('net/minecraftforge/fml/relauncher/Side.class'), 'Wrong runtime Side enum'
print('Verified official Forge logical-side enum in generated runtime')
PY
}
if ! verify_runtime; then
  python3 scripts/repair-legacy-merge.py
  ./gradlew build --console=plain
  verify_runtime
fi
