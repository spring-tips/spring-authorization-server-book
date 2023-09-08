package bootiful.javareloaded;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

public class SmartCastsTest {

    @Test
    void casts() {
        interface Animal {

            String speak();

        }

        class Cat implements Animal {

            @Override
            public String speak() {
                return "meow!";
            }

        }

        class Dog implements Animal {

            @Override
            public String speak() {
                return "woof!";
            }

        }

        var newPet = Math.random() < .5 ? new Cat() : new Dog();
        var messages = new HashSet<String>();

        // <1>
        if (newPet instanceof Cat) {
            var cat = (Cat) newPet;
            messages.add("the cat says " + cat.speak());
        }
        // <2>
        if (newPet instanceof Cat cat) {
            messages.add("the cat says " + cat.speak());
        }

        if (newPet instanceof Dog dog) {
            messages.add("the dog says " + dog.speak());
        }

        Assertions.assertEquals(messages.size(), 1);
        Assertions.assertTrue(messages.contains("the dog says woof!") || messages.contains("the cat says meow!"));

    }

}
