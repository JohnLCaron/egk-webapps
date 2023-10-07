package webapps.electionguard.client

import electionguard.webapps.client.main
import kotlin.test.Test

// server must be running
class RunEgkClientTest {
    private val homeDir = "/home/stormy/dev/github/egk-webapps"

    @Test
    fun testRunEgkClientTest() {
        main(
            arrayOf(
                "-in", "$homeDir/testInput/unchained",
                "-device", "precinct42",
                "-out", "$homeDir/testOut/encrypt/RunEgkServer",
                "-saveBallots", "$homeDir/testOut/encrypt/RunEgkServer/secret/input",
                "-nballots", "11",
                "--serverHost", "localhost",
                "--serverPort", "11111",
                "--sslKeyStore", "$homeDir/egKeystore.jks",
                "--keystorePassword", "crypto",
                "--electionguardPassword", "biotic",
            )
        )
    }

}

