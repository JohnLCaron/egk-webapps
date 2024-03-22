# ~/.bashrc
INPUT="/home/stormy/dev/github/egk-ec/src/test/data"
OUTPUT="/home/stormy/tmp/testOut/egkwebapps"

./scripts/keyceremony.sh ${INPUT}/startConfigEc $OUTPUT/keyceremony/trustees $OUTPUT/keyceremony
./scripts/encryption.sh $OUTPUT/keyceremony $OUTPUT/electionRecord
./scripts/tally.sh $OUTPUT/electionRecord $OUTPUT/electionRecord
./scripts/decryption.sh $OUTPUT/keyceremony/trustees $OUTPUT/electionRecord $OUTPUT/electionRecord
./scripts/validate.sh $OUTPUT/electionRecord
