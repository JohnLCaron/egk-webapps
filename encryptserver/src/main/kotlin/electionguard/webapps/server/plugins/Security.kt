package electionguard.webapps.server.plugins

import electionguard.webapps.server.egPassword
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureSecurity() {
    authentication {
        // https://ktor.io/docs/basic.html
        basic("auth-basic") {
            realm = "Access to the '/' path"
            validate { credentials ->
                if (credentials.name == "electionguard" && credentials.password == egPassword) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }
}
