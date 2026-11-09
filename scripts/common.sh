#!/usr/bin/env bash
set -euo pipefail
LAB_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LAB_JAR="$LAB_ROOT/src/ai-shopping-cart-service/target/ai-shopping-cart-service-1.0.0-SNAPSHOT.jar"

resolve_java() {
  if [[ -n "${JAVA_HOME:-}" ]]; then
    LAB_JAVA="$JAVA_HOME/bin/java"
  else
    LAB_JAVA="$(command -v java || true)"
  fi
  if [[ -z "$LAB_JAVA" ]] || ! "$LAB_JAVA" -version >/dev/null 2>&1; then
    echo "Java is unavailable. Install JDK 17 and set JAVA_HOME to its installation directory." >&2
    exit 1
  fi
}

require_jar() {
  if [[ ! -f "$LAB_JAR" ]]; then
    echo "Build the application first: bash build-azure.sh" >&2
    exit 1
  fi
}

configure_local_server() {
  export SPRING_PROFILES_ACTIVE=local
  export SPRING_DATASOURCE_URL='jdbc:h2:mem:ai_shopping_cart;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1'
  export SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver
  export SPRING_DATASOURCE_USERNAME=sa
  export SPRING_DATASOURCE_PASSWORD=''
  export SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect
  export SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop
  export SERVER_ADDRESS=127.0.0.1
  export SERVER_PORT="${SERVER_PORT:-8080}"
  export CHAT_ENABLED=true
  export CHAT_OWNER_ID=''
  export CHAT_ORIGIN="http://127.0.0.1:$SERVER_PORT"
  export NUTRITION_LOOKUP_ENABLED=true
}
