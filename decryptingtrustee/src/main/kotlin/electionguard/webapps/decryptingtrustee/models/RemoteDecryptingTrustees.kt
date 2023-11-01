package electionguard.webapps.decryptingtrustee.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
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
        val result = consumer.readTrustee(trusteeDir, guardianId)
        if (result is Err) {
            throw RuntimeException(result.error.toString())
        } else {
            return result.unwrap()
        }
    }
}
