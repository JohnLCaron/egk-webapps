package electionguard.webapps.server

import electionguard.core.PowRadixOption
import electionguard.core.ProductionMode
import electionguard.core.productionGroup
import electionguard.webapps.server.models.EncryptionService
import electionguard.webapps.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.security.KeyStore
import kotlinx.cli.*
import org.slf4j.LoggerFactory
import java.io.*

private var ksPassword = ""
var egPassword = ""
var isSSL = false
val groupContext = productionGroup(PowRadixOption.HIGH_MEMORY_USE, ProductionMode.Mode4096)

fun main(args: Array<String>) {
    val parser = ArgParser("RunEgkServerKt")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input election record"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory containing output election record"
    ).required()
    val serverPort by parser.option(
        ArgType.Int,
        shortName = "port",
        description = "listen on this port"
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

    println("ElectionGuardKotlinServer\n" +
            "  inputDir = '$inputDir'\n" +
            "  outputDir = '$outputDir'\n" +
            "  isSsl = '$isSSL'\n" +
            "  serverPort = '$serverPort'\n" +
            " ")

    EncryptionService.initialize(inputDir, outputDir)

    if (isSSL) {
        egPassword = electionguardPassword!!
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
        embeddedServer(Netty, environment).start(wait = true)

    } else {
        // TODO host = "localhost" ??
        embeddedServer(Netty, port = 11111, host = "localhost", module = Application::module)
            .start(wait = true)
    }
}

fun Application.module() {
    if (isSSL) configureSecurity()
    configureMonitoring()
    configureSerialization()
    configureAdministration()
    configureRouting()
}