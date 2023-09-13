# Remote Key Ceremony REST API

_last update 9/13/2023_

Also see [OpenDoc API](../encryptserver/src/main/resources/openapi/documentation.yaml)

route("/egk/ktrustee") {

    get {
        call.respond(List<RemoteKeyTrustee>)
    }

    post {
      val rguardian = call.receive(RemoteKeyTrusteeJson)
      call.respondText("RemoteKeyTrustee ${rguardian.id} created")
      status = HttpStatusCode.Created
    }

    get("{id}/publicKeys") {
        call.respond(PublicKeysJson)
    }
    
    post("{id}/receivePublicKeys") {
       val publicKeys = call.recieve(PublicKeysJson)
       call.respondText( "RemoteKeyTrustee ${rguardian.id} receivePublicKeys from ${publicKeys.guardianId} correctly")
    }
    
    get("{id}/encryptedKeyShareFor/{forTrustee}") {
         call.respond(EncryptedKeyShareJson)
    }  
    
    post("{id}/receiveEncryptedKeyShare") {
       val publicKeys = call.recieve(EncryptedKeyShareJson)
       call.respondText( "RemoteKeyTrustee ${rguardian.id} receiveEncryptedKeyShare correctly")
    }
    
    get("{id}/keyShareFor/{forTrustee}") {
         call.respond(KeyShareJson)
    }  
    
    post("{id}/receiveKeyShare") {
       val publicKeys = call.recieve(KeyShareJson)
       call.respondText( "RemoteKeyTrustee ${rguardian.id} receiveSecretKeyShare correctly)
    }
    
    get("{id}/saveState")  {
       call.respondText("RemoteKeyTrustee ${rguardian.id} saveState succeeded")
    }  
    
    get("{id}/checkComplete") {
         call.respond("true" or "false")
    }  

}