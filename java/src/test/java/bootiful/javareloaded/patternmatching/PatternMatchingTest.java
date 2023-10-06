package bootiful.javareloaded.patternmatching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternMatchingTest {

    // <.>
    sealed interface BrowserClient permits UnknownUser, Customer {
    }

    record UnknownUser() implements BrowserClient {
    }

    record Customer(Integer id, String name) implements BrowserClient {
    }

    // <.>
    final String authenticateMessage = """
                please authenticate, so that we can
                get to know you a little better.
            """
            .stripIndent()
            .stripLeading()
            .stripTrailing();

    final String customerMessage = "the customer's name is %s";

    // <.>
    String showMessageWithIf(BrowserClient browserClient) {
        var message = "";
        if (browserClient instanceof Customer(var id, var name)) {
            message = this.customerMessage.formatted(name);
        } else if (browserClient instanceof UnknownUser) {
            message = this.authenticateMessage;
        }
        return message;
    }

    // <.>
    String showMessageWithSwitch(BrowserClient browserClient) {
        return switch (browserClient) {
            case Customer(var id, var name) -> this.customerMessage.formatted(name);
            case UnknownUser() -> this.authenticateMessage;
        };
    }

    @Test
    void patternMatching() throws Exception {
        assertTrue(showMessageWithIf(new Customer(1, "Josh")).contains(this.customerMessage.formatted("Josh")));
        assertTrue(showMessageWithSwitch(new UnknownUser()).contains(this.authenticateMessage));
    }
}
