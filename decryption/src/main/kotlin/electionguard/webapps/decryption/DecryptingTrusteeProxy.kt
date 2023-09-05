package electionguard.webapps.decryption

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.decrypt.ChallengeRequest
import electionguard.decrypt.ChallengeResponse
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.PartialDecryption
import electionguard.json2.*

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

/** Implement DecryptingTrusteeIF by connecting to a decryptingtrustee webapp. */
class DecryptingTrusteeProxy(
    val client: HttpClient,
    val remoteURL: String,
    val id: String,
    val xcoord: Int,
    val publicKey: ElementModP,
) : DecryptingTrusteeIF {

    init {
        runBlocking {
            val url = "$remoteURL/dtrustee/create/$id"
            val response: HttpResponse = client.get(url)
            println("DecryptingTrusteeProxy create $id = ${response.status}")
        }
    }

    override fun decrypt(
        group: GroupContext,
        texts: List<ElementModP>,
    ): List<PartialDecryption> {
        return runBlocking {
            val url = "$remoteURL/dtrustee/$xcoord/decrypt"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(DecryptRequest(texts).publishJson())
            }
            val decryptResponseJson: DecryptResponseJson = response.body()
            val decryptResponses = decryptResponseJson.import(group)
            if (decryptResponses is Ok) {
                decryptResponses.unwrap().shares
            } else {
                println("$id decrypt = ${response.status} err = ${decryptResponses.unwrapError()}")
                emptyList()
            }
        }
    }

    override fun challenge(
        group: GroupContext,
        challenges: List<ChallengeRequest>,
    ): List<ChallengeResponse> {
        return runBlocking {
            val url = "$remoteURL/dtrustee/$xcoord/challenge"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(ChallengeRequests(challenges).publishJson())
            }
            println("DecryptingTrusteeProxy challenge $id = ${response.status}")
            val challengeResponsesJson: ChallengeResponsesJson = response.body()
            val challengeResponses = challengeResponsesJson.import(group)
            if (challengeResponses is Ok) {
                challengeResponses.unwrap().responses
            } else {
                println("$id challenge = ${response.status} err = ${challengeResponses.unwrapError()}")
                emptyList()
            }
        }
    }

    override fun xCoordinate(): Int {
        return xcoord
    }

    override fun electionPublicKey(): ElementModP {
        return publicKey
    }

    override fun id(): String {
        return id
    }
}