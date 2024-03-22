package webapps.electionguard

import org.cryptobiotic.eg.core.productionGroup
import org.cryptobiotic.eg.publish.json.EncryptedKeyShareJson
import org.cryptobiotic.eg.publish.json.KeyShareJson
import org.cryptobiotic.eg.publish.json.PublicKeysJson
import org.cryptobiotic.eg.publish.json.import
import org.cryptobiotic.eg.keyceremony.EncryptedKeyShare
import org.cryptobiotic.eg.keyceremony.KeyShare
import electionguard.webapps.keyceremonytrustee.models.remoteKeyTrustees
import electionguard.webapps.keyceremonytrustee.plugins.configureRouting
import electionguard.webapps.keyceremonytrustee.plugins.configureSerialization
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.test.*
import io.ktor.server.testing.*

class KeyCeremonyTrusteeTest {
    val group = productionGroup()

    @Test
    fun testGetEmptyTrusteeList() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }
        client.get("/egk/ktrustee").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testCreateTrustee() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }

        client.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(1))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }

        client.get("/egk/ktrustee").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testGetTrusteePublicKeys() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }

        client.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(1))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }

        client.get("/egk/ktrustee/trustee1/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(bodyAsText().startsWith("""{"guardianId":"trustee1","guardianXCoordinate":1,"coefficientProofs":[{"public_key":"""))
        }
    }

    @Test
    fun testGetBadPublicKeys() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }

        client.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(1))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }

        client.get("/egk/ktrustee/42/publicKeys").apply {
            assertEquals(HttpStatusCode.NotFound, status)
            assertEquals("No RemoteKeyTrustee with id= 42", bodyAsText())
        }
    }

    @Test
    fun testReceivePublicKeys() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }

        val myclient = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        // create trustees 1 and 2
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(1))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }

        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(2))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }

        // get public keys for trustee1
        val response = myclient.get("/egk/ktrustee/trustee1/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson: PublicKeysJson = response.body()

        // send public keys for trustee1 to trustee2
        myclient.post("/egk/ktrustee/trustee2/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testGetEncryptedKeyShareFor() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }
        val myclient = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        // create trustees
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(1))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(2))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(3))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee3 created", bodyAsText())
        }

        // get public keys for trustee1, send to trustee2
        val response1 = myclient.get("/egk/ktrustee/trustee1/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson: PublicKeysJson = response1.body()
        myclient.post("/egk/ktrustee/trustee2/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // get trustee2's encryptedKeyShareFor trustee1
        val response2 = myclient.get("/egk/ktrustee/trustee2/encryptedKeyShareFor/trustee1").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val encryptedKeyShareJson: EncryptedKeyShareJson = response2.body()
        val encryptedKeyShare: EncryptedKeyShare? = encryptedKeyShareJson.import(group)
        println("---> trustee2's encryptedKeyShareFor trustee1 = ${encryptedKeyShare}")
        assertTrue(encryptedKeyShare.toString().startsWith("EncryptedKeyShare(ownerXcoord=2, polynomialOwner=trustee2, secretShareFor=trustee1, encryptedCoordinate=HashedElGamalCiphertext(c0="))
    }

    @Test
    fun testReceiveEncryptedKeyShareMissingPublicKey() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }
        val myclient = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        // create trustees
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(1))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(2))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(3))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee3 created", bodyAsText())
        }

        // get public keys for trustee1, send to trustee2
        val response1 = myclient.get("/egk/ktrustee/trustee1/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson: PublicKeysJson = response1.body()
        myclient.post("/egk/ktrustee/trustee2/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // get trustee2's encryptedKeyShareFor trustee1
        val response2 = myclient.get("/egk/ktrustee/trustee2/encryptedKeyShareFor/trustee1").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val encryptedKeyShareJson: EncryptedKeyShareJson = response2.body()
        val encryptedKeyShare: EncryptedKeyShare? = encryptedKeyShareJson.import(group)
        println("---> trustee2's encryptedKeyShareFor trustee1 = ${encryptedKeyShare}")
        assertTrue(encryptedKeyShare.toString().startsWith("EncryptedKeyShare(ownerXcoord=2, polynomialOwner=trustee2, secretShareFor=trustee1, encryptedCoordinate=HashedElGamalCiphertext(c0="))

        // send that to trustee1
        //     post("{id}/receiveEncryptedKeyShare") {
        myclient.post("/egk/ktrustee/trustee1/receiveEncryptedKeyShare") {
            contentType(ContentType.Application.Json)
            setBody(encryptedKeyShareJson)
        }.apply {
            assertEquals("RemoteKeyTrustee trustee1 receiveEncryptedKeyShare failed Trustee 'trustee1' does not have public keys for missingGuardianId 'trustee2'", bodyAsText())
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun testReceiveEncryptedKeyShare() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }
        val myclient = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        // create trustees
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(1))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(2))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(3))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee3 created", bodyAsText())
        }

        // get public keys for trustee1, send to trustee2
        val response1 = myclient.get("/egk/ktrustee/trustee1/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson1: PublicKeysJson = response1.body()
        myclient.post("/egk/ktrustee/trustee2/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson1)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        // get public keys for trustee2, send to trustee1
        val response2 = myclient.get("/egk/ktrustee/trustee2/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson2: PublicKeysJson = response2.body()
        myclient.post("/egk/ktrustee/trustee1/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson2)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // get trustee2's encryptedKeyShareFor trustee1
        val responseShare = myclient.get("/egk/ktrustee/trustee2/encryptedKeyShareFor/trustee1").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val encryptedKeyShareJson: EncryptedKeyShareJson = responseShare.body()
        val encryptedKeyShare: EncryptedKeyShare? = encryptedKeyShareJson.import(group)
        println("---> trustee2's encryptedKeyShareFor trustee1 = ${encryptedKeyShare}")
        assertTrue(encryptedKeyShare.toString().startsWith("EncryptedKeyShare(ownerXcoord=2, polynomialOwner=trustee2, secretShareFor=trustee1, encryptedCoordinate=HashedElGamalCiphertext(c0="))

        // send that to trustee1
        //     post("{id}/receiveEncryptedKeyShare") {
        myclient.post("/egk/ktrustee/trustee1/receiveEncryptedKeyShare") {
            contentType(ContentType.Application.Json)
            setBody(encryptedKeyShareJson)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("RemoteKeyTrustee trustee1 receiveEncryptedKeyShare from polynomial owner trustee2 correctly", bodyAsText())
        }
    }

    @Test
    fun testCompleteFalse() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }
        val myclient = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        // create trustees
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(1))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(2))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }

        // get public keys for trustee1, send to trustee2
        val response1 = myclient.get("/egk/ktrustee/trustee1/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson1: PublicKeysJson = response1.body()
        myclient.post("/egk/ktrustee/trustee2/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson1)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        // get public keys for trustee2, send to trustee1
        val response2 = myclient.get("/egk/ktrustee/trustee2/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson2: PublicKeysJson = response2.body()
        myclient.post("/egk/ktrustee/trustee1/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson2)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // get trustee2's encryptedKeyShareFor trustee1
        val responseShare = myclient.get("/egk/ktrustee/trustee2/encryptedKeyShareFor/trustee1").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val encryptedKeyShareJson: EncryptedKeyShareJson = responseShare.body()
        val encryptedKeyShare: EncryptedKeyShare? = encryptedKeyShareJson.import(group)
        println("---> trustee2's encryptedKeyShareFor trustee1 = ${encryptedKeyShare}")
        assertTrue(encryptedKeyShare.toString().startsWith("EncryptedKeyShare(ownerXcoord=2, polynomialOwner=trustee2, secretShareFor=trustee1, encryptedCoordinate=HashedElGamalCiphertext(c0="))

        // send that to trustee1
        //     post("{id}/receiveEncryptedKeyShare") {
        myclient.post("/egk/ktrustee/trustee1/receiveEncryptedKeyShare") {
            contentType(ContentType.Application.Json)
            setBody(encryptedKeyShareJson)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("RemoteKeyTrustee trustee1 receiveEncryptedKeyShare from polynomial owner trustee2 correctly", bodyAsText())
        }

        // not enough shares
        myclient.get("/egk/ktrustee/trustee1/isComplete") {
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals("false", bodyAsText())
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testCompleteTrue() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }
        val myclient = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        // create 3 trustees
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(1, 2))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(2, 2))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }

        // get public keys for trustee1, send to trustee2
        val response1 = myclient.get("/egk/ktrustee/trustee1/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson1: PublicKeysJson = response1.body()
        myclient.post("/egk/ktrustee/trustee2/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson1)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        // get public keys for trustee2, send to trustee1
        val response2 = myclient.get("/egk/ktrustee/trustee2/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson2: PublicKeysJson = response2.body()
        myclient.post("/egk/ktrustee/trustee1/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson2)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // get trustee2's encryptedKeyShareFor trustee1
        val responseShare1 = myclient.get("/egk/ktrustee/trustee2/encryptedKeyShareFor/trustee1").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val encryptedKeyShareJson1: EncryptedKeyShareJson = responseShare1.body()
        val encryptedKeyShare1: EncryptedKeyShare? = encryptedKeyShareJson1.import(group)
        println("---> trustee2's encryptedKeyShareFor trustee1 = ${encryptedKeyShareJson1.import(group)}")
        assertTrue(encryptedKeyShare1.toString().startsWith("EncryptedKeyShare(ownerXcoord=2, polynomialOwner=trustee2, secretShareFor=trustee1, encryptedCoordinate=HashedElGamalCiphertext(c0="))

        // send that to trustee1
        //     post("{id}/receiveEncryptedKeyShare") {
        myclient.post("/egk/ktrustee/trustee1/receiveEncryptedKeyShare") {
            contentType(ContentType.Application.Json)
            setBody(encryptedKeyShareJson1)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("RemoteKeyTrustee trustee1 receiveEncryptedKeyShare from polynomial owner trustee2 correctly", bodyAsText())
        }

        // get trustee2's encryptedKeyShareFor trustee1, send that to trustee2
        val responseShare2 = myclient.get("/egk/ktrustee/trustee1/encryptedKeyShareFor/trustee2").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val encryptedKeyShareJson2: EncryptedKeyShareJson = responseShare2.body()
        myclient.post("/egk/ktrustee/trustee2/receiveEncryptedKeyShare") {
            contentType(ContentType.Application.Json)
            setBody(encryptedKeyShareJson2)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        myclient.get("/egk/ktrustee/trustee1/isComplete") {
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("true", bodyAsText())
        }

        // sneak this test in
        myclient.get("/egk/ktrustee/trustee1/saveState").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testSaveState() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }

        client.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(1, 2))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }

        client.get("/egk/ktrustee/trustee1/saveState").apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
            assertEquals("RemoteKeyTrustee trustee1 saveState failed requires nguardians 2 but have 1 shares", bodyAsText())
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Note that these dont usually get called, sinve the above ones succeed.
    // Im unclear in what case the above fails, and these get called.

    @Test
    fun testReceiveKeyShareError() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }
        val myclient = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        // create trustees
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(1))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(2))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(3))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee3 created", bodyAsText())
        }

        // get public keys for trustee1, send to trustee2
        val response1 = myclient.get("/egk/ktrustee/trustee1/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson1: PublicKeysJson = response1.body()
        myclient.post("/egk/ktrustee/trustee2/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson1)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        // get public keys for trustee2, send to trustee1
        val response2 = myclient.get("/egk/ktrustee/trustee2/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson2: PublicKeysJson = response2.body()
        myclient.post("/egk/ktrustee/trustee1/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson2)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // get trustee2's keyShareFor trustee1, get error
        myclient.get("/egk/ktrustee/trustee2/keyShareFor/trustee1").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertEquals("RemoteKeyTrustee trustee2 keyShareFor forTrustee trustee1 error='Trustee 'trustee2', does not have KeyShare for 'trustee1'; must call encryptedKeyShareFor() first'", bodyAsText())
        }
    }

    @Test
    fun testReceiveKeyShare() = testApplication {
        remoteKeyTrustees.clear()
        application {
            configureRouting()
            configureSerialization()
        }
        val myclient = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        // create trustees
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(1))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(2))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(makeTrusteeJson(3))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee3 created", bodyAsText())
        }

        // get public keys for trustee1, send to trustee2
        val response1 = myclient.get("/egk/ktrustee/trustee1/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson1: PublicKeysJson = response1.body()
        myclient.post("/egk/ktrustee/trustee2/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson1)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        // get public keys for trustee2, send to trustee1
        val response2 = myclient.get("/egk/ktrustee/trustee2/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val publicKeysJson2: PublicKeysJson = response2.body()
        myclient.post("/egk/ktrustee/trustee1/receivePublicKeys") {
            contentType(ContentType.Application.Json)
            setBody(publicKeysJson2)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // must call this first
        myclient.get("/egk/ktrustee/trustee2/encryptedKeyShareFor/trustee1").apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // now can get trustee2's keyShareFor trustee1
        val responseShare = myclient.get("/egk/ktrustee/trustee2/keyShareFor/trustee1").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        val keyShareJson: KeyShareJson = responseShare.body()
        val keyShare: KeyShare? = keyShareJson.import(group)
        println("---> trustee2's keyShare trustee1 = ${keyShare}")
        assertTrue(keyShare.toString().startsWith("KeyShare(ownerXcoord=2, polynomialOwner=trustee2, secretShareFor=trustee1, yCoordinate="))

        // send that to trustee1
        //     post("{id}/receiveKeyShare") {
        myclient.post("/egk/ktrustee/trustee1/receiveKeyShare") {
            contentType(ContentType.Application.Json)
            setBody(keyShareJson)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            println("---> ${bodyAsText()}")
            assertEquals("RemoteKeyTrustee trustee1 receiveSecretKeyShare from polynomial owner trustee2 correctly", bodyAsText())
        }
    }
}

private fun makeTrusteeJson(xcoord: Int, n: Int = 3) : String {
    return """{"id":"trustee$xcoord","xCoordinate":$xcoord,"nguardians":$n,"quorum":$n}"""
}