#!/usr/bin/env bash
# Two offline clients against local Paper. Writes proof/transcript.txt.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/proof/run"
mkdir -p "$RUN/plugins" "$RUN/world"

PAPER="$RUN/paper.jar"
URL="https://fill-data.papermc.io/v1/objects/5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba/paper-1.21.11-132.jar"
if [[ ! -f "$PAPER" ]]; then
  curl -fL --retry 3 -o "$PAPER.partial" "$URL"
  mv "$PAPER.partial" "$PAPER"
fi

PORT=25597
if ss -ltn | grep -q ":${PORT} "; then
  echo "port ${PORT} is already in use" >&2
  exit 1
fi

printf 'eula=true\n' > "$RUN/eula.txt"
{
  cat "$ROOT/world/server.properties.snippet"
  echo "motd=Lantern"
  echo "max-players=4"
  echo "server-port=${PORT}"
  echo "gamemode=creative"
  echo "force-gamemode=true"
  echo "enforce-secure-profile=false"
  echo "spawn-monsters=false"
} > "$RUN/server.properties"

python3 - <<'PY' > "$RUN/whitelist.json"
import hashlib, json, uuid
def offline(name):
    digest = hashlib.md5(("OfflinePlayer:" + name).encode("utf-8")).digest()
    arr = bytearray(digest)
    arr[6] = (arr[6] & 0x0F) | 0x30
    arr[8] = (arr[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(arr)))
names = ["Ada", "Bea"]
print(json.dumps([{"uuid": offline(n), "name": n} for n in names], indent=2))
PY
python3 - <<'PY' > "$RUN/ops.json"
import hashlib, json, uuid
def offline(name):
    digest = hashlib.md5(("OfflinePlayer:" + name).encode("utf-8")).digest()
    arr = bytearray(digest)
    arr[6] = (arr[6] & 0x0F) | 0x30
    arr[8] = (arr[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(arr)))
print(json.dumps([
    {"uuid": offline(n), "name": n, "level": 4, "bypassesPlayerLimit": False}
    for n in ("Ada", "Bea")
], indent=2))
PY

cp "$ROOT/plugin/build/libs/LanternEmbers-1.0.0.jar" "$RUN/plugins/LanternEmbers-1.0.0.jar"
# Drop a previous session log so join exceptions are from this boot.
rm -f "$RUN/logs/latest.log"

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
cd "$RUN"
: > boot.out
"$JAVA_HOME/bin/java" -Xms512M -Xmx2G -jar "$PAPER" nogui > boot.out 2>&1 &
JAVA_PID=$!

cleanup() {
  if kill -0 "$JAVA_PID" 2>/dev/null; then
    kill "$JAVA_PID" 2>/dev/null || true
    wait "$JAVA_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

for _ in $(seq 1 180); do
  if grep -q "Done (" boot.out 2>/dev/null; then
    break
  fi
  if ! kill -0 "$JAVA_PID" 2>/dev/null; then
    echo "paper exited before Done" >&2
    cat boot.out >&2
    exit 1
  fi
  sleep 1
done
if ! grep -q "Done (" boot.out; then
  echo "paper did not reach Done" >&2
  exit 1
fi

export PORT
export NODE_PATH="${NODE_PATH:-/tmp/le-bot/node_modules}"
set +e
node "$ROOT/proof/two-clients.cjs"
NODE_EXIT=$?
set -e

cleanup
trap - EXIT
exit "$NODE_EXIT"
