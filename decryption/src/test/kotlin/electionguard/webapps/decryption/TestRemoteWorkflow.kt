package electionguard.webapps.decryption

import org.cryptobiotic.eg.election.PlaintextBallot
import org.cryptobiotic.eg.cli.RunAccumulateTally.Companion.runAccumulateBallots
import org.cryptobiotic.eg.cli.RunBatchEncryption.Companion.batchEncryption
import org.cryptobiotic.util.Stats
import org.cryptobiotic.eg.core.productionGroup
import org.cryptobiotic.eg.input.RandomBallotProvider
import org.cryptobiotic.eg.publish.makePublisher
import org.cryptobiotic.eg.publish.readElectionRecord
import org.cryptobiotic.eg.verifier.Verifier
import kotlin.test.Test
import kotlin.test.assertTrue

class TestRemoteWorkflow {
    val remoteUrl = "http://0.0.0.0:11190"
    val keyceremonyDir = "../testOut/remoteWorkflow/keyceremony"
    private val nballots = 25
    private val nthreads = 25

    @Test
    fun runWorkflowAllAvailable() {
        val workingDir =  "testOut/remoteWorkflow/allAvailableProto"
        val privateDir =  "$workingDir/private_data"
        val ballotsDir =  "${privateDir}/input"
        val invalidDir =  "${privateDir}/invalid"

        val group = productionGroup()

        // key ceremony was already run in RunRemoteKeyCeremonyTest
        val electionRecordIn = readElectionRecord(keyceremonyDir)
        println("ElectionInitialized read from $keyceremonyDir")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(electionRecordIn.manifest(), nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        /*
        public final fun batchEncryption(
            inputDir: kotlin.String,
            ballotDir: kotlin.String,
            device: kotlin.String,
            outputDir: kotlin.String?,
            encryptDir: kotlin.String?,
            invalidDir: kotlin.String?,
            nthreads: kotlin.Int,
            createdBy: kotlin.String?,
            check: org.cryptobiotic.eg.cli.RunBatchEncryption.Companion.CheckType = COMPILED_CODE,
            cleanOutput: kotlin.Boolean = COMPILED_CODE,
            anonymize: kotlin.Boolean = COMPILED_CODE

         */

        // encrypt
        batchEncryption(keyceremonyDir, ballotsDir, device = "testDevice", workingDir, workingDir, invalidDir, nthreads, "runWorkflowAllAvailable")

        /*
        public final fun runAccumulateBallots(
            inputDir: kotlin.String,
            outputDir: kotlin.String,
            encryptDir: kotlin.String?,
            name: kotlin.String,
            createdBy: kotlin.String
        } */
        // tally
        runAccumulateBallots(workingDir, workingDir, null, "RunWorkflow", "runWorkflowAllAvailable")

        // decrypt tally
        runRemoteDecrypt(group, workingDir, workingDir, remoteUrl, null, "RunRemoteWorkflowAll",
            false, "", null, "", null)


        // verify
        println("\nRun Verifier")
        val electionRecord = readElectionRecord(workingDir)
        val verifier = Verifier(electionRecord)
        val stats = Stats()
        val ok = verifier.verify(stats)
        stats.show()
        println("Verify is $ok")
        assertTrue(ok)
    }

    @Test
    fun runWorkflowSomeAvailable() {
        val workingDir =  "/home/snake/tmp/electionguard/RunRemoteWorkflowSome"
        val privateDir =  "$workingDir/private_data"
        val ballotsDir =  "${privateDir}/input"
        val invalidDir =  "${privateDir}/invalid"

        val group = productionGroup()

        // key ceremony was already run in RunRemoteKeyCeremonyTest
        val electionRecordIn = readElectionRecord(keyceremonyDir)
        println("ElectionInitialized read from  $keyceremonyDir")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(electionRecordIn.manifest(), nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(keyceremonyDir, ballotsDir, device = "testDevice", workingDir, workingDir, invalidDir, nthreads, "RunRemoteWorkflowSome")

        // tally
        runAccumulateBallots(workingDir, workingDir, null, "RunWorkflow", "RunRemoteWorkflowSome")

        // fun runRemoteDecrypt(
        //    group: GroupContext,
        //    inputDir: String,
        //    outputDir: String,
        //    remoteUrl: String,
        //    missing: String?,
        //    createdBy: String?,
        //    isSSL: Boolean,
        //    clientKeyStore: String,
        //    clientKeystorePassword: String?,
        //    clientName: String,
        //    clientPassword: String?,
        //): Boolean {

        // decrypt
        runRemoteDecrypt(group, workingDir, workingDir, remoteUrl, "2,4", "RunRemoteWorkflowSome",
            false, "", null, "", null)

        // verify
        println("\nRun Verifier")
        val electionRecord = readElectionRecord(workingDir)
        val verifier = Verifier(electionRecord)
        val stats = Stats()
        val ok = verifier.verify(stats)
        stats.show()
        println("Verify is $ok")
        assertTrue(ok)
    }
}
