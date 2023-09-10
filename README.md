# ElectionGuard-Kotlin-Multiplatform Webapps

_last update 8/01/2023_

[ElectionGuard-Kotlin-Multiplatform (EKM)](https://github.com/danwallach/electionguard-kotlin-multiplatform) 
is a multiplatform Kotlin implementation of 
[ElectionGuard](https://github.com/microsoft/electionguard), version 2.0.0, available under an MIT-style open source 
[License](LICENSE). 

This repo contains web applications built on top of that library.

Currently Java 17 is required.

## Build the Egklib fat jar

**Fat Jars** include the library and all of its dependencies, and simplify the classpath.

1. Place latest egklib-jvm-2.0.0-SNAPSHOT.jar into the **libs/** directory in this repo (if needed).
2. Execute _./gradlew fatJar_ to create the egklib fat jar at **egklib/build/libs/egklib-all.jar**.
3. Use the fat jar into egklib/build/libs/ of the library repo, and use that repo as your working directory for its
   [Command Line Programs](https://github.com/votingworks/electionguard-kotlin-multiplatform/blob/main/docs/CommandLineInterface.md)

## Build the Webapps fat jars

1. Build all the webapps fat jars: _./gradlew clean assemble_
2. Fat jars for the webapps are now in their respective build/libs directories:
   1. **encryptserver/build/libs/encryptserver-all.jar**
   2. **encryptclient/build/libs/encryptclient-all.jar**
   3. **keyceremony/build/libs/keyceremony-all.jar**
   4. **keyceremonytrustee/build/libs/keyceremonytrustee-all.jar**   
   5. **decryption/build/libs/decryption-all.jar**
   6. **decryptingtrustee/build/libs/decryptingtrustee-all.jar**

## Remote Workflow

Here is a general diagram for the ElectionGuard workflow:

<img src="./images/Workflow.svg" alt="Workflow" width="1200"/>

For additional security, in a real election we might want to make sure that each trustee is the only one with access 
to its own **_secret key_**, by having each trustee run their own program on their own personal computer, and store 
their secret key on their own computer. In this way, neither the election administrator nor any of the other trustees 
has any kind of access to the secret key. 

Heres a diagram of the general way that works, where the separate boxes represent separate processes on separate 
computers. The processes communicate remotely, so this is called a **_Remote Workflow_**:

<img src="./images/RemoteProcesses.svg" alt="RemoteProcesses" width="1200"/>

This remote workflow is only needed for the Key Ceremony and the Decryption stages of the workflow, because thats the
only time the secret keys are written (Key Ceremony) or read (Decryption).

The examples below assume that you are in the top directory of the egk-webapps repo, to make the classpath easier to use.
In production, you may use any working directory and adjust the paths accordingly.

## Remote KeyCeremony

The election administrator runs the **keyceremony** program, which orchestrates the 
_Key Ceremony_ where the secret keys and the _joint election keys_ are generated.
Each trustee runs a seperate
**keyceremonytrustee** _process_ that starts up first and then waits for the keyceremony to send it
requests. When the Key Ceremony is done, each keyceremonytrustee writes its own secret key to wherever the
human trustee has configured. This secret key is then used later, when the trustees come together to decrypt the
election record.

You must first generate the _Manifest_ and _Election Configuration_ files, as detailed in
[Create an Election Configuration](https://github.com/votingworks/electionguard-kotlin-multiplatform/blob/main/docs/CommandLineInterface.md#create-an-election-configuration).
The output of that process give you both the **trusteeDir** for the keyceremonytrustee, and the **inputDir** for the
keyceremony.

### The keyceremonytrustee program

_(For debugging purposes, currently all the trustees are handled by a single KeyCeremonyRemoteTrustee server. We will
soon add the "each trustee in its own process" production workflow)_

````
Usage: RunKeyCeremonyTrustee options_list
Options: 
    --sslKeyStore, -keystore -> file path of the keystore file { String }
    --keystorePassword, -kpwd -> password for the entire keystore { String }
    --electionguardPassword, -epwd -> password for the electionguard entry { String }
    --trustees, -trusteeDir -> trustee output directory (always required) { String }
    --serverPort, -port -> listen on this port, default = 11183 { Int }
    --help, -h -> Usage info 
````

Example:

````
/usr/lib/jvm/jdk-19/bin/java \
  -classpath keyceremonytrustee/build/libs/keyceremonytrustee-all.jar \
  electionguard.webapps.keyceremonytrustee.RunKeyCeremonyTrusteeKt \
  -trusteeDir testOut/remoteWorkflow/keyceremony/trustees 
````

You should see something like:

````
KeyCeremonyRemoteTrustee
    isSsl = false
    serverPort = '11183'
    trusteeDir = 'testOut/remoteWorkflow/keyceremony'
KeyCeremonyRemoteTrustee server ready...
````

### The keyceremony program

Start up the keyceremonytrustee program first. Then:

````
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
````

Example:

````
/usr/lib/jvm/jdk-19/bin/java \
  -classpath keyceremony/build/libs/keyceremony-all.jar \
  electionguard.webapps.keyceremony.RunRemoteKeyCeremonyKt \
  --inputDir /home/stormy/dev/github/electionguard-kotlin-multiplatform/testOut/cliWorkflow/config \
  --outputDir testOut/remoteWorkflow/keyceremony 
````

You should see something like:

````
RunRemoteKeyCeremony
inputDir = '/home/stormy/dev/github/electionguard-kotlin-multiplatform/testOut/cliWorkflow/config'
outputDir = 'testOut/remoteWorkflow/keyceremony'
isSsl = false

response.status for trustee1 = 201 Created
response.status for trustee2 = 201 Created
response.status for trustee3 = 201 Created
trustee2 receivePublicKeys for trustee1 = 200 OK
trustee3 receivePublicKeys for trustee1 = 200 OK
trustee1 receivePublicKeys for trustee2 = 200 OK
trustee3 receivePublicKeys for trustee2 = 200 OK
trustee1 receivePublicKeys for trustee3 = 200 OK
trustee2 receivePublicKeys for trustee3 = 200 OK
trustee1 encryptedKeyShareFor trustee2 = 200 OK
trustee2 receiveEncryptedKeyShare from trustee1 = 200 OK
trustee1 encryptedKeyShareFor trustee3 = 200 OK
trustee3 receiveEncryptedKeyShare from trustee1 = 200 OK
trustee2 encryptedKeyShareFor trustee1 = 200 OK
trustee1 receiveEncryptedKeyShare from trustee2 = 200 OK
trustee2 encryptedKeyShareFor trustee3 = 200 OK
trustee3 receiveEncryptedKeyShare from trustee2 = 200 OK
trustee3 encryptedKeyShareFor trustee1 = 200 OK
trustee1 receiveEncryptedKeyShare from trustee3 = 200 OK
trustee3 encryptedKeyShareFor trustee2 = 200 OK
trustee2 receiveEncryptedKeyShare from trustee3 = 200 OK
trustee1 saveState from = 200 OK
trustee2 saveState from = 200 OK
trustee3 saveState from = 200 OK
RunTrustedKeyCeremony took 17260 millisecs
````

You can check that the Election Configuration file was written to the outputDir. 


## Remote Encryption

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

## Remote Decryption

The election administrator runs the **decryption** program, which orchestrates the
_Decryption_ workflow where the encrypted tally and (optionally) the challenged ballots are decrypted.
Each trustee runs a seperate
**decryptingtrustee** _process_ that starts up first and then waits for the decryption program to send it
requests. When the Decryption is done, the decrypted (aka plaintext) tally and ballots are written to the 
election record.

### The decryptingtrustee program

_(For debugging purposes, currently all the trustees are handled by a single KeyCeremonyRemoteTrustee server. We will
soon add the "each trustee in its own process" production workflow)_

````
Usage: RunDecryptingTrustee options_list
Options: 
    --sslKeyStore, -keystore -> file path of the keystore file { String }
    --keystorePassword, -kpwd -> password for the entire keystore { String }
    --electionguardPassword, -epwd -> password for the electionguard entry { String }
    --trustees, -trusteeDir -> trustee output directory (always required) { String }
    --serverPort, -port -> listen on this port, default = 11190 { Int }
    --help, -h -> Usage info
````

Example:

````
/usr/lib/jvm/jdk-19/bin/java \
  -classpath decryptingtrustee/build/libs/decryptingtrustee-all.jar \
  electionguard.webapps.decryptingtrustee.RunDecryptingTrusteeKt \
  -trusteeDir testOut/remoteWorkflow/keyceremony/trustees 
````

You should see something like:

````
RunDecryptingTrustee
  isSsl = false
  serverPort = '11190'
  trusteeDir = 'testOut/remoteWorkflow/keyceremony/trustees'
RunDecryptingTrustee server ready...
````

### The decryption program

Start up the decryptingtrustee program first. Then:

````
Usage: RunRemoteDecryption options_list
Options: 
    --inputDir, -in -> Directory containing input election record (always required) { String }
    --outputDir, -out -> Directory to write output election record (always required) { String }
    --remoteUrl, -remoteUrl [http://localhost:11190/egk] -> URL of decrypting trustee app  { String }
    --createdBy, -createdBy -> who created { String }
    --missing, -missing -> missing guardians' xcoord, comma separated, eg '2,4' { String }
    --help, -h -> Usage info 
````

Example:

````
/usr/lib/jvm/jdk-19/bin/java \
  -classpath decryption/build/libs/decryption-all.jar \
  electionguard.webapps.decryption.RunRemoteDecryptionKt \
  --inputDir /home/stormy/dev/github/electionguard-kotlin-multiplatform/testOut/cliWorkflow/electionRecord \
  --outputDir testOut/remoteWorkflow/electionRecord
````

You should see something like:

````
RunRemoteDecryption starting
   input= /home/stormy/dev/github/electionguard-kotlin-multiplatform/testOut/cliWorkflow/electionRecord
   missing= 'null'
   output = testOut/remoteWorkflow/electionRecord
runRemoteDecrypt present = [trustee1, trustee2, trustee3] missing = []
runRemoteDecrypt reset 200 OK
DecryptingTrusteeProxy create trustee1 = 500 Internal Server Error
DecryptingTrusteeProxy create trustee2 = 500 Internal Server Error
DecryptingTrusteeProxy create trustee3 = 500 Internal Server Error
Exception in thread "main" io.ktor.client.call.NoTransformationFoundException: Expected response body of the type 'class electionguard.json2.DecryptResponseJson (Kotlin reflection is not available)' but was 'class io.ktor.utils.io.ByteBufferChannel (Kotlin reflection is not available)'
In response from `http://localhost:11190/egk/dtrustee/1/decrypt`
Response status `404 Not Found`
Response header `ContentType: text/plain; charset=UTF-8` 
Request header `Accept: application/json`

````

You can check that the Decrypted tally file was written to the outputDir.



### Make KeyStore

To use HTTPS between remote processes, we need a digital certificate. You may supply your own keystore, or use the
__MakeKeystore__ CLI (in keyceremonytrustee test directory).
This will generate a self-signed certificate and write it to a JKS keystore, to be used in the webapps.
The certificate _alias_ = "electionguard" and _domains_ = listOf("127.0.0.1", "0.0.0.0", "localhost").

````
Usage: MakeKeyStore options_list
Options: 
Options: 
    --keystorePassword, -kpwd -> password for the entire keystore (always required) { String }
    --electionguardPassword, -epwd -> password for the electionguard certificate entry (always required) { String }
    --sslKeyStore, -keystore -> write the keystore file to this path, default webapps/keystore.jks { String }
    --help, -h -> Usage info 
````

Example

````
java -classpath <classpath> webapps.electionguard.MakeKeystoreKt -kpwd keystorePassword -epwd egPassword
````

output:

````
MakeKeyStore
 keystorePassword = 'ksPassword' electionguardPassword = 'egPassword'
 write to path = 'webapps/keystore.jks'
````
