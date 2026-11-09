#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/scripts/common.sh"
if [[ -f "$LAB_ROOT/.env" ]]; then
  set -a
  source "$LAB_ROOT/.env"
  set +a
fi
for setting in AZURE_OPENAI_ENDPOINT AZURE_OPENAI_DEPLOYMENT_ID AZURE_OPENAI_API_KEY; do
  value="${!setting:-}"
  if [[ -z "$value" || "$value" == *YOUR-* || "$value" == REPLACE-ME ]]; then
    echo "Set $setting in .env or your shell. Start with .env.example." >&2
    exit 1
  fi
done
resolve_java
require_jar
configure_local_server
export SHOPPING_CART_AI_MOCK=false
export RAG_ENABLED="${RAG_ENABLED:-false}"
export CHAT_AI_ENABLED="${CHAT_AI_ENABLED:-false}"
if [[ "$CHAT_AI_ENABLED" == true && "$RAG_ENABLED" != true ]]; then
  echo "Grounded chat requires RAG_ENABLED=true and configured Azure Search resources." >&2
  exit 1
fi
if [[ "$RAG_ENABLED" == true ]]; then
  for setting in RAG_SEARCH_ENDPOINT RAG_SEARCH_API_KEY RAG_SEARCH_INDEX; do
    if [[ -z "${!setting:-}" ]]; then
      echo "Set $setting before enabling Azure Search." >&2
      exit 1
    fi
  done
fi
echo "Azure OpenAI mode: http://127.0.0.1:$SERVER_PORT (local database; live Azure model requests)"
exec "$LAB_JAVA" -jar "$LAB_JAR"
