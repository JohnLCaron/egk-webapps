package electionguard.webapps.client

import com.github.michaelbull.result.unwrap
import electionguard.core.ElGamalPublicKey
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.input.RandomBallotProvider
import electionguard.publish.makeConsumer
import electionguard.publish.readElectionRecord
import electionguard.verifier.VerifyEncryptedBallots


import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*


val remoteUrl = "http://localhost:11111/egk"
val inputDir = "testInput"
val outputDir = "testOut/encrypt/RunEgkServer"
val chained = true
val device = "device11"

fun main(args: Array<String>) {
    val client = HttpClient(Java) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
            // level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json()
        }
        //engine {
        //    config {
        //        sslContext(SslSettings.getSslContext())
        //    }
        // }
    }
    val proxy = RemoteEncryptorProxy(client, remoteUrl)

    val group = productionGroup()
    val electionRecord = readElectionRecord(group, inputDir)
    val electionInit = electionRecord.electionInit()!!

    // encrypt 7 randomly generated ballots
    val ballotProvider = RandomBallotProvider(electionRecord.manifest())
    repeat(7) {
        val ballot = ballotProvider.makeBallot()
        val resultEncrypt = proxy.encryptBallot(device, ballot)
        val ccode = resultEncrypt.unwrap()
        println(" encrypt ${ballot.ballotId} -> $ccode")
        val resultCast = proxy.castBallot(device, ccode)
        println(" cast ${ballot.ballotId} -> $resultCast")
    }

    // write out the results to outputDir
    // encryptor.close()

    // verify
    verifyOutput(group, outputDir, chained)
}

fun verifyOutput(group: GroupContext, outputDir: String, chained: Boolean = false) {
    val consumer = makeConsumer(outputDir, group, false)
    var count = 0
    consumer.iterateAllEncryptedBallots { true }.forEach {
        count++
    }
    println("$count EncryptedBallots")

    val record = readElectionRecord(consumer)
    val verifier = VerifyEncryptedBallots(
        group, record.manifest(),
        ElGamalPublicKey(record.jointPublicKey()!!),
        record.extendedBaseHash()!!,
        record.config(), 1
    )

    // Note we are verifying all ballots, not just CAST
    val verifierResult = verifier.verifyBallots(record.encryptedAllBallots { true })
    println("verifyEncryptedBallots $verifierResult")

    if (chained) {
        val chainResult = verifier.verifyConfirmationChain(record)
        println(" verifyChain $chainResult")
    }
}