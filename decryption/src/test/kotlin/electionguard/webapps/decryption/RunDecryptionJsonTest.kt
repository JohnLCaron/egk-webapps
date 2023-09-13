package electionguard.webapps.decryption

import kotlin.test.Test
import kotlin.test.assertTrue

/** Test Decryption with in-process DecryptingTrustee's. */
class RunDecryptionJsonTest {

    // /usr/lib/jvm/jdk-19/bin/java \
    //  -classpath decryptingtrustee/build/libs/decryptingtrustee-all.jar \
    //  electionguard.webapps.decryptingtrustee.RunDecryptingTrusteeKt \
    //  -trusteeDir /home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/workflow/allAvailableJson/private_data/trustees
    @Test
    fun testDecryptionAll() {
        val inputDir = "/home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/workflow/allAvailableJson/"
        main(
            arrayOf(
                "-in", inputDir,
                "-out", "../testOut/remoteWorkflow/testDecryptionAll",
                "-createdBy", "testDecryptionAll",
            )
        )
    }

    /*
    /usr/lib/jvm/jdk-19/bin/java \
  -classpath decryptingtrustee/build/libs/decryptingtrustee-all.jar \
  electionguard.webapps.decryptingtrustee.RunDecryptingTrusteeKt \
  -trusteeDir /home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/workflow/someAvailableJson/private_data/trustees
     */
    @Test
    fun testDecryptionSome() {
        val inputDir = "/home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/workflow/someAvailableJson/"
        main(
            arrayOf(
                "-in", inputDir,
                "-out", "../testOut/remoteWorkflow/testDecryptionSome",
                "-trusteeHost", "localhost",
                "-serverPort", "11190",
                "-missing", "3"
            )
        )
    }
}
