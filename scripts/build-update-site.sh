#!/bin/sh
set -eu

project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
repository_output="$project_root/releng/com.brianwalker.dbeaver.resultsvisualizer.repository/target/repository"

if [ -z "${JAVA_HOME:-}" ] && [ -d "/Applications/DBeaver.app/Contents/Eclipse/jre/Contents/Home" ]; then
    JAVA_HOME="/Applications/DBeaver.app/Contents/Eclipse/jre/Contents/Home"
    export JAVA_HOME
fi

cd "$project_root"
./mvnw clean verify
"$project_root/scripts/validate-update-site.sh" "$repository_output"

echo "Clean update site created at $repository_output"
