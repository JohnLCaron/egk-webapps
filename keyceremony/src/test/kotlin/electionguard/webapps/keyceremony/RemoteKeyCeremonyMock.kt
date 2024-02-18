package electionguard.webapps.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.core.*
import electionguard.keyceremony.EncryptedKeyShare
import electionguard.keyceremony.KeyShare
import electionguard.keyceremony.keyCeremonyExchange

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.every
import io.mockk.spyk
import kotlin.test.assertTrue
import kotlin.text.toByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

private val remoteUrl = "http://0.0.0.0:11183/egk"
private val group = productionGroup()

// Overrides some of the RemoteKeyTrustee responses in order to test keyCeremonyExchange.
// Requires the (non-SSL)  KeyCeremonyTrustee app to be running, providing the default behaviors.
class RemoteKeyCeremonyMock() {

    @Test
    fun testRemoteKeyCeremonyMockOk() {
        val client = HttpClient(Java) {
            install(ContentNegotiation) {
                json()
            }
        }
        val trustee1 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id1", 1, 3, 3)
        val trustee2 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id2", 2, 3, 3)
        val trustee3 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id3", 3, 3, 3)
        val spy3 = spyk(trustee3)

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
            it.saveState()
        }
    }

    @Test
    fun testBadEncryptedShare() {
        val client = HttpClient(Java) {
            install(ContentNegotiation) {
                json()
            }
        }
        val trustee1 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id1", 1, 3, 3)
        val trustee2 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id2", 2, 3, 3)
        val trustee3 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id3", 3, 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            trustee3.encryptedKeyShareFor(trustee1.id()) // trustee needs to cache
            // bad EncryptedShare
            Ok(EncryptedKeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), generateHashedCiphertext(group)))
        }

        val exchangeResult = keyCeremonyExchange(listOf(trustee1, trustee2, spy3), false)
        if (exchangeResult is Err) {
            println(exchangeResult.error)
        }
        assertTrue(exchangeResult is Err)
        assertTrue(exchangeResult.error.contains("keyCeremonyExchange had failures exchanging shares"))
        assertTrue(exchangeResult.error.contains("HttpResponse[http://0.0.0.0:11183/egk/ktrustee/id1/receiveEncryptedKeyShare, 400 Bad Request]"))
    }

    @Test
    fun testAllowBadEncryptedShare() {
        val client = HttpClient(Java) {
            install(ContentNegotiation) {
                json()
            }
        }
        val trustee1 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id1", 1, 3, 3)
        val trustee2 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id2", 2, 3, 3)
        val trustee3 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id3", 3, 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            trustee3.encryptedKeyShareFor(trustee1.id()) // trustee needs to cache
            // bad EncryptedShare
            Ok(EncryptedKeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), generateHashedCiphertext(group)))
        }

        val exchangeResult = keyCeremonyExchange(listOf(trustee1, trustee2, spy3), true)
        if (exchangeResult is Err) {
            println(exchangeResult.error)
        }
        assertTrue(exchangeResult is Ok)
    }

    @Test
    fun testBadKeySharesAllowTrue() {
        val client = HttpClient(Java) {
            install(ContentNegotiation) {
                json()
            }
        }
        val trustee1 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id1", 1, 3, 3)
        val trustee2 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id2", 2, 3, 3)
        val trustee3 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id3", 3, 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            trustee3.encryptedKeyShareFor(trustee1.id()) // trustee needs to cache
            // bad EncryptedShare
            Ok(EncryptedKeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), generateHashedCiphertext(group)))
        }
        every { spy3.keyShareFor(trustee1.id()) } answers {
            // bad KeyShare
            Ok(KeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), group.TWO_MOD_Q))
        }

        val exchangeResult = keyCeremonyExchange(listOf(trustee1, trustee2, spy3), true)
        if (exchangeResult is Err) {
            println(exchangeResult.error)
        }
        assertTrue(exchangeResult is Err)
        assertTrue(exchangeResult.error.contains("keyCeremonyExchange had failures exchanging shares"))
//     TODO   assertTrue(exchangeResult.error.contains("HttpResponse[http://0.0.0.0:11180/ktrustee/1/receiveKeyShare, 400 Bad Request]"))
//        assertTrue(exchangeResult.error.contains("HttpResponse[http://0.0.0.0:11180/ktrustee/1/receiveEncryptedKeyShare, 400 Bad Request]"))
    }

    @Test
    fun testBadKeySharesAllowFalse() {
        val client = HttpClient(Java) {
            install(ContentNegotiation) {
                json()
            }
        }
        val trustee1 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id1", 1, 3, 3)
        val trustee2 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id2", 2, 3, 3)
        val trustee3 = RemoteKeyTrusteeProxy(group, client, remoteUrl, "id3", 3, 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            trustee3.encryptedKeyShareFor(trustee1.id()) // trustee needs to cache
            // bad EncryptedShare
            Ok(EncryptedKeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), generateHashedCiphertext(group)))
        }
        every { spy3.keyShareFor(trustee1.id()) } answers {
            // bad KeyShare
            Ok(KeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), group.TWO_MOD_Q))
        }

        val exchangeResult = keyCeremonyExchange(listOf(trustee1, trustee2, spy3), false)
        if (exchangeResult is Err) {
            println(exchangeResult.error)
        }
        assertTrue(exchangeResult is Err)
        assertTrue(exchangeResult.error.contains("keyCeremonyExchange had failures exchanging shares"))
 // TODO       assertTrue(exchangeResult.error.contains("HttpResponse[http://0.0.0.0:11180/ktrustee/1/receiveKeyShare, 400 Bad Request]"))
 //       assertTrue(exchangeResult.error.contains("HttpResponse[http://0.0.0.0:11180/ktrustee/1/receiveEncryptedKeyShare, 400 Bad Request]"))
    }
}

fun generateHashedCiphertext(group: GroupContext): HashedElGamalCiphertext {
    val two = group.binaryToElementModP(ByteArray(3) { 2 })!!
    return HashedElGamalCiphertext(two, "what".toByteArray(), group.TWO_MOD_Q.toUInt256safe(), 42)
}
