package electionguard.webapps.server.routes

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import org.cryptobiotic.eg.election.EncryptedBallot
import org.cryptobiotic.eg.election.PlaintextBallot
import org.cryptobiotic.eg.publish.json.*
import org.cryptobiotic.eg.publish.json.EncryptionResponseJson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import electionguard.webapps.server.models.EncryptionService

fun Route.serverRouting() {
    val encryptionService = EncryptionService.getInstance()

    get("hello") {
        println("HTTP version is ${call.request.httpVersion}")
        call.respondText("Hello!", status = HttpStatusCode.OK)
    }

    post("{device}/encryptBallot") {
        val device = call.parameters["device"] ?: return@post call.respondText(
            "Missing device",
            status = HttpStatusCode.BadRequest
        )

        val plaintextBallotJson = call.receive<PlaintextBallotJson>()
        val plaintextBallot = plaintextBallotJson.import()

        val result = encryptionService.encrypt(device, plaintextBallot)
        if (result is Ok) {
            val confirmationCodeString = result.unwrap().confirmationCode.toHex()
            val response = EncryptionResponseJson(confirmationCodeString)
            println("encryptBallot ${device} id=${plaintextBallot.ballotId} success cc=$confirmationCodeString")
            call.respond(response)
        } else {
            call.respondText(
                "EgkServer encrypt ballot id=${plaintextBallot.ballotId} failed ${result.unwrapError()}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    post("{device}/encryptAndCastBallot") {
        val device = call.parameters["device"] ?: return@post call.respondText(
            "Missing device",
            status = HttpStatusCode.BadRequest
        )

        val plaintextBallotJson = call.receive<PlaintextBallotJson>()
        val plaintextBallot = plaintextBallotJson.import()

        val result = encryptionService.encryptAndCast(device, plaintextBallot)
        if (result is Ok) {
            val encryptedBallot: EncryptedBallot  = result.unwrap()
            val response = encryptedBallot.publishJson()
            call.respond(response)
        } else {
            call.respondText(
                "EgkServer encrypt ballot id=${plaintextBallot.ballotId} failed ${result.unwrapError()}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    get("{device}/castBallot/{ccode}") {
        val device = call.parameters["device"] ?: return@get call.respondText(
            "Missing device",
            status = HttpStatusCode.BadRequest
        )
        val ccode = call.parameters["ccode"] ?: return@get call.respondText(
            "Missing id",
            status = HttpStatusCode.BadRequest
        )
        val result = encryptionService.submit(device, ccode, EncryptedBallot.BallotState.CAST)
        if (result is Ok) {
            call.respondText(
                "EgkServer cast ccode=${ccode} success",
                status = HttpStatusCode.OK
            )
        } else {
            call.respondText(
                "EgkServer cast ccode=${ccode} failed '${result.unwrapError()}'",
                status = HttpStatusCode.BadRequest
            )
        }
    }

    get("{device}/challengeBallot/{ccode}") {
        val device = call.parameters["device"] ?: return@get call.respondText(
            "Missing device",
            status = HttpStatusCode.BadRequest
        )
        val ccode = call.parameters["ccode"] ?: return@get call.respondText(
            "Missing id",
            status = HttpStatusCode.BadRequest
        )
        val result = encryptionService.submit(device, ccode, EncryptedBallot.BallotState.CHALLENGED)
        if (result is Ok) {
            call.respondText(
                "EgkServer challenge ccode=${ccode} success",
                status = HttpStatusCode.OK
            )
        } else {
            call.respondText(
                "EgkServer spoil ccode=${ccode} failed '${result.unwrapError()}'",
                status = HttpStatusCode.BadRequest
            )
        }
    }

    get("{device}/challengeAndDecryptBallot/{ccode}") {
        val device = call.parameters["device"] ?: return@get call.respondText(
            "Missing device",
            status = HttpStatusCode.BadRequest
        )
        val ccode = call.parameters["ccode"] ?: return@get call.respondText(
            "Missing id",
            status = HttpStatusCode.BadRequest
        )
        val result = encryptionService.challengeAndDecrypt(device, ccode)
        if (result is Ok) {
            val plaintextBallot: PlaintextBallot = result.unwrap()
            val response = plaintextBallot.publishJson()
            println("challengeAndDecrypt ${device} id=${plaintextBallot.ballotId} success")
            call.respond(response)
        } else {
            call.respondText(
                "EgkServer challengeAndDecryptBallot ccode=${ccode} failed '${result.unwrapError()}'",
                status = HttpStatusCode.BadRequest
            )
        }
    }

    get("{device}/sync") {
        val device = call.parameters["device"] ?: return@get call.respondText(
            "Missing device",
            status = HttpStatusCode.BadRequest
        )
        val result = encryptionService.sync(device)
        if (result is Ok) {
            call.respondText(
                "EgkServer sync device=${device} success",
                status = HttpStatusCode.OK
            )
        } else {
            call.respondText(
                "EgkServer sync device=${device} failed '${result.unwrapError()}'",
                status = HttpStatusCode.BadRequest
            )
        }
    }
}