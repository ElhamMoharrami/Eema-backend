#!/usr/bin/env bash
set -euo pipefail
# Date for readability; random suffix prevents collisions across parallel runs.
printf '%s-%s\n' "$(date -u +%Y%m%d-%H%M%S)" "$(python3 -c 'import secrets; print(secrets.token_hex(4))')"
