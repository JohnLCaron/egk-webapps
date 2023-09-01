package electionguard.webapps.server.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.Base16.fromHex
import electionguard.core.PowRadixOption
import electionguard.core.ProductionMode
import electionguard.core.UInt256

import electionguard.core.productionGroup
import electionguard.encrypt.AddEncryptedBallot
import electionguard.encrypt.CiphertextBallot
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord

class EncryptionService private constructor(inputDir: String,
                             val outputDir: String,
                             createNew: Boolean,
                             val isJson : Boolean = true
    ) {
    val group = productionGroup(PowRadixOption.HIGH_MEMORY_USE, ProductionMode.Mode4096)
    val manifest : Manifest
    val electionInit : ElectionInitialized
    val chainConfirmationCodes : Boolean
    val configBaux0 : ByteArray

    val encryptors = mutableMapOf<String, AddEncryptedBallot>()

    init {
        val electionRecord = readElectionRecord(group, inputDir)
        manifest = electionRecord.manifest()
        val config = electionRecord.config()
        chainConfirmationCodes = config.chainConfirmationCodes
        configBaux0 = config.configBaux0

        electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, createNew, isJson)
        publisher.writeElectionInitialized(electionInit)
    }

    private fun encryptorForDevice(device : String) : AddEncryptedBallot {
        return encryptors.getOrPut(device) {
            AddEncryptedBallot(
                group,
                manifest,
                electionInit,
                device,
                outputDir,
                "${outputDir}/invalidDir",
                isJson, // isJson
                false,
            )
        }
    }

    fun encrypt(device: String, ballot: PlaintextBallot) : Result<CiphertextBallot, String> {
        val encryptor = encryptorForDevice(device)
        return encryptor.encrypt(ballot)
    }

    fun submit(device: String, ccode: String, state: EncryptedBallot.BallotState) : Result<Boolean, String> {
        val encryptor = encryptorForDevice(device)
        val ba = ccode.fromHex() ?: throw RuntimeException("illegal confirmation code")
        return encryptor.submit(UInt256(ba), state)
        // TODO sync at each submit
    }

    fun sync(device: String) : Result<Boolean, String> {
        val encryptor = encryptorForDevice(device)
        return try {
            encryptor.sync()
            Ok(true)
        } catch (t : Throwable) {
            Err("failed to sync device $device")
        }
    }

    companion object {
        @Volatile private var instance: EncryptionService? = null

        fun initialize(inputDir: String,
                        outputDir: String,
                        createNew: Boolean,
                        isJson : Boolean) =
            instance ?: synchronized(this) {
                instance ?: EncryptionService(inputDir, outputDir, createNew, isJson).also { instance = it }
            }

        // dont call until initialized
        fun getInstance() : EncryptionService = instance!!
    }
}
