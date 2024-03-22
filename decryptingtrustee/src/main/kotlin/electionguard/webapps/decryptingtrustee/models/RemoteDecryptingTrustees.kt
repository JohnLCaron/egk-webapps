package electionguard.webapps.decryptingtrustee.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import org.cryptobiotic.eg.core.ElementModP
import org.cryptobiotic.eg.core.GroupContext
import org.cryptobiotic.eg.decrypt.DecryptingTrusteeIF
import org.cryptobiotic.eg.publish.makeConsumer
import electionguard.webapps.decryptingtrustee.groupContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.cryptobiotic.eg.core.ElementModQ

@Serializable
data class RemoteDecryptingTrusteeJson(val trusteeDir : String, val isJson : Boolean, val guardianId: String) {
    @Transient
    private val delegate : DecryptingTrusteeIF = readDecryptingTrusteeState(groupContext, guardianId)

    fun id() = guardianId
    fun xCoordinate() = delegate.xCoordinate()
    fun publicKey() = delegate.guardianPublicKey()

    fun decrypt(texts: List<ElementModP>) = delegate.decrypt(texts)

    fun challenge(batchId: Int, challenges: List<ElementModQ>) = delegate.challenge(batchId, challenges)

    private fun readDecryptingTrusteeState(group: GroupContext, guardianId: String) : DecryptingTrusteeIF {
        val consumer = makeConsumer(trusteeDir, group)
        val result = consumer.readTrustee(trusteeDir, guardianId)
        if (result is Err) {
            throw RuntimeException(result.error.toString())
        } else {
            return result.unwrap()
        }
    }
}
