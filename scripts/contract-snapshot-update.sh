#!/usr/bin/env bash
# Regenerates a module's contract-testing baseline (<module>/api-report/*.baseline.jar)
# from the CURRENT working tree and overwrites the committed snapshot.
#
# Run this after an intentional public API change, review the resulting git diff on
# the jar (a new binary blob) alongside your code change, and commit both together.
# This is the only way a committed baseline should ever change - japicmp never pulls
# a published version for the comparison.
#
#   scripts/contract-snapshot-update.sh skyvault    # regenerate one module
#   scripts/contract-snapshot-update.sh flowvault
#   scripts/contract-snapshot-update.sh             # both
#
# Prefer naming the module you actually changed. Jar archives embed timestamps, so
# regenerating a module whose API did not change still produces different bytes and
# a spurious diff on a binary file - which is exactly the thing a reviewer cannot
# eyeball. Only the baseline you intend to move should appear in the commit.
set -euo pipefail

cd "$(dirname "$0")/.."

# module -> artifactId, which is also the baseline jar's name
declare -A ARTIFACTS=(
    [skyvault]="skyflow-java"
    [flowvault]="skyflow-flowvault-java"
)

MODULES=("$@")
if [ ${#MODULES[@]} -eq 0 ]; then
    MODULES=(skyvault flowvault)
fi

for MODULE in "${MODULES[@]}"; do
    ARTIFACT="${ARTIFACTS[$MODULE]:-}"
    if [ -z "$ARTIFACT" ]; then
        echo "Error: unknown module '$MODULE'. Expected one of: ${!ARTIFACTS[*]}"
        exit 1
    fi

    echo "=== $MODULE ==="
    mvn -B package -pl "common,$MODULE" -am -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true

    # the comparison-only jar, which merges com.skyflow:common into the module
    SHADED_JAR=$(ls "$MODULE"/target/"$ARTIFACT"-*-with-common.jar 2>/dev/null | head -n1)
    if [ -z "$SHADED_JAR" ]; then
        echo "Error: could not find $MODULE/target/$ARTIFACT-*-with-common.jar. Did the build succeed?"
        exit 1
    fi

    mkdir -p "$MODULE/api-report"
    cp "$SHADED_JAR" "$MODULE/api-report/$ARTIFACT.baseline.jar"
    echo "Updated $MODULE/api-report/$ARTIFACT.baseline.jar from $SHADED_JAR"
done

echo "--------------------------"
echo "Review the diff and commit the baseline jar(s) alongside your API change."
