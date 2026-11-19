// 检查：NeedBraces, EmptyStatement, ModifiedControlVariable, EmptyCatchBlock
// 检查：FallThrough, DefaultComesLast, MissingSwitchDefault, EmptyBlock
package base;

class ControlFlowCases {
    int total;

    void branch(boolean enabled) {
        if (enabled) total++;
        if (!enabled) {
        }
        ;
    }

    void loop() {
        for (int index = 0; index < total; index++) {
            index++;
        }
    }

    void ignoredFailure() {
        try {
            Integer.parseInt("无效");
        } catch (NumberFormatException exception) {
        }
    }

    void invalidSwitches(int value) {
        switch (value) {
            default: break;
            case 1: total++;
            case 2: break;
        }

        switch (value) {
            case 1: total++; break;
        }
    }

    void documentedFallThrough(int value) {
        switch (value) {
            case 1: total++;
            // fall through
            case 2: total++;
                break;
            default: break;
        }
    }
}
