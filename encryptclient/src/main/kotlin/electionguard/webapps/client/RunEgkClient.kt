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
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
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
                    println("$it challenged $ccode, decryption Ok = ${ballot == decryptResult.unwrap()}")
                } else {
                    println("$it challengeAndDecrypt failed = ${decryptResult.getError()}")
                }
            } else {
                val result = proxy.castBallot(device, ccode)
                println("$it cast $result")
            }
        } else {
            println("$it encryptResult failed = ${encryptResult.getError()}")
        }
    }

    val ballot = ballotProvider.makeBallot()
    val encryptAndCastResult = proxy.encryptAndCastBallot(device, ballot)
    if (encryptAndCastResult is Ok) {
        println("encryptAndCastResult random ballot cc = ${encryptAndCastResult.unwrap().confirmationCode}")
    }

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