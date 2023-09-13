package webapps.electionguard.client

import electionguard.webapps.client.main
import kotlin.test.Test

class RunEgkClientTest {
    private val homeDir = "/home/stormy/dev/github/egk-webapps"

    @Test
    fun testRunEgkClientTest() {
        main(
            arrayOf(
                "-in", "$homeDir/testInput/unchained",
                "-device", "precinct42",
                "-out", "$homeDir/testOut/encrypt/RunEgkServer",
                "-nballots", "3",
                "--serverHost", "localhost",
                "--serverPort", "11111",
                "--sslKeyStore", "$homeDir/egKeystore.jks",
                "--keystorePassword", "crypto",
                "--electionguardPassword", "biotic",
            )
        )
    }

}

