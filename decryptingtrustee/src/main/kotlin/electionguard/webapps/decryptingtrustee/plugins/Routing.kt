package electionguard.webapps.decryptingtrustee.plugins

import electionguard.webapps.decryptingtrustee.routes.trusteeRouting
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureRouting(isSSL : Boolean, trusteeDir : String, isJson : Boolean) {

    routing {
        if (isSSL) {
            route("/egk/dtrustee") {
                authenticate("auth-basic") {
                    trusteeRouting(trusteeDir, isJson)
                }
            }
        } else {
            route("/egk/dtrustee") {
                trusteeRouting(trusteeDir, isJson)
            }
        }
    }

}