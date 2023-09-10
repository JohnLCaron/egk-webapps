package electionguard.webapps.decryption

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.partition
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.getSystemDate
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.DecryptorDoerre
import electionguard.decrypt.Guardians
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.cli.default
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

var keystore = ""
var ksPassword = ""
var egPassword = ""

private val logger = KotlinLogging.logger("RunRemoteDecryption")


/**
 * Run Remote Decryption CLI.
 * The RunDecryptingTrustee webapp must already be running.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunRemoteDecryption")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input election record"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output election record"
    ).required()
    val remoteUrl by parser.option(
        ArgType.String,
        shortName = "remoteUrl",
        description = "URL of decrypting trustee app "
    ).default("http://localhost:11190/egk")
    val createdBy by parser.option(
        ArgType.String,
        shortName = "createdBy",
        description = "who created"
    )
    val missing by parser.option(
        ArgType.String,
        shortName = "missing",
        description = "missing guardians' xcoord, comma separated, eg '2,4'"
    )
    /*
    val sslKeyStore by parser.option(
        ArgType.String,
        shortName = "keystore",
        description = "file path of the keystore file"
    ).required()
    val keystorePassword by parser.option(
        ArgType.String,
        shortName = "kpwd",
        description = "password for the entire keystore"
    ).required()
    val electionguardPassword by parser.option(
        ArgType.String,
        shortName = "epwd",
        description = "password for the electionguard entry"
    ).required()
     */
    parser.parse(args)
    println("RunRemoteDecryption starting\n   input= $inputDir\n   missing= '$missing'\n   output = $outputDir")

    val isSsl = false // (sslKeyStore != null) && (keystorePassword != null) && (electionguardPassword != null)

    /*
    if (isSsl) {
        keystore = sslKeyStore
        ksPassword = keystorePassword
        egPassword = electionguardPassword
    }
     */

    val group = productionGroup()
    val success = runRemoteDecrypt(
        group,
        inputDir,
        outputDir,
        remoteUrl,
        missing,
        createdBy)
    println("success = $success")
}

fun runRemoteDecrypt(
    group: GroupContext,
    inputDir: String,
    outputDir: String,
    remoteUrl: String,
    missing: String?,
    createdBy: String?
): Boolean {
    val starting = getSystemTimeInMillis()

    val consumerIn = makeConsumer(inputDir, group)
    val tallyResult: TallyResult = consumerIn.readTallyResult().getOrThrow { IllegalStateException(it) }
    val electionInitialized = tallyResult.electionInitialized

    // get the list of missing and present guardians
    val allGuardians = electionInitialized.guardians
    val missingGuardianIds =  if (missing.isNullOrEmpty()) emptyList() else {
        // remove missing guardians
        val missingX = missing.split(",").map { it.toInt() }
        allGuardians.filter { missingX.contains(it.xCoordinate) }.map { it.guardianId }
    }
    val presentGuardians =  allGuardians.filter { !missingGuardianIds.contains(it.guardianId) }
    val presentGuardianIds =  presentGuardians.map { it.guardianId }
    if (presentGuardianIds.size < electionInitialized.config.quorum) {
        logger.atError().log("number of guardians present ${presentGuardianIds.size} < quorum ${electionInitialized.config.quorum}")
        throw IllegalStateException("number of guardians present ${presentGuardianIds.size} < quorum ${electionInitialized.config.quorum}")
    }
    println("runRemoteDecrypt present = $presentGuardianIds missing = $missingGuardianIds")

    val client = HttpClient(Java) {
        install(Logging)
        install(ContentNegotiation) {
            json()
        }
    }
    reset(client, remoteUrl)

    // public fun <V, E> Iterable<Result<V, E>>.partition(): Pair<List<V>, List<E>> {
    val trusteeResults : List<Result<DecryptingTrusteeIF, String>> = presentGuardians.map {
        DecryptingTrusteeProxy.create(group, client, remoteUrl, it.guardianId, it.xCoordinate, it.publicKey())
    }
    val (trustees, errors) = trusteeResults.partition()
    if (errors.isNotEmpty()) {
        println("FAIL runRemoteDecrypt creating trustees: ${errors.joinToString("\n ")}")
        return false
    }

    val guardians = Guardians(group, tallyResult.electionInitialized.guardians)
    val decryptor = DecryptorDoerre(group,
        tallyResult.electionInitialized.extendedBaseHash,
        tallyResult.electionInitialized.jointPublicKey(),
        guardians,
        trustees,
    )
    val decryptedTally = with(decryptor) { tallyResult.encryptedTally.decrypt() }

    val publisher = makePublisher(outputDir, createNew = true, jsonSerialization = true) // LOOK
    publisher.writeDecryptionResult(
        DecryptionResult(
            tallyResult,
            decryptedTally,
            mapOf(
                Pair("CreatedBy", createdBy ?: "RunTrustedDecryption"),
                Pair("CreatedOn", getSystemDate()),
                Pair("CreatedFromDir", inputDir))
        )
    )

    val took = getSystemTimeInMillis() - starting
    println("runRemoteDecrypt took $took millisecs")
    return true
}

fun reset(client : HttpClient, remoteUrl : String) {
    runBlocking {
        val url = "$remoteUrl/dtrustee/reset"
        val response: HttpResponse = client.post(url)
        println("runRemoteDecrypt reset ${response.status}")
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
