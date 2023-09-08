package bootiful.javareloaded;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

class LambdasTest {

	@Test
	void lambdas() {
		Function<String, Integer> stringIntegerFunction = str -> 2;// <1>
		interface MyHandler {

			String handle(String one, Integer two);

		}
		MyHandler withExplicit = (one, two) -> one + ':' + two;// <2>
		Assertions.assertEquals(stringIntegerFunction.apply(""), 2);
		Assertions.assertEquals(withExplicit.handle("one", 2), "one:2");
		var withVar = (MyHandler) (one, two) -> one + ':' + two;// <3>
		Assertions.assertEquals(withVar.handle("one", 2), "one:2");
		MyHandler delegate = this::doHandle; // <4>
		Assertions.assertEquals(delegate.handle("one", 2), "one:2");

	}

	private String doHandle(String one, Integer two) {
		return one + ':' + two;
	}

}
