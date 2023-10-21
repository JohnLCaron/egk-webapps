package electionguard.webapps.decryptingtrustee

import io.ktor.server.application.*
import electionguard.core.PowRadixOption
import electionguard.core.ProductionMode
import electionguard.core.productionGroup
import electionguard.webapps.decryptingtrustee.plugins.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore

val groupContext = productionGroup(PowRadixOption.HIGH_MEMORY_USE, ProductionMode.Mode4096)

// Note this doesnt need config parameters. But trustees must be correct ones for this election record.
fun main(args: Array<String>) {
    val parser = ArgParser("RunDecryptingTrustee")
    val trustees by parser.option(
        ArgType.String,
        shortName = "trusteeDir",
        description = "trustee output directory"
    ).required()
    val trusteeIsJson by parser.option(
        ArgType.Boolean,
        shortName = "isJson",
        description = "trustees are in JSON format"
    ).default(true)

    val serverPort by parser.option(
        ArgType.Int,
        shortName = "port",
        description = "listen on this port, default = 11190"
    ).default(11190)

    // heres where the SSL certificate is stored, to authenticate to the client
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
        description = "password for the electionguard entry in the keystore"
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
    ) // default it to electionguardPassword (?)

    parser.parse(args)

    val isSSL = (keystorePassword != null) && (electionguardPassword != null)

    println("RunDecryptingTrustee\n" +
            "  isSSL = $isSSL\n" +
            "  serverPort = '$serverPort'\n" +
            "  trustees = '$trustees'\n" +
            " ")

    if (isSSL) {
        val keyStoreFile = File(sslKeyStore)
        val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType()) // LOOK assumes jks
        keyStore.load(FileInputStream(keyStoreFile), keystorePassword!!.toCharArray())

        val environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
            sslConnector(
                keyStore = keyStore,
                keyAlias = "electionguard",
                keyStorePassword = { keystorePassword!!.toCharArray() },
                // LOOK this is the electionguard entry password in the keystore. So client may be different, i think
                privateKeyPassword = { electionguardPassword!!.toCharArray() }) {
                port = serverPort
                keyStorePath = keyStoreFile
            }
        }
        environment.application.module(trustees, trusteeIsJson, isSSL = true, clientName,
            clientPassword = clientPassword?: electionguardPassword!!)

        println("RunDecryptingTrustee server start...")
        embeddedServer(Netty, environment).start(wait = true)

    } else {
        println("RunDecryptingTrustee server (no SSL) start...")
        val environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
            connectors.add(EngineConnectorBuilder().withPort(serverPort))
        }
        environment.application.module(trustees, trusteeIsJson)

        println("RunDecryptingTrustee server start...")
        embeddedServer(Netty, environment).start(wait = true)
        // embeddedServer(Netty, port = serverPort, host = "localhost", module = Application::module).start(wait = true)
    }
}

fun Application.module(trusteeDir : String, isJson : Boolean,
                       isSSL : Boolean = false, clientName : String = "", clientPassword : String = "" ) {
    if (isSSL) configureSecurity(clientName, clientPassword)
    configureMonitoring()
    configureSerialization()
    configureAdministration()
    configureRouting(isSSL, trusteeDir, isJson)
}
