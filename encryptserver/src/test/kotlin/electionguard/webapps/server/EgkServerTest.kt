package electionguard.webapps.server

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.*

import electionguard.ballot.PlaintextBallot
import electionguard.core.productionGroup
import electionguard.input.RandomBallotProvider
import electionguard.json2.publishJson
import electionguard.json2.EncryptionResponseJson
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.webapps.server.models.EncryptionService
import electionguard.webapps.server.plugins.configureRouting
import electionguard.webapps.server.plugins.configureSerialization

import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.serialization.json.*

class EgkServerTest {
    val inputDir = "../testInput/chained"
    val outputDir = "testOut/encrypt/EgkServerTest"

    init {
        // clean out the output directory
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, inputDir)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, electionRecord.isJson())
        publisher.writeElectionInitialized(electionInit)
    }

    @Test
    fun testBadRoute() = testApplication {
        EncryptionService.initialize(inputDir, outputDir)

        application {
            configureRouting()
        }

        client.get("/").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
        client.get("/egk/badroute").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun testBadCast() = testApplication {
        EncryptionService.initialize(inputDir, outputDir)

        application {
            configureRouting()
        }

        val response = client.get("/egk/castBallot/device0/42").apply {
            println("status = $status bodyAsText = ${bodyAsText()}")
            assertEquals(HttpStatusCode.BadRequest, status)
            assertEquals("EgkServer cast ccode=42 failed 'illegal confirmation code (UInt256 must have exactly 32 bytes)'", bodyAsText())
        }
        println("response = $response")
    }

    @Test
    fun testBadSpoil() = testApplication {
        EncryptionService.initialize(inputDir, outputDir)

        application {
            configureRouting()
        }

        val response = client.get("/egk/challengeBallot/device0/xyz").apply {
            println("status = $status bodyAsText = ${bodyAsText()}")
            assertEquals(HttpStatusCode.BadRequest, status)
            assertEquals("EgkServer spoil ccode=xyz failed 'illegal confirmation code'", bodyAsText())
        }
        println("response = $response")
    }

    @Test
    fun testEncrypt() = testApplication {
        EncryptionService.initialize(inputDir, outputDir)

        application {
            configureRouting()
            configureSerialization()
        }

        val myclient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val encryptionService = EncryptionService.getInstance()
        val ballotProvider = RandomBallotProvider(encryptionService.manifest)
        repeat(7) {
            val ballot : PlaintextBallot = ballotProvider.makeBallot()

            myclient.post("/egk/encryptBallot/device42") {
                contentType(ContentType.Application.Json)
                setBody(ballot.publishJson())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                println(" encryptBallotResponse body=${bodyAsText()}")
                val responseJson = Json.decodeFromString<EncryptionResponseJson>(bodyAsText())
                println(" responseJson cc=${responseJson.confirmationCode}")

                myclient.get("/egk/castBallot/device42/${responseJson.confirmationCode}").apply {
                    // assertEquals(HttpStatusCode.OK, status)
                    println(" castBallot state = $status body=${bodyAsText()}")
                }

            }
        }
        val doneResult = encryptionService.sync("device42")
        println("done = $doneResult")
    }
}