package webapps.electionguard.client

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.core.GroupContext
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.productionGroup
import electionguard.core.toUInt256
import electionguard.webapps.client.RemoteEncryptorProxy

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.every
import io.mockk.spyk
import junit.framework.TestCase.assertNotNull
import kotlin.test.assertTrue
import kotlin.text.toByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

private val remoteUrl = "http://0.0.0.0:11180"
private val group = productionGroup()

// Overrides some of the RemoteKeyTrustee responses in order to test keyCeremonyExchange.
// Requires the KeyCeremonyTrustee app to be running, providing the default behaviors.
class EgkClientMock() {

    @Test
    fun testEgkClientMock() {
        val client = HttpClient(Java) {
            install(ContentNegotiation) {
                json()
            }
        }
        val proxy = RemoteEncryptorProxy( client, remoteUrl)
        val spy3 = spyk(proxy)

        /*
        val exchangeResult = keyCeremonyExchange(listOf(trustee1, trustee2, spy3))
        if (exchangeResult is Err) {
            println(exchangeResult.error)
        }
        assertTrue(exchangeResult is Ok)

        // check results
        val kcResults = exchangeResult.unwrap()
        assertEquals(3, kcResults.publicKeys.size)
        assertEquals(3, kcResults.publicKeysSorted.size)

        listOf(trustee1, trustee2, spy3).forEach {
            assertNotNull(it.keyShare())
        }

         */
    }
}

fun generateHashedCiphertext(group: GroupContext): HashedElGamalCiphertext {
    return HashedElGamalCiphertext(group.TWO_MOD_P, "what".toByteArray(), group.TWO_MOD_Q.toUInt256(), 42)
}
