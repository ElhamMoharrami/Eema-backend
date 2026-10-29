#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
jar_file="$(realpath target/backend-0.0.1-SNAPSHOT.jar)"
probe_dir="$(mktemp -d /tmp/eema-font-package-XXXXXX)"
trap 'rm -rf "$probe_dir"' EXIT
unzip -q "$jar_file" 'BOOT-INF/classes/*' 'BOOT-INF/lib/*' -d "$probe_dir"
javac -encoding UTF-8 -cp "$probe_dir/BOOT-INF/classes:$probe_dir/BOOT-INF/lib/*" -d "$probe_dir/probe" scripts/qa/DocumentFontPackageProbe.java
java -Djava.awt.headless=true -Deema.doc.probeOutput=/tmp/eema-unicode-packaged-statement.pdf -Dloader.path="$probe_dir/probe" -Dloader.main=DocumentFontPackageProbe -cp "$jar_file" org.springframework.boot.loader.launch.PropertiesLauncher
if [[ ${1:-} == --docker ]]; then
 docker run --rm --network none --entrypoint java -v "$probe_dir/probe:/probe:ro" eema-doc-unicode:qa \
  -Djava.awt.headless=true -Dloader.path=/probe -Dloader.main=DocumentFontPackageProbe -cp /app/app.jar org.springframework.boot.loader.launch.PropertiesLauncher
fi
# Validate clear startup failures in disposable copies; the production JAR is untouched.
font_file="$probe_dir/BOOT-INF/classes/fonts/NotoSans-Regular.ttf"
cp "$font_file" "$probe_dir/original.ttf"
rm "$font_file"
if java -cp "$probe_dir/probe:$probe_dir/BOOT-INF/classes:$probe_dir/BOOT-INF/lib/*" DocumentFontPackageProbe > "$probe_dir/missing.log" 2>&1; then
 echo 'FAIL: missing required font accepted'; exit 1
fi
rg -q 'Required document font unavailable|Missing required bundled font' "$probe_dir/missing.log"
printf 'not a font' > "$font_file"
if java -cp "$probe_dir/probe:$probe_dir/BOOT-INF/classes:$probe_dir/BOOT-INF/lib/*" DocumentFontPackageProbe > "$probe_dir/corrupt.log" 2>&1; then
 echo 'FAIL: corrupt required font accepted'; exit 1
fi
rg -q 'Required bundled document fonts are missing or corrupt' "$probe_dir/corrupt.log"
echo 'PASS: missing and corrupt required resources fail clearly at renderer startup'
