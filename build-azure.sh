#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/scripts/common.sh"
resolve_java
if ! command -v node >/dev/null || ! command -v npm >/dev/null; then
  echo "Install Node.js 20 or later with npm before building." >&2
  exit 1
fi
if ! node -e 'process.exit(Number(process.versions.node.split(".")[0]) >= 20 ? 0 : 1)'; then
  echo "Node.js 20 or later is required." >&2
  exit 1
fi
app_root="$LAB_ROOT"
frontend="$app_root/src/frontend"
backend="$app_root/src/ai-shopping-cart-service"

cd "$frontend"
npm ci --no-audit --no-fund
npm run build
# Keep browser requests on the same origin. Credentials belong to the backend.
printf '%s\n' 'window.ENV_CONFIG = { REACT_APP_API_URL: window.location.origin };' > build/env-config.js
# Both locations are generated. Remove old hashed assets before a repeat build.
rm -rf "$backend/src/main/resources/static" "$backend/target/classes/static"
mkdir -p "$backend/src/main/resources/static"
cp -R build/. "$backend/src/main/resources/static/"
cd "$backend"
./mvnw -B package "$@"
printf '%s\n' "Azure artifact: $backend/target/ai-shopping-cart-service-1.0.0-SNAPSHOT.jar"
