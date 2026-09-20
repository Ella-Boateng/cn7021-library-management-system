#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes build/test-classes dist
javac -encoding UTF-8 --release 17 -d build/classes src/library/*.java
jar --create --file dist/library-management.jar --main-class library.Main -C build/classes .
echo "Built dist/library-management.jar"
