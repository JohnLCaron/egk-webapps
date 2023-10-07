package electionguard.webapps.decryption

import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.cli.RunAccumulateTally.Companion.runAccumulateBallots
import electionguard.cli.RunBatchEncryption.Companion.batchEncryption
import electionguard.core.GroupContext
import electionguard.core.Stats
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.input.RandomBallotProvider
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.publish.makeConsumer
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

        // encrypt
        batchEncryption(group, keyceremonyDir, workingDir, ballotsDir, invalidDir, "testDevice", nthreads, "runWorkflowAllAvailable")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "runWorkflowAllAvailable")

        // decrypt tally
        runRemoteDecrypt(group, workingDir, workingDir, remoteUrl, null, "RunRemoteWorkflowAll")

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
        batchEncryption(group, keyceremonyDir, workingDir, ballotsDir, invalidDir, "testDevice", nthreads, "RunRemoteWorkflowSome")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "RunRemoteWorkflowSome")

        // decrypt
        runRemoteDecrypt(group, workingDir, workingDir, remoteUrl, "2,4", "RunRemoteWorkflowSome")

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

fun readDecryptingTrustees(
    group: GroupContext,
    trusteeDir: String,
    init: ElectionInitialized,
    present: List<Int>,
): List<DecryptingTrusteeIF> {
    val consumer = makeConsumer(group, trusteeDir)
    return init.guardians.filter { present.contains(it.xCoordinate)}.map { consumer.readTrustee(trusteeDir, it.guardianId) }
}