package electionguard.webapps.decryptingtrustee.plugins

import electionguard.webapps.decryptingtrustee.isSSL
import electionguard.webapps.decryptingtrustee.routes.trusteeRouting
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureRouting() {

    routing {
        if (isSSL) {
            route("/egk/dtrustee") {
                // https://ktor.io/docs/basic.html
                authenticate("auth-basic") {
                    trusteeRouting()
                }
            }
        } else {
            route("/egk/dtrustee") {
                trusteeRouting()
            }
        }
    }

}