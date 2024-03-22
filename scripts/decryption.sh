#!/bin/bash

TRUSTEE_DIR=$1
INPUT_DIR=$2
OUTPUT_DIR=$3

if [ -z "${INPUT_DIR}" ]; then
    echo "No input directory provided."
    exit 1
fi

if [ -z "${INPUT_DIR}" ]; then
    echo "No input directory provided."
    exit 1
fi

if [ -z "${OUTPUT_DIR}" ]; then
    echo "No output directory provided."
    exit 1
fi

echo ""
echo "***decryption"

java -classpath decryptingtrustee/build/libs/decryptingtrustee-all.jar \
   electionguard.webapps.decryptingtrustee.RunDecryptingTrusteeKt \
   -trusteeDir $TRUSTEE_DIR  &

SERVER_PID=$!
sleep 5

java -classpath decryption/build/libs/decryption-all.jar \
    electionguard.webapps.decryption.RunRemoteDecryptionKt \
    --inputDir  $INPUT_DIR \
    --outputDir $OUTPUT_DIR

kill ${SERVER_PID}

echo " [DONE] decryption "
