// 检查：MissingOverrideOnRecordAccessor, FinalClass, NoFinalizer
// 检查：InnerAssignment, EqualsHashCode, StringLiteralEquality, CovariantEquals, AvoidDoubleBraceInitialization
// 检查：SimplifyBooleanExpression, SimplifyBooleanReturn, UnusedLocalVariable, UnusedPrivateField
package base;

import java.util.ArrayList;
import java.util.List;

class CodeQualityCases {
    private int unreferenced;
    List<String> values = new ArrayList<>() {{
        add("初始值");
    }};

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    boolean useValue(String value) {
        int length;
        String text = Integer.toString(length = value.length());
        int unused = text.length();
        return value == "启用";
    }

    boolean simplifyComparison(boolean enabled) {
        return enabled == true;
    }

    boolean simplifyBranches(boolean enabled) {
        if (enabled) {
            return true;
        } else {
            return false;
        }
    }

    protected void finalize() { }

    static class Covariant {
        public boolean equals(Covariant other) {
            return this == other;
        }
    }

    static class PrivateConstructor {
        private PrivateConstructor() { }
    }

    record Item(String name) {
        public String name() {
            return name;
        }
    }
}
