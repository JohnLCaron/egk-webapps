package electionguard.webapps.decryptingtrustee.plugins

import electionguard.webapps.decryptingtrustee.routes.trusteeRouting
import io.ktor.server.routing.*
import io.ktor.server.application.*

fun Application.configureRouting() {
    routing {
        trusteeRouting()
    }
}
