#!/bin/bash

INPUT_DIR=$1
TRUSTEE_DIR=$2
OUTPUT_DIR=$3

if [ -z "${INPUT_DIR}" ]; then
    echo "No input directory provided."
    exit 1
fi

if [ -z "${TRUSTEE_DIR}" ]; then
    echo "No trustee directory provided."
    exit 1
fi

if [ -z "${OUTPUT_DIR}" ]; then
    echo "No output directory provided."
    exit 1
fi

echo ""
echo "***keyceremony"


java -classpath keyceremonytrustee/build/libs/keyceremonytrustee-all.jar \
   electionguard.webapps.keyceremonytrustee.RunKeyCeremonyTrusteeKt \
   -trusteeDir $TRUSTEE_DIR &

SERVER_PID=$!
sleep 5

java -classpath keyceremony/build/libs/keyceremony-all.jar \
  electionguard.webapps.keyceremony.RunRemoteKeyCeremonyKt \
  --inputDir ${INPUT_DIR} \
  --outputDir ${OUTPUT_DIR}

kill ${SERVER_PID}

echo " [DONE] keyceremony "
