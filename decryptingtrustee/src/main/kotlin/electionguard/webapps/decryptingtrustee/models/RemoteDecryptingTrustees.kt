package electionguard.webapps.decryptingtrustee.models

import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.decrypt.ChallengeRequest
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.publish.makeConsumer
import electionguard.webapps.decryptingtrustee.groupContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class RemoteDecryptingTrusteeJson(val trusteeDir : String, val isJson : Boolean, val guardianId: String) {
    @Transient
    private val delegate : DecryptingTrusteeIF = readDecryptingTrusteeState(groupContext, guardianId)

    fun id() = guardianId
    fun xCoordinate() = delegate.xCoordinate()
    fun publicKey() = delegate.guardianPublicKey()

    fun decrypt(texts: List<ElementModP>) = delegate.decrypt(groupContext, texts)
    fun challenge(challenges: List<ChallengeRequest>) = delegate.challenge(groupContext, challenges)

    private fun readDecryptingTrusteeState(group: GroupContext, guardianId: String) : DecryptingTrusteeIF {
        val consumer = makeConsumer(group, trusteeDir,  isJson)
        return consumer.readTrustee(trusteeDir, guardianId)
    }
}
