#!/bin/sh
set -eu
cd "$(dirname "$0")"
sh ./build.sh
javac -encoding UTF-8 --release 17 -cp build/classes -d build/test-classes tests/library/*.java
java -cp build/classes:build/test-classes library.UiSmoke evidence
java -cp build/classes:build/test-classes library.UiWorkflow
