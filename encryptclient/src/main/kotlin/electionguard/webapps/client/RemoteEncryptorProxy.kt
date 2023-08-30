package electionguard.webapps.client

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import electionguard.ballot.PlaintextBallot
import electionguard.core.*
import electionguard.json2.*
import electionguard.keyceremony.KeyCeremonyTrusteeIF
import electionguard.keyceremony.KeyShare
import electionguard.keyceremony.PublicKeys
import electionguard.keyceremony.EncryptedKeyShare

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

/** Implement KeyCeremonyTrusteeIF by connecting to a keyceremonytrustee webapp. */
class RemoteEncryptorProxy(
    val client: HttpClient,
    val remoteURL: String,
) {

    fun encryptBallot(device: String, ballot: PlaintextBallot): Result<String, String> {
        return runBlocking {
            val url = "$remoteURL/encryptBallot/$device"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    // basicAuth("electionguard", certPassword)
                }
                setBody(ballot.publishJson())
            }
            println("RemoteEncryptorProxy encryptBallot for ballotId=${ballot.ballotId} = ${response.status}")
            val ccodeJson: EncryptionResponseJson = response.body()
            if (response.status == HttpStatusCode.OK) Ok(ccodeJson.confirmationCode) else Err(response.toString())
        }
    }

    fun castBallot(device: String, ccode: String): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/castBallot/$device/$ccode"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    // basicAuth("electionguard", certPassword)
                }
            }
            println("RemoteEncryptorProxy castBallot for ccode=${ccode} = ${response.status}")

            if (response.status != HttpStatusCode.OK) {
                println("response.status for $url = ${response.status}")
                Err("$url error = ${response.status}")
            } else {
                Ok(true)
            }
        }
    }
}