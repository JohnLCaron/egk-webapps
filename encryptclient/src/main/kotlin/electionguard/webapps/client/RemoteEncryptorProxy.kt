package electionguard.webapps.client

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.PlaintextBallot
import electionguard.core.GroupContext
import electionguard.json2.*

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

/** Implement KeyCeremonyTrusteeIF by connecting to a keyceremonytrustee webapp. */
class RemoteEncryptorProxy(
    val group: GroupContext,
    val client: HttpClient,
    val remoteURL: String,
) {

    fun hello(): Boolean {
        return runBlocking {
            val url = "$remoteURL/hello"
            val response: HttpResponse = client.get(url) {
                headers {
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
            }
            println("Contact with Server $url = ${response.status}")
            response.status == HttpStatusCode.OK
        }
    }

    fun encryptBallot(device: String, ballot: PlaintextBallot): Result<String, String> {
        return runBlocking {
            val url = "$remoteURL/$device/encryptBallot"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    append(HttpHeaders.ContentType, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
                setBody(ballot.publishJson())
            }
            println("RemoteEncryptorProxy encryptBallot for ballotId=${ballot.ballotId} = ${response.status}")
            val ccodeJson: EncryptionResponseJson = response.body()
            if (response.status == HttpStatusCode.OK) Ok(ccodeJson.confirmationCode) else Err(response.toString())
        }
    }

    fun encryptAndCastBallot(device: String, ballot: PlaintextBallot): Result<EncryptedBallot, String> {
        return runBlocking {
            val url = "$remoteURL/$device/encryptAndCastBallot"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    append(HttpHeaders.ContentType, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
                setBody(ballot.publishJson())
            }
            println("RemoteEncryptorProxy encryptAndCastBallot for ballotId=${ballot.ballotId} = ${response.status}")
            val eballotJson: EncryptedBallotJson = response.body()
            val eballot = eballotJson.import(group)
            if (response.status == HttpStatusCode.OK) Ok(eballot) else Err(response.toString())
        }
    }

    fun castBallot(device: String, ccode: String): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/$device/castBallot/$ccode"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
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

    fun challengeBallot(device: String, ccode: String): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/$device/challengeBallot/$ccode"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
            }
            println("RemoteEncryptorProxy challengeBallot for ccode=${ccode} = ${response.status}")

            if (response.status != HttpStatusCode.OK) {
                println("response.status for $url = ${response.status}")
                Err("$url error = ${response.status}")
            } else {
                Ok(true)
            }
        }
    }

    fun challengeAndDecryptBallot(device: String, ccode: String): Result<PlaintextBallot, String> {
        return runBlocking {
            val url = "$remoteURL/$device/challengeAndDecryptBallot/$ccode"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
            }
            println("RemoteEncryptorProxy castBallot for ccode=${ccode} = ${response.status}")

            if (response.status != HttpStatusCode.OK) {
                println("response.status for $url = ${response.status}")
                Err("$url error = ${response.status}")
            } else {
                val dballotJson: PlaintextBallotJson = response.body()
                if (response.status == HttpStatusCode.OK) Ok(dballotJson.import()) else Err(response.toString())
            }
        }
    }

    fun sync(device: String): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/$device/sync"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
            }
            println("RemoteEncryptorProxy sync device=${device} = ${response.status}")

            if (response.status != HttpStatusCode.OK) {
                println("response.status for $url = ${response.status}")
                Err("$url error = ${response.status}")
            } else {
                Ok(true)
            }
        }
    }
}