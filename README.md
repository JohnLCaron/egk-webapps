# ElectionGuard-Kotlin-Multiplatform Webapps

_last update 7/30/2023_

[ElectionGuard-Kotlin-Multiplatform (EKM)](https://github.com/danwallach/electionguard-kotlin-multiplatform) 
is a multiplatform Kotlin implementation of 
[ElectionGuard](https://github.com/microsoft/electionguard), version 2.0.0, available under an MIT-style open source 
[License](LICENSE).

This repo contains applications built on top of that library.

Currently Java 17 is required.

## Egklib fatJar

1. Place latest egklib-jvm-2.0.0-SNAPSHOT.jar into the **libs/** directory in this repo.
2. Execute `./gradlew :egklib:fatJar` to create the egklib fat jar at **egklib/build/libs/egklib-2.0.0-all.jar**.
3. Use the fat jar like:
````
/usr/lib/jvm/jdk-19/bin/java \
  -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
  -classpath egklib/build/libs/egklib-2.0.0-all.jar \
  electionguard.cli.RunCreateElectionConfig
````

## Encryption Server

This is an http web server (aka webapp) that does ballot encryption. 
Its purpose is to provide ballot encryption to remote clients, or to non-JVM programs.
It uses JetBrains ktor web framework.

## Encryption Client

This is an integration test of the Encryption Server.

## Authors
- [John Caron](https://github.com/JohnLCaron)
- [Dan S. Wallach](https://www.cs.rice.edu/~dwallach/)