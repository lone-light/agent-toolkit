package tests;

import java.util.List;

import static com.puppycrawl.tools.checkstyle.api.SeverityLevel.ERROR;
import static com.puppycrawl.tools.checkstyle.api.SeverityLevel.INFO;
import static com.puppycrawl.tools.checkstyle.api.SeverityLevel.WARNING;
import static tests.RuleCase.method;
import static tests.RuleCase.type;

/**
 * Checkstyle 内置规则的基础正反例
 *
 * @author example
 */
// 测试用例保留边界数值与重复文本，便于逐项阅读
@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:MultipleStringLiterals"})
final class NativeRuleCases {

    private NativeRuleCases() {}

    static List<RuleCase> cases() {
        return List.of(
                new RuleCase("FileTabCharacter", WARNING, "class Example {}\n", "\tclass Example {}\n"),
                new RuleCase("NewlineAtEndOfFile", WARNING, "class Example {}\n", "class Example {}"),
                new RuleCase("LineLength", INFO, "//" + "x".repeat(118) + "\n", "//" + "x".repeat(119) + "\n"),
                new RuleCase("RegexpSingleline#noTrailingWhitespace", WARNING,
                        "class Example {}\n", "class Example {} \n"),
                new RuleCase("OuterTypeFilename", ERROR, "public class Example {}", "public class Other {}"),
                new RuleCase("PackageDeclaration", ERROR, "package sample; class Example {}", "class Example {}"),
                new RuleCase("OneTopLevelClass", WARNING, "class Example { class Nested {} }",
                        "class Example {} class Other {}"),
                new RuleCase("NoLineWrap", WARNING,
                        "package sample; class Example {}", "package\nsample; class Example {}"),
                new RuleCase("ModuleDirectiveOrder", WARNING, """
                        module example {
                            requires java.base;

                            exports example;
                        }
                        """, """
                        module example {
                            exports example;

                            requires java.base;
                        }
                        """, "module-info.java"),
                new RuleCase("PackageName", WARNING, "package sample.good; class Example {}",
                        "package Sample.bad; class Example {}"),
                new RuleCase("TypeName", WARNING, "class Example {}", "class badExample {}"),
                new RuleCase("MethodName#productionMethodName", WARNING,
                        type("void run() {}"), type("void bad_name() {}")),
                new RuleCase("MemberName", WARNING, type("private int count;"), type("private int Count;")),
                new RuleCase("StaticVariableName", WARNING, type("private static int count;"),
                        type("private static int Count;")),
                new RuleCase("ParameterName", WARNING, type("void run(int value) {}"), type("void run(int Value) {}")),
                new RuleCase("LocalVariableName", WARNING, method("int value = 1;"), method("int Value = 1;")),
                new RuleCase("LocalFinalVariableName", WARNING,
                        method("final int value = 1;"), method("final int VALUE = 1;")),
                new RuleCase("PatternVariableName", WARNING,
                        type("void run(Object value) { if (value instanceof String text) {} }"),
                        type("void run(Object value) { if (value instanceof String Text) {} }")),
                new RuleCase("LambdaParameterName", WARNING,
                        method("java.util.function.Function<String, String> f = value -> value;"),
                        method("java.util.function.Function<String, String> f = Value -> Value;")),
                new RuleCase("CatchParameterName", WARNING, method("try { run(); } catch (Exception exception) {}"),
                        method("try { run(); } catch (Exception Exception) {}")),
                new RuleCase("RecordComponentName", WARNING,
                        "record Example(String name) {}", "record Example(String Name) {}"),
                new RuleCase("ConstantName", WARNING, type("static final int MAX_COUNT = 1;"),
                        type("static final int maxCount = 1;")),
                new RuleCase("AbbreviationAsWordInName", WARNING, "class ExampleDTO {}", "class XMLReader {}"),
                new RuleCase("ClassTypeParameterName", WARNING, "class Example<T> {}", "class Example<value> {}"),
                new RuleCase("InterfaceTypeParameterName", WARNING,
                        "interface Example<T> {}", "interface Example<value> {}"),
                new RuleCase("MethodTypeParameterName", WARNING, type("<T> T run(T value) { return value; }"),
                        type("<value> value run(value item) { return item; }")),
                new RuleCase("RecordTypeParameterName", WARNING, "record Example<T>(T value) {}",
                        "record Example<value>(value item) {}"),
                new RuleCase("UnusedImports", WARNING, "import java.util.List; class Example { List<String> names; }",
                        "import java.util.List; class Example {}"),
                new RuleCase("RedundantImport", WARNING, "import java.util.List; class Example {}",
                        "import java.lang.String; class Example {}"),
                new RuleCase("IllegalImport", ERROR, "import com.sun.net.httpserver.HttpServer; class Example {}",
                        "import sun.misc.Unsafe; class Example {}"),
                new RuleCase("ImportOrder", WARNING, "import java.util.List;\nimport java.util.Map;\nclass Example {}",
                        "import java.util.Map;\nimport java.util.List;\nclass Example {}"),
                new RuleCase("Indentation", WARNING, "class Example {\n    private int value;\n}\n",
                        "class Example {\n  private int value;\n}\n"),
                new RuleCase("NeedBraces", ERROR, method("if (true) { run(); }"), method("if (true) run();")),
                new RuleCase("LeftCurly", WARNING, "class Example {\n}\n", "class Example\n{\n}\n"),
                new RuleCase("RightCurly", WARNING, method("if (true) {\nrun();\n} else {\nrun();\n}"),
                        method("if (true) {\nrun();\n}\nelse {\nrun();\n}")),
                new RuleCase("WhitespaceAfter", WARNING, type("void run(int first, int second) {}"),
                        type("void run(int first,int second) {}")),
                new RuleCase("WhitespaceAround", WARNING, method("int value = 1;"), method("int value=1;")),
                new RuleCase("WhitespaceBeforeEmptyBody", WARNING, type("void run() {}"), type("void run(){}")),
                new RuleCase("GenericWhitespace", WARNING, type("java.util.List<String> names;"),
                        type("java.util.List< String > names;")),
                new RuleCase("SingleSpaceSeparator", WARNING, type("int count = 1;"), type("int  count = 1;")),
                new RuleCase("MethodParamPad", WARNING, type("void run() {}"), type("void run () {}")),
                new RuleCase("ParenPad", WARNING, type("void run(int value) {}"), type("void run( int value ) {}")),
                new RuleCase("TypecastParenPad", WARNING, method("Object value = null; String text = (String) value;"),
                        method("Object value = null; String text = ( String ) value;")),
                new RuleCase("EmptyForIteratorPad", WARNING, method("for (int index = 0; index < 1;) { break; }"),
                        method("for (int index = 0; index < 1; ) { break; }")),
                new RuleCase("NoWhitespaceBefore", WARNING, method("run();"), method("run() ;")),
                new RuleCase("NoWhitespaceBeforeCaseDefaultColon", WARNING,
                        method("switch (1) { case 1: break; default: break; }"),
                        method("switch (1) { case 1 : break; default: break; }")),
                new RuleCase("EmptyLineSeparator", WARNING, type("void first() {}\n\nvoid second() {}"),
                        type("void first() {}\nvoid second() {}")),
                new RuleCase("OneStatementPerLine", WARNING, method("int first = 1;\nint second = 2;"),
                        method("int first = 1; int second = 2;")),
                new RuleCase("MultipleVariableDeclarations", WARNING, method("int first;\nint second;"),
                        method("int first, second;")),
                new RuleCase("ArrayTypeStyle", WARNING, type("String[] names;"), type("String names[];")),
                new RuleCase("UpperEll", WARNING, type("long value = 1L;"), type("long value = 1l;")),
                new RuleCase("ModifierOrder", WARNING, type("public static final int VALUE = 1;"),
                        type("static public final int VALUE = 1;")),
                new RuleCase("RedundantModifier", INFO, "interface Example { void run(); }",
                        "interface Example { public void run(); }"),
                new RuleCase("DeclarationOrder", WARNING,
                        type("static int first;\nint second;\nExample() {}\nvoid run() {}"),
                        type("void run() {}\nint value;")),
                new RuleCase("VisibilityModifier", WARNING, type("private int value;"), type("public int value;")),
                new RuleCase("VariableDeclarationUsageDistance", INFO,
                        type("int run() { int value = 1;\n" + "System.nanoTime();\n".repeat(11) + "return value; }"),
                        type("int run() { int value = 1;\n" + "System.nanoTime();\n".repeat(12) + "return value; }")),
                new RuleCase("MissingOverrideOnRecordAccessor", WARNING,
                        "record Example(String name) { @Override public String name() { return name; } }",
                        "record Example(String name) { public String name() { return name; } }"),
                new RuleCase("HideUtilityClassConstructor", WARNING,
                        type("private Example() {} public static void run() {}"),
                        type("public static void run() {}")),
                new RuleCase("FinalClass", WARNING, "final class Example { private Example() {} }",
                        "class Example { private Example() {} }"),
                new RuleCase("NoFinalizer", ERROR,
                        type("protected void close() {}"), type("protected void finalize() {}")),
                new RuleCase("EmptyStatement", ERROR, method("run();"), method(";")),
                new RuleCase("InnerAssignment", WARNING,
                        method("int value = 1; String text = Integer.toString(value);"),
                        method("int value; String text = Integer.toString(value = 1);")),
                new RuleCase("EqualsHashCode", ERROR, type("public boolean equals(Object other) { return false; } "
                        + "public int hashCode() { return 1; }"),
                        type("public boolean equals(Object other) { return false; }")),
                new RuleCase("StringLiteralEquality", ERROR,
                        type("boolean run(String value) { return \"active\".equals(value); }"),
                        type("boolean run(String value) { return value == \"active\"; }")),
                new RuleCase("CovariantEquals", ERROR, type("public boolean equals(Object other) { return false; }"),
                        type("public boolean equals(Example other) { return false; }")),
                new RuleCase("ModifiedControlVariable", ERROR, method("for (int index = 0; index < 2; index++) {}"),
                        method("for (int index = 0; index < 2; index++) { index++; }")),
                new RuleCase("AvoidDoubleBraceInitialization", WARNING,
                        type("Object values = new java.util.ArrayList<String>();"),
                        type("Object values = new java.util.ArrayList<String>() {{ add(\"one\"); }};")),
                new RuleCase("SimplifyBooleanExpression", INFO, type("boolean run(boolean flag) { return flag; }"),
                        type("boolean run(boolean flag) { return flag == true; }")),
                new RuleCase("SimplifyBooleanReturn", INFO, type("boolean run(boolean flag) { return flag; }"),
                        type("boolean run(boolean flag) { if (flag) { return true; } else { return false; } }")),
                new RuleCase("UnusedLocalVariable", WARNING, method("int value = 1; System.out.println(value);"),
                        method("int value = 1;")),
                new RuleCase("UnusedPrivateField", WARNING, type("private int value; int getValue() { return value; }"),
                        type("private int value;")),
                new RuleCase("MagicNumber", WARNING, type("int run() { return 2; }"), type("int run() { return 42; }")),
                new RuleCase("MultipleStringLiterals", INFO, method("run(\"first-value\"); run(\"second-value\");"),
                        method("run(\"same-value\"); run(\"same-value\");")),
                new RuleCase("EmptyCatchBlock", ERROR,
                        method("try { run(); } catch (Exception exception) {\n// 已处理该输入\n}"),
                        method("try { run(); } catch (Exception exception) {}")),
                new RuleCase("FallThrough", ERROR, method("switch (1) { case 1: run(); break; case 2: break; "
                        + "default: break; }"),
                        method("switch (1) { case 1: run(); case 2: break; default: break; }")),
                new RuleCase("DefaultComesLast", WARNING, method("switch (1) { case 1: break; default: break; }"),
                        method("switch (1) { default: break; case 1: break; }")),
                new RuleCase("MissingSwitchDefault", ERROR, method("switch (1) { case 1: break; default: break; }"),
                        method("switch (1) { case 1: break; }")),
                new RuleCase("EmptyBlock", ERROR, method("if (true) {\n// 已完成所需处理\n}"), method("if (true) {}")),
                new RuleCase("AnnotationLocation", WARNING,
                        "@Deprecated\nclass Example {}", "@Deprecated class Example {}"),
                new RuleCase("TrailingComment", WARNING, type("// 字段说明\nint value;"), type("int value; // 字段说明")),
                new RuleCase("TodoComment#noCommentDivider", WARNING, "// 用户说明\nclass Example {}",
                        "// ======\nclass Example {}"),
                new RuleCase("TodoComment#todoMarker", INFO, "// TODO: 后续处理\nclass Example {}",
                        "// todo 后续处理\nclass Example {}"),
                new RuleCase("MissingJavadocType", WARNING, "/** 示例 */\nclass Example {}", "class Example {}"),
                new RuleCase("JavadocType", WARNING, "/**\n * 示例\n *\n * @author example\n */\nclass Example {}",
                        "/**\n * 示例\n */\nclass Example {}"),
                new RuleCase("MissingJavadocMethod", WARNING, "public " + type("/** 执行 */\npublic void run() {}"),
                        "public " + type("public void run() {}")),
                new RuleCase("JavadocMethod", ERROR, type("/**\n * 返回结果\n *\n * @param value 输入值\n * @return 结果\n "
                        + "*/\npublic int run(int value) { return value; }"),
                        type("/** 返回结果 */\npublic int run(int value) { return value; }")),
                new RuleCase("InvalidJavadocPosition", WARNING,
                        type("/** 执行 */\nvoid run() {}"), method("/** 放错位置 */\nrun();")),
                new RuleCase("JavadocMissingLeadingAsterisk", WARNING, "/**\n * 说明\n */\nclass Example {}",
                        "/**\n  说明\n */\nclass Example {}"),
                new RuleCase("JavadocMissingWhitespaceAfterAsterisk", WARNING, "/**\n * 说明\n */\nclass Example {}",
                        "/**\n *说明\n */\nclass Example {}"),
                new RuleCase("JavadocLeadingAsteriskAlign", WARNING, "/**\n * 说明\n */\nclass Example {}",
                        "/**\n  * 说明\n */\nclass Example {}"),
                new RuleCase("JavadocContentLocation", WARNING, "/**\n * 说明\n */\nclass Example {}",
                        "/** 说明\n * 后续内容\n */\nclass Example {}"),
                new RuleCase("JavadocParagraph", WARNING, "/**\n * 说明\n *\n * <p>后续内容\n */\nclass Example {}",
                        "/**\n * 说明\n *\n * 后续内容\n */\nclass Example {}"),
                new RuleCase("SingleLineJavadoc", WARNING, type("/** 结果 */\nint run() { return 1; }"),
                        type("/** @return 结果 */\nint run() { return 1; }")),
                new RuleCase("NonEmptyAtclauseDescription", ERROR,
                        type("/**\n * 结果\n *\n * @return 结果值\n */\nint run() { return 1; }"),
                        type("/**\n * 结果\n *\n * @return\n */\nint run() { return 1; }")),
                new RuleCase("RequireEmptyLineBeforeBlockTagGroup", WARNING,
                        type("/**\n * 结果\n *\n * @return 结果值\n */\nint run() { return 1; }"),
                        type("/**\n * 结果\n * @return 结果值\n */\nint run() { return 1; }")),
                new RuleCase("AtclauseOrder", WARNING, type("/**\n * 结果\n *\n * @param value 输入\n * @return 结果\n "
                        + "*/\nint run(int value) { return value; }"),
                        type("/**\n * 结果\n *\n * @return 结果\n * @param value 输入\n */\nint run(int value) { "
                                + "return value; }")));
    }

}
