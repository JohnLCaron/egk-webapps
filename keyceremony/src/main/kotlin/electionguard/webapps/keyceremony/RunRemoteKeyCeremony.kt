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

var keystore = ""
var ksPassword = ""
var egPassword = ""

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
    )
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output ElectionInitialized record"
    ).required()
    val remoteUrl by parser.option(
        ArgType.String,
        shortName = "remoteUrl",
        description = "URL of keyceremony trustee webapp "
    ).default("http://localhost:11183/egk")
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

    val createdBy by parser.option(
        ArgType.String,
        shortName = "createdBy",
        description = "who created for ElectionInitialized metadata"
    )
    parser.parse(args)

    val isSsl = false // (sslKeyStore != null) && (keystorePassword != null) && (electionguardPassword != null)

    /*
    if (isSsl) {
        keystore = sslKeyStore
        ksPassword = keystorePassword
        egPassword = electionguardPassword
    }
     */

    val group = productionGroup()
    var createdFrom : String

    val consumerIn = makeConsumer(inputDir!!, group)
    createdFrom = inputDir!!
    println(
        "RunRemoteKeyCeremony\n" +
                "  inputDir = '$inputDir'\n" +
                "  outputDir = '$outputDir'\n" +
                "  isSsl = $isSsl\n"
    )
    val config = consumerIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

    runKeyCeremony(group, remoteUrl, createdFrom, config, outputDir, consumerIn.isJson(), createdBy)
}

fun runKeyCeremony(
    group: GroupContext,
    remoteUrl: String,
    createdFrom: String,
    config: ElectionConfig,
    outputDir: String,
    isJson : Boolean,
    createdBy: String?,
): Boolean {
    val starting = getSystemTimeInMillis()

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

    val trustees: List<RemoteKeyTrusteeProxy> = List(config.numberOfGuardians) {
        val seq = it + 1
        RemoteKeyTrusteeProxy(group, client, remoteUrl, "trustee$seq", seq, config.quorum, egPassword)
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
            Pair("CreatedFrom", createdFrom),
        )
    )

    val publisher = makePublisher(outputDir, false, isJson)
    publisher.writeElectionInitialized(electionInitialized)
    println("writeElectionInitialized to $outputDir")

    // tell the trustees to save their state in some private place.
    trustees.forEach { it.saveState(isJson) }
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
