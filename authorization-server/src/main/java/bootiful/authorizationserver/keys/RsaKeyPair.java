package bootiful.authorizationserver.keys;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;


// <1>
record RsaKeyPair(String id, Instant created, RSAPublicKey publicKey, RSAPrivateKey privateKey) {
}
