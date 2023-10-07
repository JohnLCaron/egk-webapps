package electionguard.webapps.decryptingtrustee.models

import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.decrypt.ChallengeRequest
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.publish.makeConsumer
import electionguard.webapps.decryptingtrustee.groupContext
import electionguard.webapps.decryptingtrustee.trusteeDir
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging

@Serializable
data class RemoteDecryptingTrusteeJson(val guardian_id: String) {
    @Transient
    private val delegate = readState(groupContext, guardian_id)

    fun id() = guardian_id
    fun xCoordinate() = delegate.xCoordinate()
    fun publicKey() = delegate.guardianPublicKey()

    fun decrypt(texts: List<ElementModP>) = delegate.decrypt(groupContext, texts)
    fun challenge(challenges: List<ChallengeRequest>) = delegate.challenge(groupContext, challenges)
}

fun readState(group: GroupContext, guardianId: String) : DecryptingTrusteeIF {
    val consumer = makeConsumer(group, trusteeDir,  true)
    return consumer.readTrustee(trusteeDir, guardianId)
}
