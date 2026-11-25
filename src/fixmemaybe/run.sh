#!/bin/sh
# Fix Me Maybe - start on Mac / Linux:  ./run.sh
cd "$(dirname "$0")"
rm -rf out
javac -encoding UTF-8 -d out $(find src -name "*.java") || exit 1
java -cp out fixmemaybe.Main
