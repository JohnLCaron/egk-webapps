**# Encryption Server REST API

_last update 9/13/2023_

Also see [OpenAPI yaml](../encryptserver/src/main/resources/openapi/documentation.yaml)

route("/egk") {

    get("hello" {
        call.respond("Hello!")
    }

    post("{device}/encryptBallot") {
        val plaintextBallotJson = call.receive<PlaintextBallotJson>()
        call.respond(EncryptionResponseJson))
    }

    get("{device}/castBallot/{ccode}") {
        call.respond("EgkServer cast ccode=${ccode} success")
    }
    
    get("{device}/challengeBallot/{ccode}") {
        call.respond("EgkServer challenge ccode=${ccode} success")
    }
    
    get("{device}/challengeAndDecryptBallot/{ccode}") {
         call.respond(PlaintextBallotJson)
    }  
    
    get("{device}/sync") {
         call.respond("EgkServer sync device=${device} success")
    }
}