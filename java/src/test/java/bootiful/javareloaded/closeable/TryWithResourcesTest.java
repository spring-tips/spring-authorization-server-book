package bootiful.javareloaded.closeable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

class TryWithResourcesTest {

	private final File file = Utils.setup();

	@Test
	void tryWithResources() {
		try (var fileReader = new FileReader(this.file);//
			 var bufferedReader = new BufferedReader(fileReader)) {
			var stringBuilder = new StringBuilder();
			var line = (String) null;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(System.lineSeparator());
			}
			var contents = stringBuilder.toString().trim();
			Assertions.assertEquals(contents, Utils.CONTENTS);
		} //
		catch (IOException e) {
			Utils.error(e);
		}
	}

}
