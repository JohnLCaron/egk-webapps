# Remote Decryption REST API

_last update 9/13/2023_

Also see [OpenAPI yaml](../decryptingtrustee/src/main/resources/openapi/documentation.yaml)


route("/egk/dtrustee") {

    get {
        call.respond(List<RemoteDecryptingTrustee>)
    }

    get("create/{id}") {
      val trustee = RemoteDecryptingTrusteeJson(id)
      call.respond(trustee.publicKey()) // return guardian public key = g^s (ElementModP)
    }
    
    post("{id}/decrypt") {
       call.receive(DecryptRequest)
       call.respond(DecryptResponse)
    }

    post("{id}/challenge") {
       call.receive(ChallengeRequests)
       call.respond(ChallengeResponses)
    }
}