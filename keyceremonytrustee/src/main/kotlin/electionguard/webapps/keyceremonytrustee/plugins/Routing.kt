package electionguard.webapps.keyceremonytrustee.plugins

import electionguard.webapps.keyceremonytrustee.routes.trusteeRouting
import io.ktor.server.routing.*
import io.ktor.server.application.*

fun Application.configureRouting() {

    routing {
        trusteeRouting()
    }
}
