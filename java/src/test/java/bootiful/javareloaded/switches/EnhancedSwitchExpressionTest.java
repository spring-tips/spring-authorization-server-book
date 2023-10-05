package bootiful.javareloaded.switches;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EnhancedSwitchExpressionTest {

    @Test
    void switchExpression() {
        Assertions.assertEquals(respondToEmotionalState(Emotion.HAPPY), "that's wonderful.");
        Assertions.assertEquals(respondToEmotionalState(Emotion.SAD), "I'm so sorry to hear that.");
    }

    private String respondToEmotionalState(Emotion emotion) { // <1>
        return switch (emotion) {
            case HAPPY -> "that's wonderful.";
            case SAD -> "I'm so sorry to hear that.";
        };
    }

    enum Emotion {

        HAPPY, SAD;

    }

}
