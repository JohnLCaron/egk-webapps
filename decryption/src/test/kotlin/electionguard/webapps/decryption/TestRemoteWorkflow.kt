package electionguard.webapps.decryption

import electionguard.ballot.PlaintextBallot
import electionguard.cli.RunAccumulateTally.Companion.runAccumulateBallots
import electionguard.cli.RunBatchEncryption.Companion.batchEncryption
import electionguard.util.Stats
import electionguard.core.productionGroup
import electionguard.input.RandomBallotProvider
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.verifier.Verifier
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
        val electionRecordIn = readElectionRecord(group, keyceremonyDir)
        println("ElectionInitialized read from $keyceremonyDir")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(electionRecordIn.manifest(), nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        //         fun batchEncryption(
        //            group: GroupContext,
        //            inputDir: String,
        //            ballotDir: String,
        //            device: String,
        //            outputDir: String?,
        //            encryptDir: String?,
        //            invalidDir: String?,
        //            nthreads: Int,
        //            createdBy: String?,
        //            check: CheckType = CheckType.None,
        //            cleanOutput: Boolean = false,
        //            anonymize: Boolean = false,
        // encrypt
        batchEncryption(group, keyceremonyDir, ballotsDir, device = "testDevice", workingDir, workingDir, invalidDir, nthreads, "runWorkflowAllAvailable")

        //         fun runAccumulateBallots(
        //            group: GroupContext,
        //            inputDir: String,
        //            outputDir: String,
        //            encryptDir: String?,
        //            name: String,
        //            createdBy: String
        // tally
        runAccumulateBallots(group, workingDir, workingDir, null, "RunWorkflow", "runWorkflowAllAvailable")

        // decrypt tally
        runRemoteDecrypt(group, workingDir, workingDir, remoteUrl, null, "RunRemoteWorkflowAll",
            false, "", null, "", null)


        // verify
        println("\nRun Verifier")
        val electionRecord = readElectionRecord(group, workingDir)
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
        val electionRecordIn = readElectionRecord(group, keyceremonyDir)
        println("ElectionInitialized read from  $keyceremonyDir")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(electionRecordIn.manifest(), nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, keyceremonyDir, ballotsDir, device = "testDevice", workingDir, workingDir, invalidDir, nthreads, "RunRemoteWorkflowSome")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, null, "RunWorkflow", "RunRemoteWorkflowSome")

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
        val electionRecord = readElectionRecord(group, workingDir)
        val verifier = Verifier(electionRecord)
        val stats = Stats()
        val ok = verifier.verify(stats)
        stats.show()
        println("Verify is $ok")
        assertTrue(ok)
    }
}
