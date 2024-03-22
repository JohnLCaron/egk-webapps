#!/bin/bash

INPUT_DIR=$1
OUTPUT_DIR=$2

if [ -z "${INPUT_DIR}" ]; then
    echo "No input directory provided."
    exit 1
fi

if [ -z "${OUTPUT_DIR}" ]; then
    echo "No output directory provided."
    exit 1
fi

echo ""
echo "***tally"

java -classpath libs/egk-ec-2.1-SNAPSHOT-uber.jar \
       org.cryptobiotic.eg.cli.RunAccumulateTally \
         -in $INPUT_DIR \
         -count \
         -out $OUTPUT_DIR

echo " [DONE] tally "
