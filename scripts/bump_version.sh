# Input Arguments
Version=$1
CommitHash=$2
PomFile="$GITHUB_WORKSPACE/pom.xml"

if [ -z "$Version" ]; then
    echo "Error: Version argument is required."
    exit 1
fi

# Update only the main project's <version>
if [ -z "$CommitHash" ]; then
    echo "Bumping main project version to $Version"

    awk -v version="$Version" '
        BEGIN { updated = 0 }
        /<version>/ && updated == 0 {
            sub(/<version>.*<\/version>/, "<version>" version "</version>")
            updated = 1
        }
        { print }
        ' "$PomFile" > tempfile && cat tempfile > "$PomFile" && rm -f tempfile

    echo "--------------------------"
    echo "Done. Main project version now at $Version"
else
    echo "Bumping main project version to $Version-dev-$CommitHash"

     awk -v version="$Version-dev.$CommitHash" '
            BEGIN { updated = 0 }
            /<version>/ && updated == 0 {
                sub(/<version>.*<\/version>/, "<version>" version "</version>")
                updated = 1
            }
            { print }
            ' "$PomFile" > tempfile && cat tempfile > "$PomFile" && rm -f tempfile

    echo "--------------------------"
    echo "Done. Main project version now at $Version-dev.$CommitHash"
fi

