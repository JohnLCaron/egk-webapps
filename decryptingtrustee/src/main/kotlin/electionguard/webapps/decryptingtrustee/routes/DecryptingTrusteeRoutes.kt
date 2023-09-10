package electionguard.webapps.decryptingtrustee.routes

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.webapps.decryptingtrustee.groupContext
import electionguard.json2.*
import electionguard.webapps.decryptingtrustee.models.RemoteDecryptingTrusteeJson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger("DecryptingTrusteeRoutes")
private var trustees = mutableListOf<RemoteDecryptingTrusteeJson>()

fun Route.trusteeRouting() {
    route("/egk/dtrustee") {
        get {
            if (trustees.isNotEmpty()) {
                call.respondText(trustees.joinToString(",") { it.id() }, status = HttpStatusCode.OK)
            } else {
                call.respondText("No trustees found", status = HttpStatusCode.OK)
            }
        }

        post("reset") {
            if (trustees.isNotEmpty()) {
                trustees = mutableListOf()
            }
            call.respondText("trustees reset", status = HttpStatusCode.OK)
        }

        get("create/{id?}") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            try {
                val trustee = RemoteDecryptingTrusteeJson(id)
                trustees.add(trustee)
                println("RemoteDecryptingTrustee ${trustee.id()} created")
                val result = trustee.publicKey()
                call.respond(result.publishJson()) // ElementModP
            } catch (t: Throwable) {
                logger.atError().setCause(t).log(" create RemoteDecryptingTrustee $id failed ${t.message}")
                return@get call.respondText(
                    "RemoteDecryptingTrustee '$id' doesnt exist",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        post("{id?}/decrypt") {
            val id = call.parameters["id"] ?: return@post call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val trustee =
                trustees.find { it.xCoordinate() == id.toInt() } ?: return@post call.respondText(
                    "No trustee with id $id",
                    status = HttpStatusCode.NotFound
                )
            val decryptRequestJson = call.receive<DecryptRequestJson>()
            val decryptRequest = decryptRequestJson.import(groupContext)
            if (decryptRequest is Ok) {
                val decryptions = trustee.decrypt(decryptRequest.unwrap().texts)
                val response = DecryptResponse(decryptions)
                println("RemoteDecryptingTrustee ${trustee.id()} decrypt")
                call.respond(response.publishJson())
            } else {
                call.respondText("RemoteDecryptingTrustee $id decrypt failed", status = HttpStatusCode.InternalServerError)
            }
        }

        post("{id?}/challenge") {
            val id = call.parameters["id"] ?: return@post call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val trustee =
                trustees.find { it.xCoordinate() == id.toInt() } ?: return@post call.respondText(
                    "No trustee with id $id",
                    status = HttpStatusCode.NotFound
                )
            val requestsJson = call.receive<ChallengeRequestsJson>()
            val requests = requestsJson.import(groupContext)
            if (requests is Ok) {
                val responses = trustee.challenge(requests.unwrap().challenges)
                val response = ChallengeResponses(responses)
                println("RemoteDecryptingTrustee ${trustee.id()} challenge")
                call.respond(response.publishJson())
            } else {
                call.respondText("RemoteDecryptingTrustee $id challenge failed", status = HttpStatusCode.InternalServerError)
            }
        }

    }

}