package electionguard.webapps.decryption

import kotlin.test.Test
import kotlin.test.assertTrue

/** Test Decryption with in-process DecryptingTrustee's. */
class RunDecryptionJsonTest {
    val remoteUrl = "http://0.0.0.0:11190/egk"

    @Test
    fun testDecryptionAll() {
        val inputDir = "/home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/workflow/allAvailableJson/"
        main(
            arrayOf(
                "-in",
                inputDir,
                "-out",
                "../testOut/remoteWorkflow/testDecryptionAll",
                "-createdBy",
                "RunDecryptionJsonTest",
            )
        )
    }

    @Test
    fun testDecryptionSome() {
        val inputDir = "/home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/workflow/someAvailableJson/"
        main(
            arrayOf(
                "-in",
                inputDir,
                "-out",
                "../testOut/remoteWorkflow/testDecryptionSome",
                "-createdBy",
                "RunDecryptionJsonTest",
                "-remoteUrl",
                remoteUrl,
                "-missing",
                "3"
            )
        )
    }
}
