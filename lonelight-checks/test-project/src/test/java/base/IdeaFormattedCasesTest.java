package base;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static base.IdeaFormattedCases.OrderState.READY;
import static base.IdeaFormattedCases.OrderState.WAITING;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证排版样例取出首个就绪状态的行为
 *
 * @author lonelight
 */
class IdeaFormattedCasesTest {
    @Test
    void takeFirstReady_readyAfterWaiting_removesFirstReady() {
        var ideaFormattedCases = new IdeaFormattedCases();
        var orderStates = new ArrayList<>(List.of(WAITING, READY, WAITING, READY));

        assertEquals(READY, ideaFormattedCases.takeFirstReady(orderStates));
        assertEquals(List.of(WAITING, WAITING, READY), orderStates);
    }

    @Test
    void takeFirstReady_onlyWaiting_returnsWaiting() {
        var ideaFormattedCases = new IdeaFormattedCases();
        var orderStates = new ArrayList<>(List.of(WAITING));

        assertEquals(WAITING, ideaFormattedCases.takeFirstReady(orderStates));
        assertEquals(List.of(WAITING), orderStates);
    }
}
