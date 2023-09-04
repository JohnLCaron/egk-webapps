/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath /home/stormy/dev/github/egk-webapps/encryptserver/build/libs/encryptserver-all.jar \
    electionguard.webapps.server.RunEgkServerKt -in testInput/unchained -out testOut/encrypt/RunEgkServerUnchained

/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -classpath /home/stormy/dev/github/egk-webapps/encryptclient/build/libs/encryptclient-all.jar \
    electionguard.webapps.client.RunEgkClientKt -in testInput/unchained -out testOut/encrypt/RunEgkServerUnchained \
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