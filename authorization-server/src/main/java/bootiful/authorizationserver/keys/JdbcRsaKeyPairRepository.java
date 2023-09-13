package bootiful.authorizationserver.keys;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Component
class JdbcRsaKeyPairRepository implements RsaKeyPairRepository {

    private final JdbcTemplate template;

    private final RsaPublicKeySerializer rsaPublicKeySerializer;

    private final RsaPrivateKeySerializer rsaPrivateKeySerializer;

    private final RowMapper<RsaKeyPair> keyPairRowMapper;

    JdbcRsaKeyPairRepository(
            RowMapper<RsaKeyPair> keyPairRowMapper,
            RsaPublicKeySerializer publicKeySerializer,
            RsaPrivateKeySerializer privateKeySerializer,
            JdbcTemplate template) {
        this.template = template;
        this.keyPairRowMapper = keyPairRowMapper;
        this.rsaPublicKeySerializer = publicKeySerializer;
        this.rsaPrivateKeySerializer = privateKeySerializer;
    }

    @Override
    public List<RsaKeyPair> findKeyPairs() {
        return this.template.query("select * from rsa_key_pairs order by created desc", this.keyPairRowMapper);
    }

    @Override
    public void save(RsaKeyPair keyPair) {
        var sql = """
                INSERT INTO rsa_key_pairs (id, private_key, public_key, created)
                VALUES (?, ?, ?, ?)
                on conflict  on constraint rsa_key_pairs_id_created_key
                do nothing
                """;
        try (var privateBaos = new ByteArrayOutputStream();
             var publicBaos = new ByteArrayOutputStream()
        ) {
            this.rsaPrivateKeySerializer.serialize(keyPair.privateKey(), privateBaos);
            this.rsaPublicKeySerializer.serialize(keyPair.publicKey(), publicBaos);
            var updated = this.template.update(sql, keyPair.id(),
                    privateBaos.toString(), publicBaos.toString(), new Date(keyPair.created().toEpochMilli()));
            Assert.state(updated == 0 || updated == 1, "no more than one record should have been updated");
        }//
        catch (IOException e) {
            throw new IllegalArgumentException("there's been an exception", e);
        }
    }
}
