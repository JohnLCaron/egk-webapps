package electionguard.webapps.keyceremony

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.SchnorrProof
import electionguard.core.UInt256
import electionguard.json2.*
import electionguard.keyceremony.KeyCeremonyTrusteeIF
import electionguard.keyceremony.KeyShare
import electionguard.keyceremony.PublicKeys
import electionguard.keyceremony.EncryptedKeyShare

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

/** Implement KeyCeremonyTrusteeIF by connecting to a keyceremonytrustee webapp. */
class RemoteKeyTrusteeProxy(
    val group : GroupContext,
    val client: HttpClient,
    val remoteURL: String = "",
    val id: String,
    val xcoord: Int,
    val nguardians: Int,
    val quorum: Int,
    val egPassword: String = "",
) : KeyCeremonyTrusteeIF {
    var publicKeys : PublicKeys? = null

    init {
        runBlocking {
            val url = "$remoteURL/ktrustee"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
                setBody(
                    """{
                      "id": "$id",
                      "xCoordinate": $xcoord,
                      "nguardians": $nguardians
                      "quorum": $quorum
                    }"""
                )
            }
            println("response.status for $id = ${response.status}")
        }
    }

    override fun publicKeys(): Result<PublicKeys, String> {
        if (this.publicKeys != null) {
            return Ok(this.publicKeys!!)
        }
        return runBlocking {
            val url = "$remoteURL/ktrustee/$id/publicKeys"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
            }
            if (response.status != HttpStatusCode.OK) {
                println("response.status for $url = ${response.status}")
                Err("$url error = ${response.status}")
            } else {
                try {
                    val publicKeysJson: PublicKeysJson = response.body()
                    val publicKeys = publicKeysJson.import(group)
                    if (publicKeys == null) {
                        Err("$id error getting publicKeys = ${response.status}")
                    } else {
                        this@RemoteKeyTrusteeProxy.publicKeys = publicKeys
                        Ok(publicKeys)
                    }
                } catch (t : Throwable) {
                    Err(t.message?: "exception importing publicKeys")
                }
            }
        }
    }

    override fun receivePublicKeys(publicKeys: PublicKeys): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$id/receivePublicKeys"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
                setBody(publicKeys.publishJson())
            }
            println("$id receivePublicKeys for ${publicKeys.guardianId} = ${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }

    override fun encryptedKeyShareFor(otherGuardian: String): Result<EncryptedKeyShare, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$id/encryptedKeyShareFor/$otherGuardian"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
            }
            if (response.status != HttpStatusCode.OK) {
                println("response.status for $url = ${response.status}")
                Err("$url error = ${response.status}")
            } else {
                val encryptedKeyShareJson: EncryptedKeyShareJson = response.body()
                val encryptedKeyShare: EncryptedKeyShare? = encryptedKeyShareJson.import(group)
                println("$id encryptedKeyShareFor ${encryptedKeyShare?.secretShareFor} = ${response.status}")
                if (encryptedKeyShare == null) Err("EncryptedKeyShare") else Ok(encryptedKeyShare)
            }
        }
    }

    override fun receiveEncryptedKeyShare(share: EncryptedKeyShare?): Result<Boolean, String> {
        if (share == null) {
            return Err("$id receiveEncryptedKeyShare sent null share")
        }
        return runBlocking {
            val url = "$remoteURL/ktrustee/$id/receiveEncryptedKeyShare"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
                setBody(share.publishJson())
            }
            println("$id receiveEncryptedKeyShare from ${share.polynomialOwner} = ${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }

    override fun keyShareFor(otherGuardian: String): Result<KeyShare, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$id/keyShareFor/$otherGuardian"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
            }
            if (response.status != HttpStatusCode.OK) {
                println("response.status for $url = ${response.status}")
                Err("$url error = ${response.status}")
            } else {
                val keyShareJson: KeyShareJson = response.body()
                val keyShare: KeyShare? = keyShareJson.import(group)
                println("$id secretKeyShareFor ${keyShare?.secretShareFor} = ${response.status}")
                if (keyShare == null) Err("SecretKeyShare") else Ok(keyShare)
            }
        }
    }

    override fun receiveKeyShare(keyShare: KeyShare): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$id/receiveKeyShare"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
                setBody(keyShare.publishJson())
            }
            println("$id receiveKeyShare from ${keyShare.polynomialOwner} = ${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }

    override fun isComplete(): Boolean {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$id/isComplete"
            val response: HttpResponse = client.get(url) {
                headers {
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
            }
            if (response.status == HttpStatusCode.OK) {
                val isComplete = response.bodyAsText()
                isComplete == "true"
            } else {
                false
            }
        }
    }

    fun saveState(electionId : UInt256): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$id/saveState"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    if (isSSL) basicAuth("electionguard", egPassword)
                }
                setBody(electionId.publishJson())
            }
            println("$id saveState status=${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }

    override fun xCoordinate(): Int {
        return xcoord
    }

    override fun coefficientCommitments(): List<ElementModP> {
        publicKeys()
        return publicKeys?.coefficientCommitments() ?: throw IllegalStateException("$id coefficientCommitments failed")
    }

    override fun coefficientProofs(): List<SchnorrProof> {
        publicKeys()
        return publicKeys?.coefficientProofs ?: throw IllegalStateException()
    }

    override fun guardianPublicKey(): ElementModP {
        publicKeys()
        return publicKeys?.publicKey()?.key ?: throw IllegalStateException()
    }

    override fun id(): String {
        return id
    }
}