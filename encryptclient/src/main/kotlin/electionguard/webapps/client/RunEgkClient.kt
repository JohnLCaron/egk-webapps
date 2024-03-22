package electionguard.webapps.client

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.unwrap
import org.cryptobiotic.eg.election.PlaintextBallot
import org.cryptobiotic.eg.core.ElGamalPublicKey
import org.cryptobiotic.eg.core.GroupContext
import org.cryptobiotic.eg.core.productionGroup
import org.cryptobiotic.eg.input.RandomBallotProvider
import org.cryptobiotic.eg.publish.makeConsumer
import org.cryptobiotic.eg.publish.makePublisher
import org.cryptobiotic.eg.publish.readElectionRecord
import org.cryptobiotic.util.ErrorMessages
import org.cryptobiotic.eg.verifier.VerifyEncryptedBallots

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.math.max
import kotlin.random.Random

private var keystore = ""
private var ksPassword = ""
var egPassword = ""
var isSSL = false
val group = productionGroup()

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
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory containing output election record, optional for validating"
    ).required()
    val nballots by parser.option(
        ArgType.Int,
        shortName = "nballots",
        description = "Number of test ballots to send to server"
    ).default(11)
    val saveBallotsDir by parser.option(
        ArgType.String,
        shortName = "saveBallots",
        description = "save generated plaintext ballots in given directory"
    )
    val challengeSome by parser.option(
        ArgType.Boolean,
        shortName = "challengeSome",
        description = "randomly challenge a few ballots"
    ).default(false)

    val serverHost by parser.option(
        ArgType.String,
        shortName = "trusteeHost",
        description = "hostname of encryption server trustee webapp "
    ).default("localhost")
    val serverPort by parser.option(
        ArgType.Int,
        shortName = "serverPort",
        description = "port of encryption server webapp"
    ).default(11111)
    val sslKeyStore by parser.option(
        ArgType.String,
        shortName = "keystore",
        description = "file path of the keystore file"
    ).default("egKeystore.jks")
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
    parser.parse(args)

    isSSL = (keystorePassword != null) && (electionguardPassword != null)
    val serverUrl = if (isSSL) "https://$serverHost:$serverPort/egk" else "http://$serverHost:$serverPort/egk"
    if (isSSL) {
        keystore = sslKeyStore
        ksPassword = keystorePassword!!
        egPassword = electionguardPassword!!
    }

    println("RunEgkClient\n" +
            "  inputDir = '$inputDir'\n" +
            "  device = '$device'\n" +
            "  nballots = '$nballots'\n" +
            "  saveBallotsDir = '$saveBallotsDir'\n" +
            "  challengeSome = '$challengeSome'\n" +
            "  outputDir = '$outputDir'\n" +
            "  serverUrl = '$serverUrl'\n" +
            "  isSSL = '$isSSL'\n" +
            " ")

    val client = HttpClient(Java) {
        engine {
            protocolVersion = java.net.http.HttpClient.Version.HTTP_2
            if (isSSL) {
                config {
                    sslContext(SslSettings.getSslContext())
                }
            }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
            // level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json()
        }
    }
    val proxy = RemoteEncryptorProxy(group, client, serverUrl)
    try {
        if (!proxy.hello()) {
            println("Problem with server at $serverUrl - exit")
            return
        }
    } catch (t: Throwable) {
        println("Server at $serverUrl is not running - exit")
        return
    }

    val electionRecord = readElectionRecord(inputDir)

    // encrypt randomly generated ballots
    val inputBallots = mutableListOf<PlaintextBallot>()
    val ballotProvider = RandomBallotProvider(electionRecord.manifest())

    repeat(nballots) {
        val ballot = ballotProvider.makeBallot()
        if (challengeSome) {
            val encryptResult = proxy.encryptBallot(device, ballot)
            if (encryptResult is Ok) {
                val ccode = encryptResult.unwrap()
                // randomly challenge a few
                val challengeIt = Random.nextInt(nballots) < max(nballots/10, 2) // approx 10% or 2, whichever is bigger
                if (challengeIt) {
                    val decryptResult = proxy.challengeAndDecryptBallot(device, ccode)
                    if (decryptResult is Ok) {
                        println("$it challenged $ccode, decryption Ok = ${ballot == decryptResult.unwrap()}")
                    } else {
                        println("$it challengeAndDecrypt failed = ${decryptResult.getError()}")
                    }
                } else {
                    val result = proxy.castBallot(device, ccode)
                    println("$it castBallot $result")
                }
                inputBallots.add(ballot)
            } else {
                println("$it encryptResult failed = ${encryptResult.getError()}")
            }
        } else {
            val encryptAndCastResult = proxy.encryptAndCastBallot(device, ballot)
            if (encryptAndCastResult is Ok) {
                println("encryptAndCastResult random ballot cc = ${encryptAndCastResult.unwrap().confirmationCode}")
                inputBallots.add(ballot)
            }
        }
    }

    // write out the results
    proxy.sync(device)

    // optionally save the input ballots
   if (saveBallotsDir != null) {
        val publisher = makePublisher(saveBallotsDir!!, false)
        publisher.writePlaintextBallot(saveBallotsDir!!, inputBallots)
    }

    // verify
    verifyOutput(group, outputDir)
}

fun verifyOutput(group: GroupContext, outputDir: String) {
    val consumer = makeConsumer(outputDir, group)
    var count = 0
    consumer.iterateAllEncryptedBallots { true }.forEach {
        count++
    }
    println("$count EncryptedBallots")

    val record = readElectionRecord(consumer)
    val verifier = VerifyEncryptedBallots(
        group, record.manifest(),
        record.jointPublicKey()!!,
        record.extendedBaseHash()!!,
        record.config(), 1
    )

    // Note we are verifying all ballots, not just CAST
    val errs = ErrorMessages("EgkClient verify encrypted ballots")
    verifier.verifyBallots(record.encryptedAllBallots { true }, errs)
    println(errs)

    if (record.config().chainConfirmationCodes) {
        val chainErrs = ErrorMessages("EgkClient verify encrypted ballot chain")
        verifier.verifyConfirmationChain(record, chainErrs)
        println(errs)
    }
}

private object SslSettings {
    fun getKeyStore(): KeyStore {
        val keyStoreFile = FileInputStream(keystore)
        val keyStorePassword = ksPassword.toCharArray()
        val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(keyStoreFile, keyStorePassword)
        return keyStore
    }

    fun getTrustManagerFactory(): TrustManagerFactory? {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(getKeyStore())
        return trustManagerFactory
    }

    fun getSslContext(): SSLContext? {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, getTrustManagerFactory()?.trustManagers, null)
        return sslContext
    }

    fun getTrustManager(): X509TrustManager {
        return getTrustManagerFactory()?.trustManagers?.first { it is X509TrustManager } as X509TrustManager
    }
}