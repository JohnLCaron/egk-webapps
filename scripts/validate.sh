#!/bin/bash

INPUT_DIR=$1

if [ -z "${INPUT_DIR}" ]; then
    echo "No input directory provided."
    exit 1
fi

echo ""
echo "***validate"

java -classpath libs/egk-ec-2.1-SNAPSHOT-uber.jar \
  org.cryptobiotic.eg.cli.RunVerifier \
  -in $INPUT_DIR

echo " [DONE] validate "
