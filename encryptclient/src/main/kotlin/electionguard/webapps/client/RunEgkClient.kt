package electionguard.webapps.client

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
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
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import kotlin.random.Random

fun main(args: Array<String>) {
    val parser = ArgParser("RunEgkClientKt")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input election record, for generating test ballots"
    ).required()
    val device by parser.option(
        ArgType.String,
        shortName = "device",
        description = "Device name"
    ).default("testDevice")
    val serverUrl by parser.option(
        ArgType.String,
        shortName = "server",
        description = "Server URL"
    ).default("http://localhost:11111/egk")
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory containing output election record, optional for validating"
    )
    val nballots by parser.option(
        ArgType.Int,
        shortName = "nballots",
        description = "Number of test ballots to send to server"
    ).default(11)
    /*
    val sslKeyStore by parser.option(
        ArgType.String,
        shortName = "keystore",
        description = "file path of the keystore file"
    )
    val keystorePassword by parser.option(
        ArgType.String,
        shortName = "kpwd",
        description = "password for the entire keystore"
    )
    val electionguardPassword by parser.option(
        ArgType.String,
        shortName = "epwd",
        description = "password for the electionguard entry"
    )
     */
    parser.parse(args)

    val isSsl = false // (sslKeyStore != null) && (keystorePassword != null) && (electionguardPassword != null)

    println("RunEgkClient\n" +
            "  inputDir = '$inputDir'\n" +
            "  device = '$device'\n" +
            "  serverUrl = '$serverUrl'\n" +
            "  outputDir = '$outputDir'\n" +
            "  isSsl = '$isSsl'\n" +
            " ")

    val client = HttpClient(Java) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
            // level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json()
        }
        /*
        if (isSsl) {
            engine {
                config {
                    sslContext(SslSettings.getSslContext())
                }
            }
        }
         */
    }
    val proxy = RemoteEncryptorProxy(client, serverUrl)

    val group = productionGroup()
    val electionRecord = readElectionRecord(group, inputDir)

    // encrypt randomly generated ballots
    val ballotProvider = RandomBallotProvider(electionRecord.manifest())
    repeat(nballots) {
        val ballot = ballotProvider.makeBallot()
        val encryptResult = proxy.encryptBallot(device, ballot)
        if (encryptResult is Ok) {
            val ccode = encryptResult.unwrap()
            // randomly challenge a few
            val challengeIt = Random.nextInt(nballots) < 2
            if (challengeIt) {
                val decryptResult = proxy.challengeAndDecryptBallot(device, ccode)
                if (decryptResult is Ok) {
                    println("challenged $ccode, decryption Ok = ${ballot == decryptResult.unwrap()}")
                } else {
                    println("challengeAndDecrypt failed = ${decryptResult.getError()}")
                }
            } else {
                proxy.castBallot(device, ccode)
            }
        } else {
            println("encryptResult failed = ${encryptResult.getError()}")
        }
    }

    /* encrypt 3 randomly generated ballots
    val ballotProvider = RandomBallotProvider(electionRecord.manifest())
    repeat(3) {
        val ballot = ballotProvider.makeBallot()
        val resultEncrypt = proxy.encryptBallot(device, ballot)
        val ccode = resultEncrypt.unwrap()
        println(" encrypt ${ballot.ballotId} -> $ccode")
        val resultCast = proxy.castBallot(device, ccode)
        println(" cast ${ballot.ballotId} -> $resultCast")
    }

     */

    // write out the results
    proxy.sync(device)

    // verify
    if (outputDir != null) {
        verifyOutput(group, outputDir!!)
    }
}

fun verifyOutput(group: GroupContext, outputDir: String) {
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
    println("verifyEncryptedBallots: $verifierResult")

    if (record.config().chainConfirmationCodes) {
        val chainResult = verifier.verifyConfirmationChain(record)
        println("verifyChain: $chainResult")
    }
}