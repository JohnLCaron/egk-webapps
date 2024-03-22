package electionguard.webapps.decryptingtrustee.routes

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.webapps.decryptingtrustee.groupContext
import org.cryptobiotic.eg.publish.json.*
import electionguard.webapps.decryptingtrustee.models.RemoteDecryptingTrusteeJson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.cryptobiotic.eg.decrypt.PartialDecryptions

private val logger = KotlinLogging.logger("DecryptingTrusteeRoutes")
private var trustees = mutableMapOf<String, RemoteDecryptingTrusteeJson>()

fun Route.trusteeRouting(trusteeDir: String, isJson : Boolean) {
    get {
        if (trustees.isNotEmpty()) {
            call.respondText(trustees.values.joinToString(",") { it.id() }, status = HttpStatusCode.OK)
        } else {
            call.respondText("No trustees found", status = HttpStatusCode.OK)
        }
    }

    post("reset") {
        if (trustees.isNotEmpty()) {
            trustees = mutableMapOf()
        }
        call.respondText("trustees reset", status = HttpStatusCode.OK)
    }

    get("load/{id}") {
        val id = call.parameters["id"]!!
        try {
            val trustee = RemoteDecryptingTrusteeJson(trusteeDir, isJson, id)
            trustees[id] = trustee
            println("RemoteDecryptingTrustee ${trustee.id()} created")
            call.respond(trustee.publicKey().key.publishJson()) // ElementModP
        } catch (t: Throwable) {
            logger.error(t) { " create RemoteDecryptingTrustee $id failed ${t.message}" }
            return@get call.respondText(
                "RemoteDecryptingTrustee '$id' doesnt exist",
                status = HttpStatusCode.BadRequest
            )
        }
    }

    post("{id}/decrypt") {
        val id = call.parameters["id"]!!
        val trustee = trustees[id]
        if (trustee == null) return@post call.respondText(
            "No trustee with id $id",
            status = HttpStatusCode.NotFound
        )
        val decryptRequestJson = call.receive<DecryptRequestJson>()
        val decryptRequestResult = decryptRequestJson.import(groupContext)
        if (decryptRequestResult is Ok) {
            val partialDecryptions: PartialDecryptions = trustee.decrypt(decryptRequestResult.unwrap())
            println("RemoteDecryptingTrustee ${trustee.id()} decrypt")
            call.respond(partialDecryptions.publishJson())
        } else {
            call.respondText("RemoteDecryptingTrustee $id decrypt failed", status = HttpStatusCode.InternalServerError)
        }
    }

    post("{id}/challenge") {
        val id = call.parameters["id"]!!
        val trustee = trustees[id]
        if (trustee == null) return@post call.respondText(
            "No trustee with id $id",
            status = HttpStatusCode.NotFound
        )
        val challengeRequestJson = call.receive<ChallengeRequestJson>()
        val challengeRequestResult = challengeRequestJson.import(groupContext)
        if (challengeRequestResult is Ok) {
            val challengeRequest = challengeRequestResult.unwrap()
            val challengeResponses = trustee.challenge(challengeRequest.batchId, challengeRequest.texts)
            println("RemoteDecryptingTrustee ${trustee.id()} challenge")
            call.respond(challengeResponses.publishJson())
        } else {
            call.respondText(
                "RemoteDecryptingTrustee $id challenge failed",
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}