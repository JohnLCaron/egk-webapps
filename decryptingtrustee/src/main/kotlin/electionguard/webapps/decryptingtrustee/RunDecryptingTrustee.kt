package electionguard.webapps.decryptingtrustee

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import electionguard.core.PowRadixOption
import electionguard.core.ProductionMode
import electionguard.core.productionGroup
import electionguard.webapps.decryptingtrustee.plugins.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore

val groupContext = productionGroup(PowRadixOption.HIGH_MEMORY_USE, ProductionMode.Mode4096)
var trusteeDir = ""
var credentialsPassword = ""

fun main(args: Array<String>) {
    val parser = ArgParser("RunDecryptingTrustee")
    val trustees by parser.option(
        ArgType.String,
        shortName = "trusteeDir",
        description = "trustee output directory"
    ).required()
    val serverPort by parser.option(
        ArgType.Int,
        shortName = "port",
        description = "listen on this port, default = 11190"
    ).default(11190)
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
    parser.parse(args)

    trusteeDir = trustees

    val isSsl = false // (sslKeyStore != null) && (keystorePassword != null) && (electionguardPassword != null)

    /*
    if (isSsl) {
        keystore = sslKeyStore
        ksPassword = keystorePassword
        egPassword = electionguardPassword
        credentialsPassword = electionguardPassword
   }
     */

    println("RunDecryptingTrustee\n" +
            "  isSsl = $isSsl\n" +
            "  serverPort = '$serverPort'\n" +
            "  trusteeDir = '$trusteeDir'\n" +
            " ")

    if (isSsl) {
        val keyStoreFile = File(sslKeyStore)
        val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType()) // LOOK assumes jks
        keyStore.load(FileInputStream(keyStoreFile), keystorePassword!!.toCharArray())

        val environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
            sslConnector(
                keyStore = keyStore,
                keyAlias = "electionguard",
                keyStorePassword = { keystorePassword!!.toCharArray() },
                privateKeyPassword = { electionguardPassword!!.toCharArray() }) {
                port = serverPort
                keyStorePath = keyStoreFile
            }
            module(Application::module)
        }

        println("RunDecryptingTrustee server ready...")
        embeddedServer(Netty, environment).start(wait = true)

    } else {
        println("RunDecryptingTrustee server (no SSL) ready...")
        embeddedServer(Netty, port = serverPort, host = "localhost", module = Application::module)
            .start(wait = true)
    }
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val path = call.request.path()
            "Status: $status, HTTP method: $httpMethod, Path: $path"
        }
    }
    configureSecurity(credentialsPassword)
    configureSerialization()
    configureAdministration()
    configureRouting()
}
