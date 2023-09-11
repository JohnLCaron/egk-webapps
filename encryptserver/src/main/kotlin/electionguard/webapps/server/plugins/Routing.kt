package electionguard.webapps.server.plugins

import electionguard.webapps.server.isSSL
import electionguard.webapps.server.routes.serverRouting
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*


fun Application.configureRouting() {

    routing {
        if (isSSL) {
            route("/egk") {
                // https://ktor.io/docs/basic.html
                authenticate("auth-basic") {
                    serverRouting()
                }
            }
        } else {
            route("/egk") {
                serverRouting()
            }
        }
    }

}