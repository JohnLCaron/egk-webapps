package webapps.electionguard

import electionguard.webapps.keyceremonytrustee.plugins.configureRouting
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*

class RunKeyCeremonyTrusteeTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting()
        }
        client.get("/egk/ktrustee").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("No guardians found", bodyAsText())
        }
    }
}