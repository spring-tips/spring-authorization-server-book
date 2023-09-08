package bootiful.javareloaded;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

class SealedTypesTest {

	// <1>
	sealed interface Shape permits Oval, Polygon {

	}

	// <2>
	static sealed class Oval implements Shape permits Circle {

	}

	static final class Circle extends Oval {

	}

	static final class Polygon implements Shape {

	}

	// <3>
	// static final class Rhombus implements Shape {}

	// <4>
	private String describeShape(Shape shape) {
		Assert.notNull(shape, () -> "the shape should never be null!");
		if (shape instanceof Oval)
			return "round";
		if (shape instanceof Polygon)
			return "straight";
		throw new RuntimeException("we should never get to this point!");
	}

	@Test
	void disjointedUnionTypes() {
		Assertions.assertEquals(describeShape(new Oval()), "round");
		Assertions.assertEquals(describeShape(new Polygon()), "straight");
	}

}
