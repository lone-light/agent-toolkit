// 检查：TypeName, MethodName#productionMethodName, MemberName, StaticVariableName, ParameterName
// 检查：LocalVariableName, LocalFinalVariableName, PatternVariableName, LambdaParameterName, CatchParameterName
// 检查：RecordComponentName, ConstantName, ClassTypeParameterName, InterfaceTypeParameterName
// 检查：MethodTypeParameterName, RecordTypeParameterName
package base;

import java.io.IOException;
import java.io.StringReader;
import java.util.function.UnaryOperator;

class NamingCases<value> {
    private static final int maxCount = 10;
    private static int ActiveCount;
    private int Total;

    // 普通方法、构造器、局部变量与资源变量的命名对照
    NamingCases(int InitialCount) {
        Total = InitialCount;
    }

    <item> item bad_name(item Value) {
        item Result = Value;
        final int LIMIT = maxCount;
        return LIMIT > 0 ? Result : Value;
    }

    void scanNames(Object value, String content) {
        if (value instanceof String Label) {
            Total += Label.length();
        }
        UnaryOperator<String> convert = Input -> Input.strip();
        try (var Reader = new StringReader(convert.apply(content))) {
            Total += Reader.read();
        } catch (IOException Failure) {
            throw new IllegalStateException(Failure);
        }
    }

    interface Handler<value> {
        void accept(value item);
    }

    record Entry<item>(item RecordValue) { }

    // 类型名反例；下方是同类正例
    static class lowerCase { }

    interface Mapper<T> {
        T map(T value);
    }

    record Result<T>(T value) { }
}
