# Remote Key Ceremony REST API

_last update 9/10/2023_

route("/egk/ktrustee")

    get {
        call.respond(List<RemoteKeyTrustee>)
    }

    post {
      val rguardian = call.receive(RemoteKeyTrustee)
      call.respondText("RemoteKeyTrustee ${rguardian.id} created")
      status = HttpStatusCode.Created
    }

    get("{id?}/publicKeys") {
        call.respond(PublicKeys)
    }
    
    post("{id?}/receivePublicKeys") {
       val publicKeys = call.recieve(PublicKeys)
       call.respondText( "RemoteKeyTrustee ${rguardian.id} receivePublicKeys from ${publicKeys.guardianId} correctly")
    }
    
    get("{id?}/{from?}/encryptedKeyShareFor") {
         call.respond(EncryptedKeyShare)
    }  
    
    post("{id?}/receiveEncryptedKeyShare") {
       val publicKeys = call.recieve(EncryptedKeyShare)
       call.respondText( "RemoteKeyTrustee ${rguardian.id} receiveEncryptedKeyShare correctly")
    }
    
    get("{id?}/{from?}/keyShareFor") {
         call.respond(KeyShare)
    }  
    
    post("{id?}/receiveKeyShare") {
       val publicKeys = call.recieve(KeySharee)
       call.respondText( "RemoteKeyTrustee ${rguardian.id} receiveSecretKeyShare correctly)
    }
    
    get("{id?}/saveState/{isJson?}")  {
       call.respondText("RemoteKeyTrustee ${rguardian.id} saveState succeeded")
    }  
    
    get("{id?}/keyShare") {
         call.respond(rguardian.keyShare()) // ElementModQ
    }  