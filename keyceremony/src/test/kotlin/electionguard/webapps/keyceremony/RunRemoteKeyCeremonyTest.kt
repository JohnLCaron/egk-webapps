package electionguard.webapps.keyceremony

import org.cryptobiotic.eg.core.productionGroup
import org.cryptobiotic.eg.publish.Consumer
import org.cryptobiotic.eg.publish.makeConsumer
import kotlin.test.Test
import kotlin.test.assertNotNull

// Requires the (SSL)  KeyCeremonyTrustee app to be running.
class RunRemoteKeyCeremonyTest {

    @Test
    fun testRemoteKeyCeremonySSLproto() {
        val configDir = "/home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/startConfigProto"
        val outputDir = "../testOut/remoteWorkflow/testRemoteKeyCeremonySSLproto"

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
        val check = makeConsumer(outputDir)
        assertNotNull(check)
    }

    @Test
    fun testRemoteKeyCeremonySSLjson() {
        val configDir = "/home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/startConfigJson"
        val outputDir = "../testOut/remoteWorkflow/testRemoteKeyCeremonySSLjson"

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
        val check = makeConsumer(outputDir)
        assertNotNull(check)
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