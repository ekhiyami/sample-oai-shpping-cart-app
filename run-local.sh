#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/scripts/common.sh"
resolve_java
require_jar
configure_local_server
# Ignore .env and disable both outbound Azure paths.
export SHOPPING_CART_AI_MOCK=true
export CHAT_AI_ENABLED=false
export RAG_ENABLED=false
export AZURE_OPENAI_MODEL_NAME=mock-responses
echo "Local practice: http://127.0.0.1:$SERVER_PORT (mock recommendations; no Azure calls)"
exec "$LAB_JAVA" -jar "$LAB_JAR"
