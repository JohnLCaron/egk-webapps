# ElectionGuard-Kotlin-Multiplatform Webapps

_last update 8/01/2023_

[ElectionGuard-Kotlin-Multiplatform (EKM)](https://github.com/danwallach/electionguard-kotlin-multiplatform) 
is a multiplatform Kotlin implementation of 
[ElectionGuard](https://github.com/microsoft/electionguard), version 2.0.0, available under an MIT-style open source 
[License](LICENSE). 

This repo contains web applications built on top of that library.

Currently Java 17 is required.

## Egklib fat jar

**Fat Jars** include the library and all of its dependencies, and simplify the classpath.

1. Place latest egklib-jvm-2.0.0-SNAPSHOT.jar into the **libs/** directory in this repo (if needed).
2. Execute _./gradlew :egklib:fatJar_ to create the egklib fat jar at **egklib/build/libs/egklib-2.0.0-all.jar**.
3. Use the fat jar as the classpath:
````
/usr/lib/jvm/jdk-19/bin/java \
  -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
  -classpath egklib/build/libs/egklib-2.0.0-all.jar \
  electionguard.cli.RunCreateElectionConfig
````

## Webapps fat jars

1. Build all the fat jars: _./gradlew clean assemble_
2. Fat jars for the webapps are now in their respective build/libs directories:
   1. **encryptserver/build/libs/encryptserver-all.jar**
   2. **encryptclient/build/libs/encryptclient-all.jar**
3. Make the top directory of the egk-webapps repo your working directory (eg _/home/stormy/dev/github/egk-webapps_), 
   or adjust the paths below.

### Encryption Server

This is an HTTP web server (aka webapp) that does ballot encryption. 

Its purpose is to provide ballot encryption to remote clients, or to non-JVM programs.
It uses JetBrain's [ktor web framework](https://ktor.io/).
Using SSL is not ready yet.

````
Usage: RunEgkServerKt options_list
Options: 
    --inputDir, -in -> Directory containing input election record (always required) { String }
    --outputDir, -out -> Directory containing output election record (always required) { String }
    --sslKeyStore, -keystore -> file path of the keystore file { String }
    --keystorePassword, -kpwd -> password for the entire keystore { String }
    --electionguardPassword, -epwd -> password for the electionguard entry { String }
    --serverPort, -port -> listen on this port, default = 11111 { Int }
    --help, -h -> Usage info 
````

Example:

````
/usr/lib/jvm/jdk-19/bin/java \
  -classpath encryptserver/build/libs/encryptserver-all.jar \
  electionguard.webapps.server.RunEgkServerKt \
  --inputDir testInput/unchained \
  --outputDir testOut/encrypt/RunEgkServer
````

### Encryption Client

This is an integration test of the Encryption Server. Start up the Encryption Server as above before running.

````
Usage: RunEgkClientKt options_list
Options: 
    --inputDir, -in -> Directory containing input election record, for generating test ballots (always required) { String }
    --device, -device [testDevice] -> Device name { String }
    --serverUrl, -server [http://localhost:11111/egk] -> Server URL { String }
    --outputDir, -out -> Directory containing output election record, optional for validating { String }
    --help, -h -> Usage info 
````

Example:

````
/usr/lib/jvm/jdk-19/bin/java \
  -classpath encryptclient/build/libs/encryptclient-all.jar \
  electionguard.webapps.client.RunEgkClientKt \
  --inputDir testInput/unchained \
  --outputDir testOut/encrypt/RunEgkServer
````

## Authors
- [John Caron](https://github.com/JohnLCaron)
- [Dan S. Wallach](https://www.cs.rice.edu/~dwallach/)