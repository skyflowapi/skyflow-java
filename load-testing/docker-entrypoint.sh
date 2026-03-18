#!/usr/bin/env bash
# Starts EchoServer and WrapperServer inside the SUT container.
set -euo pipefail

ECHO_PORT="${ECHO_PORT:-3015}"
WRAPPER_PORT="${WRAPPER_PORT:-8080}"
ECHO_WAIT_MS="${ECHO_WAIT_MS:-0}"
ECHO_ERR_PCT="${ECHO_ERR_PCT:-0}"
VAULT_ID="${VAULT_ID:-mock-vault-id}"

echo "[entrypoint] Starting EchoServer on :$ECHO_PORT (wait=${ECHO_WAIT_MS}ms, err=${ECHO_ERR_PCT}%)"
java -cp /app EchoServer "$ECHO_PORT" "$ECHO_WAIT_MS" "$ECHO_ERR_PCT" &
ECHO_PID=$!

# Wait for EchoServer to be ready
for i in $(seq 1 10); do
    curl -sf "http://localhost:$ECHO_PORT/health" > /dev/null 2>&1 && break
    sleep 1
done

echo "[entrypoint] Starting WrapperServer on :$WRAPPER_PORT"
VAULT_ID="$VAULT_ID" \
VAULT_URL="http://localhost:$ECHO_PORT" \
WRAPPER_PORT="$WRAPPER_PORT" \
java -jar /app/wrapper.jar &
WRAPPER_PID=$!

# Wait for WrapperServer to be ready
for i in $(seq 1 15); do
    curl -sf "http://localhost:$WRAPPER_PORT/health" > /dev/null 2>&1 && break
    sleep 1
done

echo "[entrypoint] Both servers running. EchoServer PID=$ECHO_PID, WrapperServer PID=$WRAPPER_PID"

# Keep container alive; forward signals
trap "kill $ECHO_PID $WRAPPER_PID 2>/dev/null" EXIT INT TERM
wait $WRAPPER_PID
