package electionguard.webapps.keyceremonytrustee.routes

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import electionguard.core.ElementModQ
import electionguard.json2.*
import electionguard.keyceremony.EncryptedKeyShare
import electionguard.webapps.keyceremonytrustee.groupContext
import electionguard.webapps.keyceremonytrustee.models.RemoteKeyTrustee
import electionguard.webapps.keyceremonytrustee.models.remoteKeyTrustees
import electionguard.webapps.keyceremonytrustee.trusteeDir
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.trusteeRouting() {
    get {
        if (remoteKeyTrustees.isNotEmpty()) {
            call.respond(remoteKeyTrustees)
        } else {
            call.respondText("No guardians found", status = HttpStatusCode.OK)
        }
    }

    post {
        val rguardian = call.receive<RemoteKeyTrustee>()
        remoteKeyTrustees.add(rguardian)
        call.respondText("RemoteKeyTrustee ${rguardian.id} created", status = HttpStatusCode.Created)
    }

    get("{id}/publicKeys") {
        val id = call.parameters["id"]
        val rguardian =
            remoteKeyTrustees.find { it.id == id } ?: return@get call.respondText(
                "No RemoteKeyTrustee with id= $id",
                status = HttpStatusCode.NotFound
            )
        val result = rguardian.publicKeys()
        if (result is Ok) {
            val pk = result.unwrap()
            call.respond(pk.publishJson())
        } else {
            call.respondText(
                "RemoteKeyTrustee ${rguardian.id} publicKeys failed ${result.unwrapError()}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    post("{id}/receivePublicKeys") {
        val id = call.parameters["id"]
        val rguardian =
            remoteKeyTrustees.find { it.id == id } ?: return@post call.respondText(
                "No RemoteKeyTrustee with id= $id",
                status = HttpStatusCode.NotFound
            )
        val publicKeysJson = call.receive<PublicKeysJson>()
        val publicKeysResult = publicKeysJson.importResult(groupContext)
        if (publicKeysResult is Ok) {
            val publicKeys = publicKeysResult.unwrap()
            val result = rguardian.receivePublicKeys(publicKeys)
            if (result is Ok) {
                call.respondText(
                    "RemoteKeyTrustee ${rguardian.id} receivePublicKeys from ${publicKeys.guardianId} correctly",
                    status = HttpStatusCode.OK
                )
            } else {
                call.respondText(
                    "RemoteKeyTrustee ${rguardian.id} receivePublicKeys from ${publicKeys.guardianId} failed ${result.unwrapError()}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        } else {
            call.respondText(
                "RemoteKeyTrustee ${rguardian.id} receivePublicKeys importPublicKeys failed ${publicKeysResult.unwrapError()}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    get("{id}/encryptedKeyShareFor/{forTrustee}") {
        val id = call.parameters["id"]
        val rguardian =
            remoteKeyTrustees.find { it.id == id } ?: return@get call.respondText(
                "No RemoteKeyTrustee with id= $id",
                status = HttpStatusCode.NotFound
            )
        val forTrustee = call.parameters["forTrustee"] ?: return@get call.respondText(
            "Missing 'forTrustee' id",
            status = HttpStatusCode.BadRequest
        )
        val result: Result<EncryptedKeyShare, String> = rguardian.encryptedKeyShareFor(forTrustee)
        if (result is Ok) {
            call.respond(result.unwrap().publishJson())
        } else {
            call.respondText(
                "RemoteKeyTrustee ${rguardian.id} encryptedKeyShareFor forTrustee ${forTrustee} failed ${result.unwrapError()}",
                status = HttpStatusCode.BadRequest
            )
        }
    }

    post("{id}/receiveEncryptedKeyShare") {
        val id = call.parameters["id"]
        val rguardian =
            remoteKeyTrustees.find { it.id == id } ?: return@post call.respondText(
                "No RemoteKeyTrustee with id= $id",
                status = HttpStatusCode.NotFound
            )
        val secretShareJson = call.receive<EncryptedKeyShareJson>()
        val secretShare = secretShareJson.import(groupContext)
        val result = rguardian.receiveEncryptedKeyShare(secretShareJson.import(groupContext))
        if (result is Ok) {
            call.respondText(
                "RemoteKeyTrustee ${rguardian.id} receiveEncryptedKeyShare from polynomial owner ${secretShare!!.polynomialOwner} correctly",
                status = HttpStatusCode.OK
            )
        } else {
            call.respondText(
                "RemoteKeyTrustee ${rguardian.id} receiveEncryptedKeyShare failed ${result.unwrapError()}",
                status = HttpStatusCode.BadRequest
            )
        }
    }

    get("{id}/keyShareFor/{forTrustee}") {
        val id = call.parameters["id"]
        val rguardian =
            remoteKeyTrustees.find { it.id == id } ?: return@get call.respondText(
                "No RemoteKeyTrustee with id= $id",
                status = HttpStatusCode.NotFound
            )
        val forTrustee = call.parameters["forTrustee"] ?: return@get call.respondText(
            "Missing 'forTrustee' id",
            status = HttpStatusCode.BadRequest
        )
        val result = rguardian.keyShareFor(forTrustee)
        if (result is Ok) {
            call.respond(result.unwrap().publishJson())
        } else {
            call.respondText(
                "RemoteKeyTrustee ${rguardian.id} keyShareFor forTrustee ${forTrustee} error='${result.unwrapError()}'",
                status = HttpStatusCode.BadRequest
            )
        }
    }

    post("{id}/receiveKeyShare") {
        val id = call.parameters["id"]
        val rguardian =
            remoteKeyTrustees.find{ it.id == id } ?: return@post call.respondText(
                "No RemoteKeyTrustee with id= $id",
                status = HttpStatusCode.NotFound
            )
        val secretShareJson = call.receive<KeyShareJson>()
        val secretShare = secretShareJson.import(groupContext)
        if (secretShare != null) {
            val result = rguardian.receiveKeyShare(secretShare)
            if (result is Ok) {
                call.respondText(
                    "RemoteKeyTrustee ${rguardian.id} receiveSecretKeyShare from polynomial owner ${secretShare.polynomialOwner} correctly",
                    status = HttpStatusCode.OK
                )
            } else {
                val msg =
                    "RemoteKeyTrustee ${rguardian.id} receiveSecretKeyShare failed ${result.unwrapError()}"
                call.application.environment.log.error(msg)
                call.respondText(msg, status = HttpStatusCode.BadRequest)
            }
        } else {
            val msg = "RemoteKeyTrustee ${rguardian.id} receiveSecretKeyShare importKeyShare failed"
            call.application.environment.log.error(msg)
            call.respondText(msg, status = HttpStatusCode.BadRequest)
        }
    }

    get("{id}/computeSecretKeyShare/{nguardians}") {
        val id = call.parameters["id"]
        val rguardian =
            remoteKeyTrustees.find { it.id == id } ?: return@get call.respondText(
                "No RemoteKeyTrustee with id= $id",
                status = HttpStatusCode.NotFound
            )
        val nguardians = call.parameters["nguardians"] ?: "0"
        val result = rguardian.computeSecretKeyShare(nguardians.toInt())
        if (result is Err) {
            val msg =
                "RemoteKeyTrustee ${rguardian.id} computeSecretKeyShare failed ${result.unwrapError()}"
            call.application.environment.log.error(msg)
            call.respondText(msg, status = HttpStatusCode.BadRequest)
        } else {
            call.respond(result.unwrap().publishJson())
        }
    }

    get("{id}/saveState/{isJson?}") {
        val id = call.parameters["id"]
        val rguardian =
            remoteKeyTrustees.find { it.id == id } ?: return@get call.respondText(
                "No RemoteKeyTrustee with id= $id",
                status = HttpStatusCode.NotFound
            )
        val isJsonS = call.parameters["isJson"] ?: "true"
        val isJson = isJsonS == "true"
        val result = rguardian.saveState(trusteeDir, isJson)
        if (result is Ok) {
            call.respondText(
                "RemoteKeyTrustee ${rguardian.id} saveState succeeded",
                status = HttpStatusCode.OK
            )
        } else {
            call.respondText(
                "RemoteKeyTrustee ${rguardian.id} saveState failed ${result.unwrapError()}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}