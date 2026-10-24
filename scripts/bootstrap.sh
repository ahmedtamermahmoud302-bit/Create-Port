#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p /workspace/toolchains /workspace/.gradle
archive=/workspace/toolchains/temurin8.tar.gz
checksum=9c70e102f527ac674ac2fe9c7d47b9a04e2d19842ba5ab8e9b33f368bbadfaea
if ! printf '%s  %s\n' "$checksum" "$archive" | sha256sum --check --status; then
  curl --fail --location --retry 2 \
    https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u504-b01/OpenJDK8U-jdk_x64_linux_hotspot_8u504b01.tar.gz \
    --output "$archive"
fi
printf '%s  %s\n' "$checksum" "$archive" | sha256sum --check
if [[ ! -x /workspace/toolchains/jdk8u504-b01/bin/java ]]; then
  tar -xzf "$archive" -C /workspace/toolchains
fi
export JAVA_HOME=/workspace/toolchains/jdk8u504-b01
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME=/workspace/.gradle
# Java does not read HTTPS_PROXY. Store only non-secret host/port settings,
# preserving unrelated existing Gradle settings. Keep TLS verification enabled.
python3 - <<'PY'
import os
from pathlib import Path
from urllib.parse import urlsplit
proxy = urlsplit(os.environ.get('HTTPS_PROXY', os.environ.get('https_proxy', '')))
if proxy.hostname:
    if proxy.username or proxy.password:
        raise SystemExit('Authenticated proxy needs a supported credential binding; refusing to copy credentials')
    port = str(proxy.port or (443 if proxy.scheme == 'https' else 80))
    settings = {
        'systemProp.http.proxyHost': proxy.hostname,
        'systemProp.http.proxyPort': port,
        'systemProp.https.proxyHost': proxy.hostname,
        'systemProp.https.proxyPort': port,
        'systemProp.http.nonProxyHosts': 'localhost|127.*',
        'systemProp.https.nonProxyHosts': 'localhost|127.*',
        'systemProp.javax.net.ssl.trustStore': '/etc/ssl/certs/java/cacerts',
    }
    path = Path('/workspace/.gradle/gradle.properties')
    lines = path.read_text().splitlines() if path.exists() else []
    preserved = [line for line in lines if line.split('=', 1)[0].strip() not in settings]
    updated = '\n'.join(preserved + [k + '=' + v for k, v in settings.items()]) + '\n'
    if not path.exists() or path.read_text() != updated:
        path.write_text(updated)
PY
./gradlew --version
python3 scripts/prepare-assets.py
bash scripts/test-kinetics.sh
