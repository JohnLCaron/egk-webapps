package electionguard.webapps.keyceremonytrustee.plugins

import electionguard.webapps.keyceremonytrustee.isSSL
import electionguard.webapps.keyceremonytrustee.routes.trusteeRouting
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureRouting() {

    routing {
        if (isSSL) {
            route("/egk/ktrustee") {
                // https://ktor.io/docs/basic.html
                authenticate("auth-basic") {
                    trusteeRouting()
                }
            }
        } else {
            route("/egk/ktrustee") {
                trusteeRouting()
            }
        }
    }

}
