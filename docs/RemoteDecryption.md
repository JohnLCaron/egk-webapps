# Remote Decryption REST API

_last update 9/10/2023_

route("/egk/dtrustee")

    get {
        call.respond(List<RemoteDecryptingTrustee>)
    }

    post("reset") {  // should be get - maybe not needed
    }

    get("create/{id?}") {
      val trustee = RemoteDecryptingTrusteeJson(id)
      call.respond(trustee.publicKey()) // return guardian public key = g^s (ElementModP)
    }
    
    post("{id?}/decrypt") {
       call.receive(DecryptRequest)
       call.respond(DecryptResponse)
    }

    post("{id?}/challenge") {
       call.receive(ChallengeRequests)
       call.respond(ChallengeResponses)
    }

data class DecryptRequest(
    val texts: List<ElementModP>
)

data class DecryptResponse(
    val shares: List<PartialDecryption>
)

/** One decryption from one Decrypting Trustee */
data class PartialDecryption(
    val guardianId: String,  // guardian i
    val Mi: ElementModP, // Mi = A ^ P(i); spec 2.0.0, eq 66 or = C0 ^ P(i); eq 77
    val u: ElementModQ,  // these are needed for the proof
    val a: ElementModP,
    val b: ElementModP,
)

interface DecryptingTrusteeIF {

    /** Guardian id. */
    fun id(): String

    /** Guardian x coordinate, for compensated partial decryption  */
    fun xCoordinate(): Int

    /** The guardian's public key = K_i.  */
    fun electionPublicKey(): ElementModP

    /**
     * Compute partial decryptions of elgamal encryptions.
     *
     * @param texts list of ElementModP (ciphertext.pad or A) to be partially decrypted
     * @return a list of partial decryptions, in the same order as the texts
     */
    fun decrypt(
        group: GroupContext,
        texts: List<ElementModP>, 
    ): List<PartialDecryption>

    /**
     * Compute responses to Chaum-Pedersen challenges
     * @param challenges list of Chaum-Pedersen challenges
     * @return a list of responses, in the same order as the challenges
     */
    fun challenge(
        group: GroupContext,
        challenges: List<ChallengeRequest>,
    ): List<ChallengeResponse>
}