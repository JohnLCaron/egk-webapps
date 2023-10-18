package electionguard.webapps.keystore

import io.ktor.network.tls.certificates.*
import io.ktor.network.tls.extensions.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.io.*
import javax.security.auth.x500.X500Principal

// https://ktor.io/docs/ssl.html#self-signed
fun main(args: Array<String>) {
    val parser = ArgParser("MakeKeystore")
    val keystoreFile by parser.option(
        ArgType.String,
        shortName = "keystore",
        description = "file path of the keystore file"
    ).default("egKeystore.jks")
    val keystorePassword by parser.option(
        ArgType.String,
        shortName = "kpwd",
        description = "password for the entire keystore"
    ).required()
    val electionguardPassword by parser.option(
        ArgType.String,
        shortName = "epwd",
        description = "password for the electionguard certificate entry"
    ).required()
    val domainList by parser.option(
        ArgType.String,
        shortName = "domains",
        description = "list of domains (comma separated)"
    ).default("127.0.0.1, 0.0.0.0, localhost")
    val validDays by parser.option(
        ArgType.Int,
        shortName = "daysvalid",
        description = "number of days the certificate is valid"
    ).default(3)
    val x500Principal by parser.option(
        ArgType.String,
        shortName = "x500",
        description = "create X500 principle with this distinguished name"
    ).default("CN=voting.works, OU=electionguard.webapps, O=votingworks, C=US")

    parser.parse(args)
    val file = File(keystoreFile)

    var listOfDomains = domainList.split(",").map { it.trim() }

    val PRINCIPAL = X500Principal(x500Principal)

    val keyStore = buildKeyStore {
        certificate("electionguard") {
            daysValid = validDays.toLong()
            hash = HashAlgorithm.SHA256
            keySizeInBits = 3072
            password = electionguardPassword
            domains = listOfDomains
            subject = PRINCIPAL
        }
    }
    keyStore.saveToFile(file, keystorePassword)
    println(" write keystore to = '$file'")
}