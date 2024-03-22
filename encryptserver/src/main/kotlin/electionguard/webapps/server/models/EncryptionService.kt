package electionguard.webapps.server.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.cryptobiotic.eg.election.ElectionInitialized
import org.cryptobiotic.eg.election.EncryptedBallot
import org.cryptobiotic.eg.election.Manifest
import org.cryptobiotic.eg.election.PlaintextBallot
import org.cryptobiotic.eg.core.*
import org.cryptobiotic.eg.core.Base16.fromHex

import org.cryptobiotic.eg.encrypt.AddEncryptedBallot
import org.cryptobiotic.eg.encrypt.PendingEncryptedBallot
import org.cryptobiotic.eg.input.BallotInputValidation
import org.cryptobiotic.eg.publish.makePublisher
import org.cryptobiotic.eg.publish.readElectionRecord
import org.cryptobiotic.util.ErrorMessages

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

    private val encryptors = mutableMapOf<String, AddEncryptedBallot>()
    private val ballotValidator: BallotInputValidation

    init {
        val electionRecord = readElectionRecord(inputDir)
        manifest = electionRecord.manifest()
        val config = electionRecord.config()
        chainConfirmationCodes = config.chainConfirmationCodes
        configBaux0 = config.configBaux0

        electionInit = electionRecord.electionInit()!!
        isJson = electionRecord.isJson()
        val publisher = makePublisher(outputDir, false)
        publisher.writeElectionInitialized(electionInit)

        ballotValidator = BallotInputValidation(manifest)
    }

    private fun encryptorForDevice(device : String) : AddEncryptedBallot {

    /* public final class AddEncryptedBallot public constructor(
            manifest: org.cryptobiotic.eg.election.Manifest,
            chaining: kotlin.Boolean,
            configBaux0: kotlin.ByteArray,
            jointPublicKey: org.cryptobiotic.eg.core.ElGamalPublicKey,
            extendedBaseHash: org.cryptobiotic.eg.core.UInt256,
            device: kotlin.String,
            outputDir: kotlin.String,
            invalidDir: kotlin.String,
            isJson: kotlin.Boolean
        ) : java.io.Closeable {

     */

        return encryptors.getOrPut(device) {
            AddEncryptedBallot(
                manifest,
                ballotValidator,
                electionInit.config.chainConfirmationCodes,
                electionInit.config.configBaux0,
                electionInit.jointPublicKey,
                electionInit.extendedBaseHash,
                device,
                outputDir,
                "${outputDir}/invalidDir",
                isJson,
            )
        }
    }

    fun encrypt(device: String, ballot: PlaintextBallot) : Result<PendingEncryptedBallot, ErrorMessages> {
        val encryptor = encryptorForDevice(device)
        val errs = ErrorMessages("encrypt")
        val eballot = encryptor.encrypt(ballot, errs)
        return if (errs.hasErrors()) Err(errs) else Ok(eballot!!)
    }

    fun encryptAndCast(device: String, ballot: PlaintextBallot) : Result<EncryptedBallot, ErrorMessages> {
        val encryptor = encryptorForDevice(device)
        val errs = ErrorMessages("encryptAndCast")
        val eballot = encryptor.encryptAndCast(ballot, errs)
        return if (errs.hasErrors()) Err(errs) else Ok(eballot!!)
    }

    fun submit(device: String, ccode: String, state: EncryptedBallot.BallotState) : Result<EncryptedBallot, String> {
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
