package electionguard.webapps.keyceremonytrustee.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.UInt256
import electionguard.json2.UInt256Json

import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.PublicKeys
import electionguard.keyceremony.EncryptedKeyShare
import electionguard.keyceremony.KeyShare
import electionguard.publish.makePublisher
import electionguard.webapps.keyceremonytrustee.groupContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("RemoteKeyTrustee")

val remoteKeyTrustees = mutableListOf<RemoteKeyTrustee>()

@Serializable
data class RemoteKeyTrustee(
    val id: String,
    val xCoordinate: Int,
    val nguardians: Int,
    val quorum: Int,
) {
    @Transient
    private val delegate = KeyCeremonyTrustee(groupContext, id, xCoordinate, nguardians, quorum)

    fun publicKeys() = delegate.publicKeys()
    fun receivePublicKeys(keys: PublicKeys) = delegate.receivePublicKeys(keys)
    fun encryptedKeyShareFor(forGuardian: String) = delegate.encryptedKeyShareFor(forGuardian)
    fun receiveEncryptedKeyShare(share: EncryptedKeyShare?) = delegate.receiveEncryptedKeyShare(share)
    fun keyShareFor(otherGuardian: String): Result<KeyShare, String> = delegate.keyShareFor(otherGuardian)
    fun receiveKeyShare(keyShare: KeyShare): Result<Boolean, String> = delegate.receiveKeyShare(keyShare)
    fun isComplete() = delegate.isComplete()
    fun saveState(trusteeDir: String, isJson: Boolean) = delegate.saveState(trusteeDir, isJson)

    fun KeyCeremonyTrustee.saveState(trusteeDir: String, isJson: Boolean): Result<Boolean, String> {
        // store the trustees in some private place.
        val trusteePublisher = makePublisher(trusteeDir, false, isJson)
        return try {
            trusteePublisher.writeTrustee(trusteeDir, this)
            println("   Write $id to $trusteeDir")
            Ok(true)
        } catch (t: Throwable) {
            logger.error(t) { t.message }
            Err(t.message ?: "KeyCeremonyTrustee.saveState failed=${t.message}")
        }
    }
}
