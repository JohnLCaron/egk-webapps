package electionguard.webapps.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.keyceremony.keyCeremonyExchange
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
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private var keystore = ""
private var ksPassword = ""
private var egPassword = ""
var isSSL = false

/**
 * Run Remote KeyCeremony CLI.
 * The keyceremonytrustee webapp must already be running.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunRemoteKeyCeremony")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input ElectionConfig record"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output ElectionInitialized record"
    ).required()
    val serverHost by parser.option(
        ArgType.String,
        shortName = "trusteeHost",
        description = "hostname of keyceremony trustee webapp "
    ).default("localhost")
    val serverPort by parser.option(
        ArgType.Int,
        shortName = "serverPort",
        description = "port of keyceremony trustee webapp "
    ).default(11183)
    val createdBy by parser.option(
        ArgType.String,
        shortName = "createdBy",
        description = "who created (for ElectionInitialized metadata)"
    )
    val sslKeyStore by parser.option(
        ArgType.String,
        shortName = "keystore",
        description = "file path of the keystore file"
    ).default("egKeystore.jks")
    val keystorePassword by parser.option(
        ArgType.String,
        shortName = "kpwd",
        description = "password for the keystore file"
    )
    val electionguardPassword by parser.option(
        ArgType.String,
        shortName = "epwd",
        description = "password for the electionguard entry"
    )
    parser.parse(args)

    isSSL = (keystorePassword != null) && (electionguardPassword != null)
    if (isSSL) {
        keystore = sslKeyStore
        ksPassword = keystorePassword!!
        egPassword = electionguardPassword!!
    }
    val remoteUrl = if (isSSL) "https://$serverHost:$serverPort/egk" else "http://$serverHost:$serverPort/egk"

    val group = productionGroup()
    val consumerIn = makeConsumer(group, inputDir)
    println("RunRemoteKeyCeremony\n" +
            "  inputDir = '$inputDir'\n" +
            "  outputDir = '$outputDir'\n" +
            "  remoteUrl = $remoteUrl\n" +
            "  isSsl = $isSSL"
    )
    if (isSSL) {
        println("  keystore = '$keystore'")
    }

    val config = consumerIn.readElectionConfig().getOrThrow { IllegalStateException(it) }
    runKeyCeremony(group, remoteUrl, inputDir, config, outputDir, consumerIn.isJson(), createdBy)
}

fun runKeyCeremony(
    group: GroupContext,
    remoteUrl: String,
    inputDir: String,
    config: ElectionConfig,
    outputDir: String,
    isJson : Boolean,
    createdBy: String?,
): Boolean {
    val starting = getSystemTimeInMillis()

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

    val trustees: List<RemoteKeyTrusteeProxy> = List(config.numberOfGuardians) {
        val seq = it + 1
        RemoteKeyTrusteeProxy(group, client, remoteURL = remoteUrl, "trustee$seq", seq, config.numberOfGuardians, config.quorum, egPassword)
    }

    val exchangeResult = keyCeremonyExchange(trustees)
    if (exchangeResult is Err) {
        println(exchangeResult.error)
        return false
    }

    val keyCeremonyResults = exchangeResult.unwrap()
    val electionInitialized = keyCeremonyResults.makeElectionInitialized(
        config,
        mapOf(
            Pair("CreatedBy", createdBy ?: "RunRemoteKeyCeremony"),
            Pair("CreatedFrom", inputDir),
        )
    )
    val publisher = makePublisher(outputDir, false, isJson)
    publisher.writeElectionInitialized(electionInitialized)
    println("writeElectionInitialized to $outputDir")

    // tell the trustees to save their state in some private place.
    trustees.forEach { it.saveState(electionInitialized.extendedBaseHash) }
    client.close()

    val took = getSystemTimeInMillis() - starting
    println("RunTrustedKeyCeremony took $took millisecs")
    return true
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
