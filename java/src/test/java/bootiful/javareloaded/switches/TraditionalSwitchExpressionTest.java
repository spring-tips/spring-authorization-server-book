package bootiful.javareloaded.switches;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TraditionalSwitchExpressionTest {

    enum Emotion {

        // <1>
        HAPPY, SAD

    }

    @Test
    void switchExpression() {
        Assertions.assertEquals(respondToEmotionalState(Emotion.HAPPY), "that's wonderful.");
        Assertions.assertEquals(respondToEmotionalState(Emotion.SAD), "I'm so sorry to hear that.");
    }

    public String respondToEmotionalState(Emotion emotion) {
        var response = ""; // <2>
        switch (emotion) {
            case HAPPY:
                response = "that's wonderful.";
                break; // <3>
            case SAD:
                response = "I'm so sorry to hear that.";
                break;
        }

        return response;
    }

}
