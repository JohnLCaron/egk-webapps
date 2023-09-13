package webapps.electionguard

import electionguard.core.PowRadixOption
import electionguard.core.ProductionMode
import electionguard.core.productionGroup
import electionguard.json2.EncryptedKeyShareJson
import electionguard.json2.KeyShareJson
import electionguard.json2.PublicKeysJson
import electionguard.json2.import
import electionguard.keyceremony.EncryptedKeyShare
import electionguard.keyceremony.KeyShare
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
    val group = productionGroup(PowRadixOption.HIGH_MEMORY_USE, ProductionMode.Mode4096)

    @Test
    fun testGetEmptyTrusteeList() = testApplication {
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
        application {
            configureRouting()
            configureSerialization()
        }

        val body = """{"id":"trustee1","xCoordinate":42,"quorum":3}"""
        client.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(body)
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
        application {
            configureRouting()
            configureSerialization()
        }

        val body = """{"id":"trustee1","xCoordinate":42,"quorum":3}"""
        client.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }

        client.get("/egk/ktrustee/trustee1/publicKeys").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(bodyAsText().startsWith("""{"guardianId":"trustee1","guardianXCoordinate":42,"coefficientProofs":[{"public_key":"""))
        }
    }

    @Test
    fun testGetBadPublicKeys() = testApplication {
        application {
            configureRouting()
            configureSerialization()
        }

        val body = """{"id":"trustee1","xCoordinate":42,"quorum":3}"""
        client.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(body)
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
        val body1 = """{"id":"trustee1","xCoordinate":42,"quorum":3}"""
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(body1)
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }

        val body2 = """{"id":"trustee2","xCoordinate":43,"quorum":3}"""
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(body2)
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

    /*
     get("{id}/encryptedKeyShareFor/{forTrustee}") {
        val id = call.parameters["id"]
        val rguardian =
            remoteKeyTrustees.find { it.id == id } ?: return@get call.respondText(
                "No RemoteKeyTrustee with id= $id",
                status = HttpStatusCode.NotFound
            )
        val forTrustee = call.parameters["forTrustee"] ?: return@get call.respondText(
            "Missing 'forTrustee' id",
            status = HttpStatusCode.BadRequest
        )
        val result: Result<EncryptedKeyShare, String> = rguardian.encryptedKeyShareFor(forTrustee)
        if (result is Ok) {
            call.respond(result.unwrap().publishJson())
        } else {
            call.respondText(
                "RemoteKeyTrustee ${rguardian.id} encryptedKeyShareFor forTrustee ${forTrustee} failed ${result.unwrapError()}",
                status = HttpStatusCode.BadRequest
            )
        }
    }

    override fun encryptedKeyShareFor(otherGuardian: String): Result<EncryptedKeyShare, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$id/encryptedKeyShareFor/$otherGuardian"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    if (isSSL) basicAuth("electionguard", certPassword)
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
     */

    @Test
    fun testGetEncryptedKeyShareFor() = testApplication {
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
            setBody("""{"id":"trustee1","xCoordinate":42,"quorum":3}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"trustee2","xCoordinate":43,"quorum":3}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"trustee3","xCoordinate":44,"quorum":3}""")
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
        assertTrue(encryptedKeyShare.toString().startsWith("EncryptedKeyShare(ownerXcoord=43, polynomialOwner=trustee2, secretShareFor=trustee1, encryptedCoordinate=HashedElGamalCiphertext(c0="))
    }

    /*
    post("{id}/receiveEncryptedKeyShare") {
        val id = call.parameters["id"]
        val rguardian =
            remoteKeyTrustees.find { it.id == id } ?: return@post call.respondText(
                "No RemoteKeyTrustee with id= $id",
                status = HttpStatusCode.NotFound
            )
        val secretShare = call.receive<EncryptedKeyShareJson>()
        val result = rguardian.receiveEncryptedKeyShare(secretShare.import(groupContext))
        if (result is Ok) {
            call.respondText(
                "RemoteKeyTrustee ${rguardian.id} receiveEncryptedKeyShare correctly",
                status = HttpStatusCode.OK
            )
        } else {
            call.respondText(
                "RemoteKeyTrustee ${rguardian.id} receiveEncryptedKeyShare failed ${result.unwrapError()}",
                status = HttpStatusCode.BadRequest
            )
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
                    if (isSSL) basicAuth("electionguard", certPassword)
                }
                setBody(share.publishJson())
            }
            println("$id receiveEncryptedKeyShare from ${share.polynomialOwner} = ${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }
     */
    @Test
    fun testReceiveEncryptedKeyShareMissingPublicKey() = testApplication {
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
            setBody("""{"id":"trustee1","xCoordinate":42,"quorum":3}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"trustee2","xCoordinate":43,"quorum":3}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"trustee3","xCoordinate":44,"quorum":3}""")
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
        assertTrue(encryptedKeyShare.toString().startsWith("EncryptedKeyShare(ownerXcoord=43, polynomialOwner=trustee2, secretShareFor=trustee1, encryptedCoordinate=HashedElGamalCiphertext(c0="))

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
            setBody("""{"id":"trustee1","xCoordinate":42,"quorum":3}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"trustee2","xCoordinate":43,"quorum":3}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"trustee3","xCoordinate":44,"quorum":3}""")
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
        assertTrue(encryptedKeyShare.toString().startsWith("EncryptedKeyShare(ownerXcoord=43, polynomialOwner=trustee2, secretShareFor=trustee1, encryptedCoordinate=HashedElGamalCiphertext(c0="))

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
    fun testKeyShareError() = testApplication {
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
            setBody("""{"id":"trustee1","xCoordinate":42,"quorum":3}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"trustee2","xCoordinate":43,"quorum":3}""")
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
        assertTrue(encryptedKeyShare.toString().startsWith("EncryptedKeyShare(ownerXcoord=43, polynomialOwner=trustee2, secretShareFor=trustee1, encryptedCoordinate=HashedElGamalCiphertext(c0="))

        // send that to trustee1
        //     post("{id}/receiveEncryptedKeyShare") {
        myclient.post("/egk/ktrustee/trustee1/receiveEncryptedKeyShare") {
            contentType(ContentType.Application.Json)
            setBody(encryptedKeyShareJson)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("RemoteKeyTrustee trustee1 receiveEncryptedKeyShare from polynomial owner trustee2 correctly", bodyAsText())
        }

        // try to get trsutee1 key share with not enough shares
        //     get("{id}/computeSecretKeyShare/{nguardians}") {
        myclient.get("/egk/ktrustee/trustee1/computeSecretKeyShare/3") {
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals("RemoteKeyTrustee trustee1 computeSecretKeyShare failed requires nguardians 3 but have 2 shares", bodyAsText())
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun testKeyShare() = testApplication {
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
            setBody("""{"id":"trustee1","xCoordinate":42,"quorum":3}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"trustee2","xCoordinate":43,"quorum":3}""")
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
        assertTrue(encryptedKeyShare.toString().startsWith("EncryptedKeyShare(ownerXcoord=43, polynomialOwner=trustee2, secretShareFor=trustee1, encryptedCoordinate=HashedElGamalCiphertext(c0="))

        // send that to trustee1
        //     post("{id}/receiveEncryptedKeyShare") {
        myclient.post("/egk/ktrustee/trustee1/receiveEncryptedKeyShare") {
            contentType(ContentType.Application.Json)
            setBody(encryptedKeyShareJson)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("RemoteKeyTrustee trustee1 receiveEncryptedKeyShare from polynomial owner trustee2 correctly", bodyAsText())
        }

        myclient.get("/egk/ktrustee/trustee1/computeSecretKeyShare/2") {
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // sneak this test in
        myclient.get("/egk/ktrustee/trustee1/saveState/true").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    // TODO saveState
    /*
        fun saveState(isJson : Boolean): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$id/saveState/$isJson"
            val response: HttpResponse = client.get(url) {
                headers {
                    if (isSSL) basicAuth("electionguard", certPassword)
                }
            }
            println("$id saveState isJson=$isJson status=${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }
     */

    @Test
    fun testSaveState() = testApplication {
        application {
            configureRouting()
            configureSerialization()
        }

        val body = """{"id":"trustee1","xCoordinate":42,"quorum":3}"""
        client.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }

        client.get("/egk/ktrustee/trustee1/saveState/true").apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
            assertEquals("RemoteKeyTrustee trustee1 saveState failed secretKeyShare was not set", bodyAsText())
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Note that these dont usually get called, sinve the above ones succeed.
    // Im unclear in what the above fails, and these get called.

    /*
    override fun keyShareFor(otherGuardian: String): Result<KeyShare, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$id/keyShareFor/$otherGuardian"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    if (isSSL) basicAuth("electionguard", certPassword)
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
                    if (isSSL) basicAuth("electionguard", certPassword)
                }
                setBody(keyShare.publishJson())
            }
            println("$id receiveKeyShare from ${keyShare.polynomialOwner} = ${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }
     */
    @Test
    fun testReceiveKeyShareError() = testApplication {
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
            setBody("""{"id":"trustee1","xCoordinate":42,"quorum":3}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"trustee2","xCoordinate":43,"quorum":3}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"trustee3","xCoordinate":44,"quorum":3}""")
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
            setBody("""{"id":"trustee1","xCoordinate":42,"quorum":3}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee1 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"trustee2","xCoordinate":43,"quorum":3}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals("RemoteKeyTrustee trustee2 created", bodyAsText())
        }
        myclient.post("/egk/ktrustee") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"trustee3","xCoordinate":44,"quorum":3}""")
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
        assertTrue(keyShare.toString().startsWith("KeyShare(ownerXcoord=43, polynomialOwner=trustee2, secretShareFor=trustee1, yCoordinate="))

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