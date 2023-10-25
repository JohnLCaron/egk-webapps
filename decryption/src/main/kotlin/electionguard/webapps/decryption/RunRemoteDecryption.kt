package electionguard.webapps.decryption

import com.github.michaelbull.result.*
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
import io.ktor.serialization.kotlinx.json.*
import kotlinx.cli.default
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

val group = productionGroup()
private val logger = KotlinLogging.logger("RunRemoteDecryption")

/**
 * Run Remote Decryption CLI.
 * The DecryptingTrustee webapp(s) must already be running.
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
    val serverHost by parser.option(
        ArgType.String,
        shortName = "trusteeHost",
        description = "hostname of decrypting trustee webapp "
    ).default("localhost")
    val serverPort by parser.option(
        ArgType.Int,
        shortName = "serverPort",
        description = "port of decrypting trustee webapp "
    ).default(11190)
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

    val clientKeyStore by parser.option(
        ArgType.String,
        shortName = "keystore",
        description = "file path of the client keystore file"
    ).default("clientkeystore.p12")
    val clientKeystorePassword by parser.option(
        ArgType.String,
        shortName = "ckpwd",
        description = "password for the client keystore"
    )

    // the decrypting client name and password. Theres only one client allowed. The password must remain secret.
    val clientName by parser.option(
        ArgType.String,
        shortName = "client",
        description = "client user name"
    ).default("electionguard")
    val clientPassword by parser.option(
        ArgType.String,
        shortName = "cpwd",
        description = "client user password"
    )

    parser.parse(args)

    val isSSL = (clientKeystorePassword != null) && (clientPassword != null)
    val remoteUrl = if (isSSL) "https://$serverHost:$serverPort/egk" else "http://$serverHost:$serverPort/egk"

    println(
        "RunRemoteDecryption starting\n   input= $inputDir\n   missing= '$missing'\n   output= $outputDir\n" +
                "   isSSL= $isSSL\n   remoteUrl= $remoteUrl"
    )

    val success = runRemoteDecrypt(
        group,
        inputDir,
        outputDir,
        remoteUrl,
        missing,
        createdBy,
        isSSL,
        clientKeyStore,
        clientKeystorePassword,
        clientName,
        clientPassword,
    )
    println("success = $success")
}

fun runRemoteDecrypt(
    group: GroupContext,
    inputDir: String,
    outputDir: String,
    remoteUrl: String,
    missing: String?,
    createdBy: String?,
    isSSL: Boolean,
    clientKeyStore: String,
    clientKeystorePassword: String?,
    clientName: String,
    clientPassword: String?,
): Boolean {
    val starting = getSystemTimeInMillis()

    // The Java engine uses the Java HTTP Client introduced in Java 11.
    val client = HttpClient(Java) {
        engine {
            protocolVersion = java.net.http.HttpClient.Version.HTTP_2
            if (isSSL) {
                val sslSettings = SslSettings(clientKeyStore, clientKeystorePassword!!)
                config {
                    // For the Java client, pass SSLContext to the sslContext function inside the config block:
                    sslContext(sslSettings.getSslContext())
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
    // reset(client, remoteUrl) // TODO ??

    val consumerIn = makeConsumer(group, inputDir)
    val tallyResult: TallyResult = consumerIn.readTallyResult().getOrThrow { IllegalStateException(it) }
    val electionInitialized = tallyResult.electionInitialized

    // get the list of missing and present guardians
    val allGuardians = electionInitialized.guardians
    val missingGuardianIds = if (missing.isNullOrEmpty()) emptyList() else {
        // remove missing guardians
        val missingX = missing!!.split(",").map { it.toInt() }
        allGuardians.filter { missingX.contains(it.xCoordinate) }.map { it.guardianId }
    }
    val presentGuardians = allGuardians.filter { !missingGuardianIds.contains(it.guardianId) }
    val presentGuardianIds = presentGuardians.map { it.guardianId }
    if (presentGuardianIds.size < electionInitialized.config.quorum) {
        logger.error { "number of guardians present ${presentGuardianIds.size} < quorum ${electionInitialized.config.quorum}" }
        throw IllegalStateException("number of guardians present ${presentGuardianIds.size} < quorum ${electionInitialized.config.quorum}")
    }
    println("runRemoteDecrypt present = $presentGuardianIds missing = $missingGuardianIds")

    // public fun <V, E> Iterable<Result<V, E>>.partition(): Pair<List<V>, List<E>> {
    val trusteeResults: List<Result<DecryptingTrusteeIF, String>> = presentGuardians.map {
        val proxy = DecryptingTrusteeProxy(
            group, client, remoteUrl, it.guardianId, it.xCoordinate, it.publicKey(),
            isSSL, clientName, clientPassword
        )
        if (proxy.initError == null) Ok(proxy) else Err(proxy.initError!!)
    }
    val (trustees, errors) = trusteeResults.partition()
    if (errors.isNotEmpty()) {
        println("FAIL runRemoteDecrypt creating trustees: ${errors.joinToString("\n ")}")
        return false
    }

    val guardians = Guardians(group, tallyResult.electionInitialized.guardians)
    val decryptor = DecryptorDoerre(
        group,
        tallyResult.electionInitialized.extendedBaseHash,
        tallyResult.electionInitialized.jointPublicKey(),
        guardians,
        trustees,
    )
    val decryptedTally = with(decryptor) { tallyResult.encryptedTally.decrypt() }

    val publisher = makePublisher(outputDir, createNew = false, jsonSerialization = true) // LOOK
    publisher.writeDecryptionResult(
        DecryptionResult(
            tallyResult,
            decryptedTally,
            mapOf(
                Pair("CreatedBy", createdBy ?: "RunTrustedDecryption"),
                Pair("CreatedOn", getSystemDate()),
                Pair("CreatedFromDir", inputDir)
            )
        )
    )

    val took = getSystemTimeInMillis() - starting
    println("runRemoteDecrypt took $took millisecs")
    return true
}

/*
fun reset(client : HttpClient, remoteUrl : String) {
    runBlocking {
        val url = "$remoteUrl/dtrustee/reset"
        val response: HttpResponse = client.post(url)
        println("runRemoteDecrypt reset ${response.status}")
    }
}

 */

// from docs: "the Ktor client will be using a certificate loaded from the existing KeyStore file (keystore.jks)
// generated for the server." This enables the client to authenticate the server.
private class SslSettings(val keystore: String, val ksPassword: String) {
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
