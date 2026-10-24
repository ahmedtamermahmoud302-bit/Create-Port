#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME=/workspace/toolchains/jdk8u504-b01
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME=/workspace/.gradle
export LIBGL_ALWAYS_SOFTWARE="${LIBGL_ALWAYS_SOFTWARE:-1}"
mode=renderSmoke
if [[ "${1:-}" == --audit-only ]]; then mode=renderAudit; fi
display_file=""
xvfb_pid=""
cleanup() {
  if [[ -n "$xvfb_pid" ]]; then kill "$xvfb_pid" 2>/dev/null || true; fi
  if [[ -n "$display_file" ]]; then rm -f "$display_file"; fi
}
trap cleanup EXIT
if [[ -z "${DISPLAY:-}" ]]; then
  display_file=$(mktemp)
  xvfb_bin="${XVFB_BIN:-/workspace/xvfb/usr/bin/Xvfb}"
  "$xvfb_bin" -displayfd 3 -screen 0 1280x720x24 -nolisten tcp -ac \
    3>"$display_file" > /tmp/createport-render-display.log 2>&1 &
  xvfb_pid=$!
  for attempt in {1..100}; do
    if [[ -s "$display_file" ]]; then break; fi
    if ! kill -0 "$xvfb_pid" 2>/dev/null; then cat /tmp/createport-render-display.log; exit 1; fi
    sleep 0.1
  done
  [[ -s "$display_file" ]]
  export DISPLAY=":$(cat "$display_file")"
fi
mkdir -p build
python3 - <<'PY'
import time
from pathlib import Path
Path('build/render-check-start').write_text(str(time.time()))
PY
./gradlew runClient "-P$mode" --console=plain
python3 - "$mode" <<'PY'
import json, sys
from pathlib import Path
started = float(Path('build/render-check-start').read_text())
path = Path('run/model-audit.json')
assert path.stat().st_mtime >= started, 'Stale model audit'
audit = json.loads(path.read_text())
assert not audit['failures'] and not audit['models_with_missing_sprites'], audit
assert audit['loaded'] == audit['baked'] and audit['baked'] > 0
if sys.argv[1] == 'renderSmoke':
    path = Path('run/render-smoke.json')
    assert path.stat().st_mtime >= started, 'Stale world smoke result'
    result = json.loads(path.read_text())
    assert result.get('passed') is True, result
    assert result.get('energy_system_passed') is True, result
    path = Path('run/energy-smoke.json')
    assert path.stat().st_mtime >= started, 'Stale energy-world result'
    assert json.loads(path.read_text()).get('passed') is True
print('Passed current-run model bake' + (' and client-world render checks' if sys.argv[1] == 'renderSmoke' else ' checks'))
PY
