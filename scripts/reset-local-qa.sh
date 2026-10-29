#!/usr/bin/env bash
# Destroys ONLY the guarded local Compose project's database/media/document volumes.
set -euo pipefail
root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
mode=${1:-}
if [[ $# != 1 || ( "$mode" != --dry-run && "$mode" != --confirm-destroy-local-qa ) ]]; then
  echo 'Usage: scripts/reset-local-qa.sh --dry-run | --confirm-destroy-local-qa' >&2
  echo 'DESTRUCTIVE: confirmation deletes ALL local QA database, uploads and private documents.' >&2
  exit 2
fi
fail() { echo "Refusing reset: $*" >&2; exit 1; }
[[ -z ${DOCKER_HOST:-} || ${DOCKER_HOST} == unix:///* ]] || fail 'DOCKER_HOST is not a local Unix socket.'
endpoint=$(docker context inspect --format '{{.Endpoints.docker.Host}}')
[[ "$endpoint" == unix:///* ]] || fail 'Docker context is not a local Unix socket.'
compose=(docker compose --project-directory "$root" --project-name eema-backend --file "$root/docker-compose.yml")
# Parse without printing effective configuration: it can contain provider secrets.
"${compose[@]}" config --format json | python3 -c '
import json, sys
c=json.load(sys.stdin)
def require(condition, message):
    if not condition: sys.exit("Refusing reset: " + message)
require(c.get("name")=="eema-backend", "unexpected project")
s=c.get("services", {})
require(set(s)=={"postgres", "backend"}, "unexpected services")
p=s["postgres"]; b=s["backend"]; pe=p.get("environment", {}); e=b.get("environment", {})
require(p.get("image")=="postgres:16-alpine", "unexpected database image")
require(pe.get("POSTGRES_DB")=="eema" and pe.get("POSTGRES_USER")=="eema", "unexpected database")
require(e.get("SPRING_PROFILES_ACTIVE")=="local", "profile must be local")
require(e.get("SPRING_DATASOURCE_URL")=="jdbc:postgresql://postgres:5432/eema", "database must be Compose postgres/eema")
require(e.get("SPRING_DATASOURCE_USERNAME")=="eema", "unexpected database user")
for key in ("EMAIL_NOTIFICATIONS_ENABLED", "MOLLIE_FEE_RECONCILIATION_ENABLED", "APP_RESEND_ENABLED", "APP_MOLLIE_FEE_RECONCILIATION_ENABLED"):
    require(str(e.get(key,"false")).lower()=="false", key+" must be false")
for key in ("SPRING_APPLICATION_JSON", "SPRING_CONFIG_LOCATION", "SPRING_CONFIG_ADDITIONAL_LOCATION", "SPRING_CONFIG_IMPORT", "SPRING_PROFILES_INCLUDE", "SPRING_PROFILES_DEFAULT", "SPRING_DATASOURCE_JNDI_NAME", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS"):
    require(not e.get(key), key+" overrides are unsupported for reset")
for key in ("STORAGE_PROVIDER", "APP_STORAGE_PROVIDER", "DOCUMENT_STORAGE_PROVIDER"):
    require(e.get(key,"local")=="local", key+" must be local")
volumes=c.get("volumes", {})
expected={"eema-postgres-data", "eema-uploads", "eema-private-documents"}
require(set(volumes)==expected, "unexpected volumes")
for name,v in volumes.items():
    require(not v.get("external") and not v.get("driver_opts") and v.get("driver","local")=="local", "nonlocal/external volume")
    require(v.get("name")=="eema-backend_"+name, "volume is outside QA project")
for service in s.values():
    require(not service.get("command") and not service.get("entrypoint"), "custom startup overrides unsupported")
    for v in service.get("volumes", []):
        require(v.get("type")=="volume" and v.get("source") in expected, "bind or unexpected mount")
print("Guards passed: local Docker socket, local database, disabled email/reporting, project-owned volumes.")
'
echo 'Will DELETE: eema-backend_eema-postgres-data, eema-backend_eema-uploads, eema-backend_eema-private-documents'
if [[ "$mode" == --dry-run ]]; then
  echo 'Dry run complete. Nothing stopped or deleted.'
  exit 0
fi
"${compose[@]}" down -v
"${compose[@]}" up -d --build postgres backend
for ((attempt=0; attempt<120; attempt++)); do
  if "${compose[@]}" exec -T postgres pg_isready -U eema -d eema >/dev/null 2>&1 &&
     "${compose[@]}" exec -T backend wget -q -O /dev/null http://localhost:8080/api/actuator/health >/dev/null 2>&1; then
    echo 'Local QA reset complete: PostgreSQL and backend healthy; local migrations reseeded baseline fixtures.'
    exit 0
  fi
  sleep 2
done
echo 'Reset started but health did not pass within 240 seconds. Inspect local service status; do not rerun destructively as a health check.' >&2
exit 1
