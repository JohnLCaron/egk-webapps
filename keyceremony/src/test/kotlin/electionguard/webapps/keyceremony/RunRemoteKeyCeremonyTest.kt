package electionguard.webapps.keyceremony

import kotlin.test.Test

class RunRemoteKeyCeremonyTest {
    private val configDir = "/home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/startConfigProto"
    private val outputDir = "../testOut/remoteWorkflow/RunRemoteKeyCeremonyTest"

    @Test
    fun testRemoteKeyCeremonyMain() {
        main(
            arrayOf(
                "-in", configDir,
                "-out", outputDir,
                "-trusteeHost", "localhost",
                "-serverPort", "11183",
                "-keystore", "../egKeystore.jks",
                "-kpwd", "crypto",
                "-epwd", "biotic",
            )
        )
    }

}

// Usage: RunRemoteKeyCeremony options_list
//Options:
//    --inputDir, -in -> Directory containing input ElectionConfig record (always required) { String }
//    --outputDir, -out -> Directory to write output ElectionInitialized record (always required) { String }
//    --serverHost, -trusteeHost [localhost] -> hostname of keyceremony trustee webapp  { String }
//    --serverPort, -serverPort [11183] -> port of keyceremony trustee webapp  { Int }
//    --createdBy, -createdBy -> who created (for ElectionInitialized metadata) { String }
//    --sslKeyStore, -keystore [egKeystore.jks] -> file path of the keystore file { String }
//    --keystorePassword, -kpwd -> password for the keystore file { String }
//    --electionguardPassword, -epwd -> password for the electionguard entry { String }
//    --help, -h -> Usage info