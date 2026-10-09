#!/usr/bin/env bash
# Headless Paper 1.21.11 boot for the LanternEmbers jar. No GUI client.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/proof/run"
mkdir -p "$RUN/plugins"

PAPER="$RUN/paper.jar"
URL="https://fill-data.papermc.io/v1/objects/5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba/paper-1.21.11-132.jar"
if [[ ! -f "$PAPER" ]]; then
  curl -fL --retry 3 -o "$PAPER.partial" "$URL"
  mv "$PAPER.partial" "$PAPER"
fi

printf 'eula=true\n' > "$RUN/eula.txt"

PORT=25599
if ss -ltn | grep -q ":${PORT} "; then
  PORT=25598
fi

{
  cat "$ROOT/world/server.properties.snippet"
  echo "motd=Lantern"
  echo "max-players=4"
  echo "server-port=${PORT}"
} > "$RUN/server.properties"

cp "$ROOT/plugin/build/libs/LanternEmbers-1.0.0.jar" "$RUN/plugins/LanternEmbers-1.0.0.jar"

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
cd "$RUN"
: > boot.out

PIPE="$(mktemp -u)"
mkfifo "$PIPE"
(
  exec 3>"$PIPE"
  for _ in $(seq 1 180); do
    if grep -q "Done (" boot.out 2>/dev/null || { [[ -f logs/latest.log ]] && grep -q "Done (" logs/latest.log; }; then
      echo stop >&3
      exec 3>&-
      exit 0
    fi
    sleep 1
  done
  echo stop >&3
  exec 3>&-
) &
WRITER=$!

set +e
"$JAVA_HOME/bin/java" -Xms512M -Xmx2G -jar "$PAPER" nogui < "$PIPE" > boot.out 2>&1
JAVA_EXIT=$?
set -e
wait "$WRITER" || true
rm -f "$PIPE"

LOG="$RUN/logs/latest.log"
ENABLING="missing"
DISABLED_LINE="none"
DISABLED_BEFORE_DONE="no"
DONE_LINE="missing"
if [[ -f "$LOG" ]]; then
  ENABLING_MATCH="$(grep -m 1 "Enabling LanternEmbers" "$LOG" || true)"
  if [[ -n "$ENABLING_MATCH" ]]; then
    ENABLING="$ENABLING_MATCH"
  fi
  DISABLED_MATCH="$(grep "LanternEmbers" "$LOG" | grep -i "disabl" || true)"
  if [[ -n "$DISABLED_MATCH" ]]; then
    DISABLED_LINE="$DISABLED_MATCH"
  fi
  DONE_MATCH="$(grep -m 1 "Done (" "$LOG" || true)"
  if [[ -n "$DONE_MATCH" ]]; then
    DONE_LINE="$DONE_MATCH"
  fi
  if [[ -n "$ENABLING_MATCH" && -n "$DISABLED_MATCH" ]]; then
    ENABLE_AT="$(grep -n "Enabling LanternEmbers" "$LOG" | head -1 | cut -d: -f1)"
    DISABLE_AT="$(grep -n "LanternEmbers" "$LOG" | grep -i "disabl" | head -1 | cut -d: -f1)"
    DONE_AT="$(grep -n "Done (" "$LOG" | head -1 | cut -d: -f1 || true)"
    if [[ -n "$DONE_AT" && "$DISABLE_AT" -lt "$DONE_AT" ]]; then
      DISABLED_BEFORE_DONE="yes"
    fi
    if [[ -z "$DONE_AT" && -n "$ENABLE_AT" ]]; then
      DISABLED_BEFORE_DONE="yes"
    fi
  fi
fi

{
  echo "server-port: ${PORT}"
  echo "java-exit: ${JAVA_EXIT}"
  echo "done: ${DONE_LINE}"
  echo "Enabling LanternEmbers: ${ENABLING}"
  echo "plugin-disabled: ${DISABLED_LINE}"
  echo "disabled-before-done: ${DISABLED_BEFORE_DONE}"
} > "$ROOT/proof/paper-load.txt"

echo "wrote $ROOT/proof/paper-load.txt"
