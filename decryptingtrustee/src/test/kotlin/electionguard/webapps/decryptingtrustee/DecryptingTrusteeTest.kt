package electionguard.webapps.decryptingtrustee

import electionguard.core.ElementModP
import io.ktor.server.application.*

import electionguard.core.productionGroup
import electionguard.json2.DecryptRequest
import electionguard.json2.publishJson
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.webapps.decryptingtrustee.plugins.configureRouting
import electionguard.webapps.decryptingtrustee.plugins.configureSerialization

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*

import kotlin.test.Test
import kotlin.test.assertEquals

class DecryptingTrusteeTest {
    val trusteeDir =
        "/home/stormy/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/workflow/someAvailableJson/private_data/trustees"

    @Test
    fun testGetTrustees() = testApplication {
        application {
            configureRouting(false, trusteeDir, true)
        }
        client.get("/egk/dtrustee").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("No trustees found", bodyAsText())
        }
    }

    @Test
    fun testReset() = testApplication {
        application {
            configureRouting(false, trusteeDir, true)
        }
        client.post("/egk/dtrustee/reset").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testDecryptEmpty() = testApplication {
        application {
            configureRouting(false, trusteeDir, true)
            configureSerialization()
        }

        val myclient = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
        myclient.get("/egk/dtrustee/load/guardian1").apply {
            println(" = $status")
            println("body = ${bodyAsText()}")
            assertEquals(HttpStatusCode.OK, status)
        }

        val texts = listOf<ElementModP>()
        val url = "/egk/dtrustee/guardian1/decrypt"
        val response: HttpResponse = myclient.post(url) {
            headers {
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(DecryptRequest(texts).publishJson()) // TODO
        }.apply {
            println(" = $status")
            println("body = ${bodyAsText()}")
            assertEquals(HttpStatusCode.OK, status)
        }
        println("response = $response")
    }
}