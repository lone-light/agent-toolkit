package lonelight.checks.configuration;

import java.util.List;

import static com.puppycrawl.tools.checkstyle.api.SeverityLevel.ERROR;
import static com.puppycrawl.tools.checkstyle.api.SeverityLevel.INFO;
import static com.puppycrawl.tools.checkstyle.api.SeverityLevel.WARNING;
import static lonelight.checks.configuration.RuleCase.method;
import static lonelight.checks.configuration.RuleCase.type;

/**
 * 收录内置规则的合规输入和预期等级
 *
 * @author lonelight
 */
// 测试用例保留边界数值与重复文本，便于逐项阅读
@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:MultipleStringLiterals"})
final class NativeRuleCases {

    /**
     * 限制测试用例目录类实例化
     */
    private NativeRuleCases() {}

    /**
     * 列出内置检查的合规输入和预期等级
     *
     * @return 按规则排列的测试用例
     */
    static List<RuleCase> cases() {
        return List.of(
                new RuleCase("FileTabCharacter", WARNING, "class Example {}\n"),
                new RuleCase("NewlineAtEndOfFile", WARNING, "class Example {}\n"),
                new RuleCase("LineLength", INFO, "//" + "x".repeat(118) + "\n"),
                new RuleCase("RegexpSingleline#noTrailingWhitespace", WARNING,
                        "class Example {}\n"),
                new RuleCase("OuterTypeFilename", ERROR, "public class Example {}"),
                new RuleCase("PackageDeclaration", ERROR, "package sample; class Example {}"),
                new RuleCase("OneTopLevelClass", WARNING, "class Example { class Nested {} }"),
                new RuleCase("NoLineWrap", WARNING,
                        "package sample; class Example {}"),
                new RuleCase("ModuleDirectiveOrder", WARNING, """
                        module example {
                            requires java.base;

                            exports example;
                        }
                        """, "module-info.java"),
                new RuleCase("PackageName", WARNING, "package sample.good; class Example {}"),
                new RuleCase("TypeName", WARNING, "class Example {}"),
                new RuleCase("MethodName#productionMethodName", WARNING,
                        type("void run() {}")),
                new RuleCase("MemberName", WARNING, type("private int count;")),
                new RuleCase("StaticVariableName", WARNING, type("private static int count;")),
                new RuleCase("ParameterName", WARNING, type("void run(int value) {}")),
                new RuleCase("LocalVariableName", WARNING, method("int value = 1;")),
                new RuleCase("LocalFinalVariableName", WARNING,
                        method("final int value = 1;")),
                new RuleCase("PatternVariableName", WARNING,
                        type("void run(Object value) { if (value instanceof String text) {} }")),
                new RuleCase("LambdaParameterName", WARNING,
                        method("java.util.function.Function<String, String> f = value -> value;")),
                new RuleCase("CatchParameterName", WARNING, method("try { run(); } catch (Exception exception) {}")),
                new RuleCase("RecordComponentName", WARNING,
                        "record Example(String name) {}"),
                new RuleCase("ConstantName", WARNING, type("static final int MAX_COUNT = 1;")),

                new RuleCase("ClassTypeParameterName", WARNING, "class Example<T> {}"),
                new RuleCase("InterfaceTypeParameterName", WARNING,
                        "interface Example<T> {}"),
                new RuleCase("MethodTypeParameterName", WARNING, type("<T> T run(T value) { return value; }")),
                new RuleCase("RecordTypeParameterName", WARNING, "record Example<T>(T value) {}"),
                new RuleCase("UnusedImports", WARNING, "import java.util.List; class Example { List<String> names; }"),
                new RuleCase("RedundantImport", WARNING, "import java.util.List; class Example {}"),
                new RuleCase("IllegalImport", ERROR, "import com.sun.net.httpserver.HttpServer; class Example {}"),
                new RuleCase("CustomImportOrder", WARNING, """
                        import app.order.Order;

                        import javax.sql.DataSource;
                        import java.util.List;

                        import static java.util.Collections.emptyList;

                        class Example {
                            DataSource source;
                            List<Order> orders = emptyList();
                        }
                        """),
                new RuleCase("Indentation", WARNING, "class Example {\n    private int value;\n}\n"),
                new RuleCase("NeedBraces", ERROR, method("if (true) { run(); }")),
                new RuleCase("LeftCurly", WARNING, "class Example {\n}\n"),
                new RuleCase("RightCurly", WARNING, method("if (true) {\nrun();\n} else {\nrun();\n}")),
                new RuleCase("WhitespaceAfter", WARNING, type("void run(int first, int second) {}")),
                new RuleCase("WhitespaceAround", WARNING, method("int value = 1;")),
                new RuleCase("WhitespaceBeforeEmptyBody", WARNING, type("void run() {}")),
                new RuleCase("GenericWhitespace", WARNING, type("java.util.List<String> names;")),
                new RuleCase("SingleSpaceSeparator", WARNING, type("int count = 1;")),
                new RuleCase("MethodParamPad", WARNING, type("void run() {}")),
                new RuleCase("ParenPad", WARNING, type("void run(int value) {}")),
                new RuleCase("TypecastParenPad", WARNING, method("Object value = null; String text = (String) value;")),
                new RuleCase("EmptyForIteratorPad", WARNING, method("for (int index = 0; index < 1;) { break; }")),
                new RuleCase("NoWhitespaceBefore", WARNING, method("run();")),
                new RuleCase("NoWhitespaceBeforeCaseDefaultColon", WARNING,
                        method("switch (1) { case 1: break; default: break; }")),
                new RuleCase("EmptyLineSeparator", WARNING, type("void first() {}\n\nvoid second() {}")),
                new RuleCase("OneStatementPerLine", WARNING, method("int first = 1;\nint second = 2;")),
                new RuleCase("MultipleVariableDeclarations", WARNING, method("int first;\nint second;")),
                new RuleCase("ArrayTypeStyle", WARNING, type("String[] names;")),
                new RuleCase("UpperEll", WARNING, type("long value = 1L;")),
                new RuleCase("ModifierOrder", WARNING, type("public static final int VALUE = 1;")),
                new RuleCase("RedundantModifier", INFO, "interface Example { void run(); }"),
                new RuleCase("DeclarationOrder", WARNING,
                        type("static int first;\nint second;\nExample() {}\nvoid run() {}")),
                new RuleCase("VisibilityModifier", WARNING, type("private int value;")),
                new RuleCase("VariableDeclarationUsageDistance", INFO,
                        type("int run() { int value = 1;\n" + "System.nanoTime();\n".repeat(11) + "return value; }")),
                new RuleCase("MissingOverrideOnRecordAccessor", WARNING,
                        "record Example(String name) { @Override public String name() { return name; } }"),
                new RuleCase("HideUtilityClassConstructor", WARNING,
                        type("private Example() {} public static void run() {}")),
                new RuleCase("FinalClass", WARNING, "final class Example { private Example() {} }"),
                new RuleCase("NoFinalizer", ERROR,
                        type("protected void close() {}")),
                new RuleCase("EmptyStatement", ERROR, method("run();")),
                new RuleCase("InnerAssignment", WARNING,
                        method("int value = 1; String text = Integer.toString(value);")),
                new RuleCase("EqualsHashCode", ERROR, type("public boolean equals(Object other) { return false; } "
                        + "public int hashCode() { return 1; }")),
                new RuleCase("StringLiteralEquality", ERROR,
                        type("boolean run(String value) { return \"active\".equals(value); }")),
                new RuleCase("CovariantEquals", ERROR, type("public boolean equals(Object other) { return false; }")),
                new RuleCase("ModifiedControlVariable", ERROR, method("for (int index = 0; index < 2; index++) {}")),
                new RuleCase("AvoidDoubleBraceInitialization", WARNING,
                        type("Object values = new java.util.ArrayList<String>();")),
                new RuleCase("SimplifyBooleanExpression", INFO, type("boolean run(boolean flag) { return flag; }")),
                new RuleCase("SimplifyBooleanReturn", INFO, type("boolean run(boolean flag) { return flag; }")),
                new RuleCase("UnusedLocalVariable", WARNING, method("int value = 1; System.out.println(value);")),
                new RuleCase("UnusedPrivateField", WARNING, type("private int value; int getValue() { return value; }")),
                new RuleCase("MagicNumber", WARNING, type("int run() { return 2; }")),
                new RuleCase("MultipleStringLiterals", INFO, method("run(\"first-value\"); run(\"second-value\");")),
                new RuleCase("EmptyCatchBlock", ERROR,
                        method("try { run(); } catch (Exception exception) {\n// 已处理该输入\n}")),
                new RuleCase("FallThrough", ERROR, method("switch (1) { case 1: run(); break; case 2: break; "
                        + "default: break; }")),
                new RuleCase("DefaultComesLast", WARNING, method("switch (1) { case 1: break; default: break; }")),
                new RuleCase("MissingSwitchDefault", ERROR, method("switch (1) { case 1: break; default: break; }")),
                new RuleCase("EmptyBlock", ERROR, method("if (true) {\n// 已完成所需处理\n}")),
                new RuleCase("AnnotationLocation", WARNING,
                        "@Deprecated\nclass Example {}"),
                new RuleCase("TrailingComment", WARNING, type("// 字段说明\nint value;")),
                new RuleCase("TodoComment#noCommentDivider", WARNING, "// 用户说明\nclass Example {}"),
                new RuleCase("TodoComment#todoMarker", INFO, "// TODO: 后续处理\nclass Example {}"),
                new RuleCase("MissingJavadocType", WARNING, "/** 示例 */\npublic class Example {}"),
                new RuleCase("JavadocType", WARNING, "/**\n * 示例\n *\n * @param <T> 数据类型\n */\nclass Example<T> {}"),
                new RuleCase("MissingJavadocMethod", WARNING, "public " + type("/** 执行 */\npublic void run() {}")),
                new RuleCase("JavadocMethod", ERROR, type("""
                        /**
                         * 返回结果
                         *
                         * @param value 输入值
                         * @return 结果
                         */
                        public int run(int value) { return value; }\
                        """)),
                new RuleCase("InvalidJavadocPosition", WARNING,
                        type("/** 执行 */\nvoid run() {}")),
                new RuleCase("JavadocMissingLeadingAsterisk", WARNING, "/**\n * 说明\n */\nclass Example {}"),
                new RuleCase("JavadocMissingWhitespaceAfterAsterisk", WARNING, "/**\n * 说明\n */\nclass Example {}"),
                new RuleCase("JavadocLeadingAsteriskAlign", WARNING, "/**\n * 说明\n */\nclass Example {}"),
                new RuleCase("JavadocContentLocation", WARNING, "/**\n * 说明\n */\nclass Example {}"),
                new RuleCase("JavadocParagraph", WARNING, "/**\n * 说明\n *\n * <p>后续内容\n */\nclass Example {}"),
                new RuleCase("NonEmptyAtclauseDescription", ERROR,
                        type("/**\n * 操作\n *\n * @param value 输入值\n */\nvoid run(int value) {}")),
                new RuleCase("RequireEmptyLineBeforeBlockTagGroup", WARNING,
                        type("/**\n * 结果\n *\n * @return 结果值\n */\nint run() { return 1; }")),
                new RuleCase("AtclauseOrder", WARNING, type("""
                        /**
                         * 结果
                         *
                         * @param value 输入
                         * @return 结果
                         */
                        int run(int value) { return value; }\
                        """)));
    }

}
