package electionguard.webapps.decryption

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import org.cryptobiotic.eg.core.ElementModP
import org.cryptobiotic.eg.core.GroupContext
import org.cryptobiotic.eg.publish.json.*

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.cryptobiotic.eg.core.ElGamalPublicKey
import org.cryptobiotic.eg.core.ElementModQ
import org.cryptobiotic.eg.decrypt.*

/** Implement DecryptingTrusteeIF by connecting to a decryptingtrustee webapp. */
class DecryptingTrusteeProxy(
    val group: GroupContext,
    val client: HttpClient,
    val remoteURL: String,
    val id: String,
    val xcoord: Int,
    val publicKey: ElGamalPublicKey,
    val isSSL: Boolean,
    val clientName: String,
    val clientPassword: String?,
) : DecryptingTrusteeIF {
    var initError : String? = null

    init {
        runBlocking {
            val url = "$remoteURL/dtrustee/load/$id"
            val response: HttpResponse = client.get(url)  {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    if (isSSL) basicAuth(clientName, clientPassword!!)
                }
            }
            if (response.status != HttpStatusCode.OK) {
                initError = "DecryptingTrusteeProxy load $id == ${response.status}"
            } else {
                val publicKeyJson: ElementModPJson = response.body()
                val remotePublicKey = publicKeyJson.import(group)
                if (remotePublicKey != publicKey.key) {
                    initError = "DecryptingTrustee $id publicKey does not match election record"
                }
            }
            println("DecryptingTrusteeProxy load $id ${if (initError == null) "OK" else "FAIL"}")
        }
    }

    override fun decrypt(
        texts: List<ElementModP>,
    ): PartialDecryptions {
        return runBlocking {
            val url = "$remoteURL/dtrustee/$id/decrypt"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    if (isSSL) basicAuth(clientName, clientPassword!!)
                }
                setBody(DecryptRequest(texts).publishJson())
            }
            val partialDecryptionsJson: PartialDecryptionsJson = response.body()
            val partialDecryptionsResult = partialDecryptionsJson.import(group)
            if (partialDecryptionsResult is Ok) {
                partialDecryptionsResult.unwrap()
            } else {
                logger.error { "$id decrypt = ${response.status} err = ${partialDecryptionsResult.unwrapError()}" }
                PartialDecryptions(partialDecryptionsResult.toString(), 0, emptyList())
            }
        }
    }

    override fun challenge(
        batchId: Int,
        challenges: List<ElementModQ>,
    ): ChallengeResponses {
        return runBlocking {
            val url = "$remoteURL/dtrustee/$id/challenge"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    if (isSSL) basicAuth(clientName, clientPassword!!)
                }
                setBody(ChallengeRequest(batchId, challenges).publishJson())
            }
            println("DecryptingTrusteeProxy challenge $id = ${response.status}")
            val challengeResponsesJson: ChallengeResponsesJson = response.body()
            val challengeResponsesResult = challengeResponsesJson.import(group)
            if (challengeResponsesResult is Ok) {
                challengeResponsesResult.unwrap()
            } else {
                println("$id challenge = ${response.status} err = ${challengeResponsesResult.unwrapError()}")
                ChallengeResponses(challengeResponsesResult.toString(), 0, emptyList())
            }
        }
    }

    override fun xCoordinate(): Int {
        return xcoord
    }

    override fun guardianPublicKey(): ElGamalPublicKey {
        return publicKey
    }

    override fun id(): String {
        return id
    }

}