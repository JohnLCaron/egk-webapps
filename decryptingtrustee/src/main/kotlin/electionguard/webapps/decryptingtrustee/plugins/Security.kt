package electionguard.webapps.decryptingtrustee.plugins

import io.ktor.server.auth.*
import io.ktor.server.application.*

fun Application.configureSecurity(clientName : String, clientPassword : String) {

    authentication {
        // https://ktor.io/docs/basic.html
        basic("auth-basic") {
            realm = "Access to decryption services"
            validate { credentials ->
                if (credentials.name == clientName && credentials.password == clientPassword) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }
}
