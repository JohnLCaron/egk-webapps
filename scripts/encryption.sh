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
echo "***encryption"


java -classpath encryptserver/build/libs/encryptserver-all.jar \
   electionguard.webapps.server.RunEgkServerKt \
   --inputDir $INPUT_DIR \
   --outputDir $OUTPUT_DIR &

SERVER_PID=$!
sleep 5

java  -classpath encryptclient/build/libs/encryptclient-all.jar \
   electionguard.webapps.client.RunEgkClientKt \
   --inputDir $INPUT_DIR \
   -nballots 42 \
   --outputDir $OUTPUT_DIR \
   --saveBallotsDir $OUTPUT_DIR/secret/input

kill ${SERVER_PID}

echo " [DONE] encryption "
