#!/bin/sh
set -eu

repository=${1:-releng/com.brianwalker.dbeaver.resultsvisualizer.repository/target/repository}

for required in artifacts.jar content.jar p2.index; do
    test -f "$repository/$required" || {
        echo "Missing p2 file: $repository/$required" >&2
        exit 1
    }
done

if find "$repository" -maxdepth 1 -name 'composite*' | grep -q .; then
    echo "Composite metadata is not allowed in the active update site." >&2
    exit 1
fi

plugin_count=$(find "$repository/plugins" -maxdepth 1 -name 'com.brianwalker.dbeaver.resultsvisualizer_*.jar' | wc -l | tr -d ' ')
feature_count=$(find "$repository/features" -maxdepth 1 -name 'com.brianwalker.dbeaver.resultsvisualizer.feature_*.jar' | wc -l | tr -d ' ')
test "$plugin_count" = "1" || {
    echo "Expected one Results Visualizer plugin JAR; found $plugin_count." >&2
    exit 1
}
test "$feature_count" = "1" || {
    echo "Expected one Results Visualizer feature JAR; found $feature_count." >&2
    exit 1
}

metadata=$(mktemp)
trap 'rm -f "$metadata"' EXIT HUP INT TERM
unzip -p "$repository/content.jar" content.xml > "$metadata"

versions=$(sed -n "s/.*<unit id='com\.brianwalker\.dbeaver\.resultsvisualizer\.feature\.feature\.group' version='\([^']*\)'.*/\1/p" "$metadata" | sort -u)
version_count=$(printf '%s\n' "$versions" | sed '/^$/d' | wc -l | tr -d ' ')
test "$version_count" = "1" || {
    echo "Expected one installable Results Visualizer version; found $version_count: $versions" >&2
    exit 1
}

find "$repository/plugins" "$repository/features" -maxdepth 1 -name '*.jar' -exec unzip -tq {} \; >/dev/null
unzip -tq "$repository/content.jar" >/dev/null
unzip -tq "$repository/artifacts.jar" >/dev/null

echo "Validated latest-only p2 repository version: $versions"
