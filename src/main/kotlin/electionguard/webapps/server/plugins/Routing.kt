package electionguard.webapps.server.plugins

import electionguard.webapps.server.routes.serverRouting
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        serverRouting()
    }
}
