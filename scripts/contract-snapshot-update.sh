#!/usr/bin/env bash
# Regenerates the skyvault contract-testing baseline (api-report/skyflow-java.baseline.jar)
# from the CURRENT working tree and overwrites the committed snapshot.
#
# Run this after an intentional public API change to v2, review the resulting
# git diff on the jar (a new binary blob) alongside your code change, and commit
# both together. This is the only way the committed baseline should ever change -
# japicmp never pulls a published version for this comparison.
set -euo pipefail

cd "$(dirname "$0")/.."

mvn -B package -pl common,v2 -am -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true

SHADED_JAR=$(ls skyvault/target/skyflow-java-*-with-common.jar | head -n1)

if [ -z "$SHADED_JAR" ]; then
    echo "Error: could not find a built skyvault/target/skyflow-java-*-with-common.jar. Did the build succeed?"
    exit 1
fi

mkdir -p skyvault/api-report
cp "$SHADED_JAR" skyvault/api-report/skyflow-java.baseline.jar

echo "--------------------------"
echo "Updated skyvault/api-report/skyflow-java.baseline.jar from $SHADED_JAR"
echo "Review the diff and commit this file alongside your API change."
