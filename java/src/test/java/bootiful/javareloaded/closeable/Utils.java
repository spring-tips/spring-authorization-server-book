package bootiful.javareloaded.closeable;


import java.io.File;
import java.nio.file.Files;
import java.time.Instant;


public class Utils {

    // <1>
    static String CONTENTS = String.format("""
            <html>
            <body><h1> Hello, world, @ %s !</h1> </body>
            </html>
            """, Instant.now().toString()).trim();

    // <2>
    static File setup() {
        try {
            var path = Files.createTempFile("bootiful", ".txt");
            var file = path.toFile();
            file.deleteOnExit();
            Files.writeString(path, CONTENTS);
            return file;
        }//
        catch (Throwable t) {
            error(t);
            throw new RuntimeException("could not produce a new file");
        }

    }

    // <3>
    static void error(Throwable throwable) {// <2>
        System.err.println(
                "there's been an exception processing the read! " +
                throwable.getMessage());
    }

}
