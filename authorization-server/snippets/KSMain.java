package bootiful.authorizationserver;

import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

public class KSMain {

    static void keystore() throws Exception {

        // this program assumes you've imported a key into a Java KeyStore file with the following use of keytool
        //keytool -importkeystore -deststorepass <keystore-password> -destkeystore <keystore-name>.jks -srckeystore <source-p8> -srcstoretype PKCS8 -srcstorepass <source-store-password>

        var keystorePasswordCharArray = "password".toCharArray(); // replace with your keystore password
        var keyPasswordCharArray = "keypassword".toCharArray();  // replace with your key's password

        var ks = KeyStore.getInstance("JKS");

        // Load keystore from file
        try (var fis = new FileInputStream("keystore.jks")) {
            ks.load(fis, keystorePasswordCharArray);
        }

        // Get key from keystore
        var key = ks.getKey("mykeyalias", keyPasswordCharArray);  // replace "mykeyalias" with your key's alias
        if (key instanceof SecretKey secretKey) {
            // Handle secret key

        }//
        else if (key instanceof PublicKey publicKey) {
            // Handle private (or public) key
        }//
        else if (key instanceof PrivateKey privateKey) {

        }


    }
}
