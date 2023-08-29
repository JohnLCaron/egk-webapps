package electionguard.webapps.server

import electionguard.webapps.server.models.EncryptionService
import electionguard.webapps.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.security.KeyStore
import kotlinx.cli.*
import org.slf4j.LoggerFactory
import java.io.*

var credentialsPassword = ""

fun main(args: Array<String>) {
    val parser = ArgParser("ElectionGuardKotlinServer")
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
        description = "listen on this port, default = 11111"
    )
    parser.parse(args)

    val sport = serverPort ?: 11111
    credentialsPassword = electionguardPassword

    println("ElectionGuardKotlinServer\n" +
            "  sslKeyStore = '$sslKeyStore'\n" +
            "  serverPort = '$sport'\n" +
            "  inputDir = '$inputDir'\n" +
            " ")

    // println("trusteeDir = '$trusteeDir'")
    // io.ktor.server.netty.EngineMain.main(args)

    EncryptionService.initialize(inputDir, outputDir, true, true)

    val keyStoreFile = File(sslKeyStore)
    val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType()) // LOOK assumes jks
    keyStore.load(FileInputStream(keyStoreFile), keystorePassword.toCharArray())

    val environment = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        sslConnector(
            keyStore = keyStore,
            keyAlias = "electionguard",
            keyStorePassword = { keystorePassword.toCharArray() },
            privateKeyPassword = { electionguardPassword.toCharArray() }) {
            port = sport
            keyStorePath = keyStoreFile
        }
        module(Application::module)
    }

    embeddedServer(Netty, environment).start(wait = true)
}

/*
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

 */

fun Application.module() {
    configureSecurity()
    configureMonitoring()
    configureSerialization()
    configureAdministration()
    configureRouting()
}