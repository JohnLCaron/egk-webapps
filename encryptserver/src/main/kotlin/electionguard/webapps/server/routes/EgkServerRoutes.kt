package electionguard.webapps.server.routes

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import electionguard.ballot.EncryptedBallot
import electionguard.json2.*
import electionguard.json2.EncryptionResponseJson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import electionguard.webapps.server.models.EncryptionService

fun Route.serverRouting() {
    val encryptionService = EncryptionService.getInstance()

    route("/egk") {
        // see https://ktor.io/docs/basic.html
        // authenticate("auth-basic") {

            post("encryptBallot/{device}") {
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

            get("castBallot/{device}/{ccode}") {
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

            get("spoilBallot/{device}/{ccode}") {
                val device = call.parameters["device"] ?: return@get call.respondText(
                    "Missing device",
                    status = HttpStatusCode.BadRequest
                )
                val ccode = call.parameters["ccode"] ?: return@get call.respondText(
                    "Missing id",
                    status = HttpStatusCode.BadRequest
                )
                val result = encryptionService.submit(device, ccode, EncryptedBallot.BallotState.SPOILED)
                if (result is Ok) {
                    call.respondText(
                        "EgkServer spoil ccode=${ccode} success",
                        status = HttpStatusCode.OK
                    )
                } else {
                    call.respondText(
                        "EgkServer spoil ccode=${ccode} failed '${result.unwrapError()}'",
                        status = HttpStatusCode.BadRequest
                    )
                }
            }

            get("sync/{device}") {
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
    // }
}