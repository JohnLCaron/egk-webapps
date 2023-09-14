package electionguard.webapps.server.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.*
import electionguard.core.Base16.fromHex

import electionguard.encrypt.AddEncryptedBallot
import electionguard.encrypt.CiphertextBallot
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord

class EncryptionService private constructor(
        val group: GroupContext,
        inputDir: String,
        val outputDir: String,
    ) {
    val manifest : Manifest
    val electionInit : ElectionInitialized
    val isJson : Boolean
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
        isJson = electionRecord.isJson()
        val publisher = makePublisher(outputDir, false, isJson)
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
                isJson,
            )
        }
    }

    fun encrypt(device: String, ballot: PlaintextBallot) : Result<CiphertextBallot, String> {
        val encryptor = encryptorForDevice(device)
        return encryptor.encrypt(ballot)
    }

    fun submit(device: String, ccode: String, state: EncryptedBallot.BallotState) : Result<Boolean, String> {
        try {
            val encryptor = encryptorForDevice(device)
            val ba = ccode.fromHex() ?: return Err("illegal confirmation code")
            return encryptor.submit(UInt256(ba), state)
        } catch (t : Throwable) {
            return Err("illegal confirmation code (${t.message})")
        }
    }

    fun challengeAndDecrypt(device: String, ccode: String) : Result<PlaintextBallot, String> {
        val encryptor = encryptorForDevice(device)
        val ba = ccode.fromHex() ?: return Err("illegal confirmation code")
        return encryptor.challengeAndDecrypt(UInt256(ba))
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

        fun initialize(group: GroupContext, inputDir: String, outputDir: String) =
            instance ?: synchronized(this) {
                instance ?: EncryptionService(group, inputDir, outputDir).also { instance = it }
            }

        // dont call until after initialized() is called
        fun getInstance() : EncryptionService = instance!!
    }
}
