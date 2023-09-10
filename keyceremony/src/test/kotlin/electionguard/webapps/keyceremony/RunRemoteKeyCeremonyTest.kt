package electionguard.webapps.keyceremony

import kotlin.test.Test

class RunRemoteKeyCeremonyTest {
    private val configDir = "/home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/startConfigProto"
    private val outputDir = "../testOut/remoteWorkflow/RunRemoteKeyCeremonyTest"

    @Test
    fun testRemoteKeyCeremonyMain() {
        main(
            arrayOf(
                "-in",
                configDir,
                "-out",
                outputDir,
                "-remoteUrl", "http://localhost:11183/egk",
                "-keystore", "../keystore.jks",
                "-kpwd", "ksPassword",
                "-epwd", "egPassword",
            )
        )
    }

}

