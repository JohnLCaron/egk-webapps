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

Usage: RunRemoteTrustee options_list
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
   electionguard.webapps.keyceremonytrustee.RunRemoteTrusteeKt \
    -trusteeDir testOut/keyceremonytrustee/RunRemoteTrustee


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
    webapps.electionguard.keyceremony.RunRemoteKeyCeremonyKt \
    -in testInput/unchained \
    -out testOut/keyceremony/RunRemoteKeyCeremony
    
