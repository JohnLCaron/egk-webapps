/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath /home/stormy/dev/github/egk-webapps/encryptserver/build/libs/encryptserver-all.jar \
    electionguard.webapps.server.RunEgkServerKt -in testInput/unchained -out testOut/encrypt/RunEgkServerUnchained

/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath /home/stormy/dev/github/egk-webapps/encryptclient/build/libs/encryptclient-all.jar \
    electionguard.webapps.client.RunEgkClientKt \
    -in testInput/unchained \
    -out testOut/encrypt/RunEgkServerUnchained \
    -device RunEgkClient

-----------------------


/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath /home/stormy/dev/github/egk-webapps/encryptserver/build/libs/encryptserver-all.jar \
    electionguard.webapps.server.RunEgkServerKt -in testInput/chained -out testOut/encrypt/RunEgkServerChained


/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath /home/stormy/dev/github/egk-webapps/encryptclient/build/libs/encryptclient-all.jar \
    electionguard.webapps.client.RunEgkClientKt -in testInput/chained -out testOut/encrypt/RunEgkServerChained \
    -device RunEgkClient

/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath /home/stormy/dev/github/egk-webapps/encryptclient/build/libs/encryptclient-all.jar \
    electionguard.webapps.client.RunEgkClientKt -in testInput/chained \
    -device RunEgkClient

/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath /home/stormy/dev/github/egk-webapps/encryptclient/build/libs/encryptclient-all.jar \
    electionguard.webapps.client.RunEgkClientKt -in testInput/chained -out testOut/encrypt/RunEgkServerChained \
    -device RunEgkClient -nballots 1

--------------------

Usage: RunKeyCeremonyTrustee options_list
Options:
--sslKeyStore, -keystore -> file path of the keystore file { String }
--keystorePassword, -kpwd -> password for the entire keystore { String }
--electionguardPassword, -epwd -> password for the electionguard entry { String }
--trustees, -trusteeDir -> trustee output directory (always required) { String }
--serverPort, -port -> listen on this port, default = 11183 { Int }
--help, -h -> Usage info

/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath /home/stormy/dev/github/egk-webapps/keyceremonytrustee/build/libs/keyceremonytrustee-all.jar \
    electionguard.webapps.keyceremonytrustee.RunKeyCeremonyTrusteeKt \
    -trusteeDir testOut/keyceremonytrustee/RunKeyCeremonyTrustee


Usage: RunRemoteKeyCeremony options_list
Options:
--inputDir, -in -> Directory containing input ElectionConfig record { String }
--electionManifest, -manifest -> Manifest file or directory (json or protobuf) { String }
--nguardians, -nguardians -> number of guardians { Int }
--quorum, -quorum -> quorum size { Int }
--outputDir, -out -> Directory to write output ElectionInitialized record (always required) { String }
--remoteUrl, -remoteUrl [http://localhost:11183/egk] -> URL of keyceremony trustee webapp  { String }
--createdBy, -createdBy -> who created for ElectionInitialized metadata { String }
--help, -h -> Usage info

/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath /home/stormy/dev/github/egk-webapps/keyceremony/build/libs/keyceremony-all.jar \
    electionguard.webapps.keyceremony.RunRemoteKeyCeremonyKt \
    -in testInput/chained \
    -out testOut/keyceremony/RunRemoteKeyCeremonyChained
    
--------------------

Usage: RunDecryptingTrustee options_list
Options:
--sslKeyStore, -keystore -> file path of the keystore file { String }
--keystorePassword, -kpwd -> password for the entire keystore { String }
--electionguardPassword, -epwd -> password for the electionguard entry { String }
--trustees, -trusteeDir -> trustee output directory (always required) { String }
--serverPort, -port -> listen on this port, default = 11190 { Int }
--help, -h -> Usage info

/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath /home/stormy/dev/github/egk-webapps/decryptingtrustee/build/libs/decryptingtrustee-all.jar \
    electionguard.webapps.decryptingtrustee.RunDecryptingTrusteeKt \
    -trusteeDir /home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/testOut/workflow/chainedJson/private_data/trustees

Usage: RunRemoteDecryption options_list
Options:
--inputDir, -in -> Directory containing input election record (always required) { String }
--outputDir, -out -> Directory to write output election record (always required) { String }
--remoteUrl, -remoteUrl [http://localhost:11183/egk] -> URL of decrypting trustee app  { String }
--createdBy, -createdBy -> who created { String }
--missing, -missing -> missing guardians' xcoord, comma separated, eg '2,4' { String }
--help, -h -> Usage info

/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath /home/stormy/dev/github/egk-webapps/decryption/build/libs/decryption-all.jar \
    electionguard.webapps.decryption.RunRemoteDecryptionKt \
    -in /home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/testOut/workflow/chainedJson \
    -out testOut/decryption/RunRemoteDecryption

==============================

/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -jar /home/stormy/dev/github/egk-webapps/egklib/build/libs/egklib-all.jar \
    -in /home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/testOut/workflow/chainedJson/
    
