package bootiful.authorizationserver.keys;

import java.util.List;

interface RsaKeyPairRepository {

    // <1>
    List<RsaKeyPair> findKeyPairs();

    // <2>
    void save(RsaKeyPair rsaKeyPair);
}

