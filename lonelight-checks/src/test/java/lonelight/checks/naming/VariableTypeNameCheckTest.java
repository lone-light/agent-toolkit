package lonelight.checks.naming;

import lonelight.checks.CheckTestSupport;
import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证变量类型命名和适用的豁免
 *
 * @author lonelight
 */
class VariableTypeNameCheckTest extends CustomCheckTest {
    /**
     * 验证类型命名检查覆盖各类变量声明
     */
    @Test
    void scan_objectVariables_checksSupportedDeclarations() throws Exception {
        verify(VariableTypeNameCheck.class, """
                package sample;

                class Example {
                    UserDTO userDTO;
                    UserDTO sourceUserDTO;
                    UserDTO request;

                    void run(UserDTO value) {
                        UserDTO data;
                    }

                    record Nested(UserDTO item) { }
                }
                """, 4);
    }

    /**
     * 验证上下文前缀只按配置和字符形式判定
     */
    @Test
    void scan_prefixedVariableNames_checksSpellingNotMeaning() throws Exception {
        verify(VariableTypeNameCheck.class,
                "class Example { UserDTO userDTO; UserDTO sourceUserDTO; UserDTO abcUserDTO; ObjectMapper mapper; }", 1);
        verify(VariableTypeNameCheck.class, "class Example { UserDTO sourceUserDTO; }", 1, "allowPrefixes", "false");
    }

    /**
     * 验证缩写、限定名和泛型类型的变量命名
     */
    @Test
    void scan_acronymsAndGenericTypes_matchesTypeNames() throws Exception {
        verify(VariableTypeNameCheck.class,
                "class Example { java.time.Instant instant; URL url; XMLReader xmlReader; Request<User> request; }", 0);
        verify(VariableTypeNameCheck.class, "class Example { Request<User> value; }", 1);
    }

    /**
     * 验证类型豁免和别名只应用于指定类型
     */
    @Test
    void scan_configuredExemptions_appliesExactTypeAndAlias() throws Exception {
        String code = "import java.util.List; import app.UserDTO; class Example { List<UserDTO> users; UserDTO request; }";
        verify(VariableTypeNameCheck.class, code, 0, "ignoredTypes", "java.util.List", "aliases", "app.UserDTO=request");
        verify(VariableTypeNameCheck.class, code, 1, "ignoredTypes", "java.util.List", "aliases", "other.UserDTO=request");
        assertThrows(IllegalArgumentException.class, () -> new VariableTypeNameCheck().setAliases("invalid"));
    }

    /**
     * 验证变量命名规则跳过推断类型和声明的泛型参数
     */
    @Test
    void scan_exemptDeclarationKinds_omitsDiagnostics() throws Exception {
        verify(VariableTypeNameCheck.class,
                "class Example<T> { static UserDTO DEFAULT; T item; UserDTO[] users; void run() { var thing = create(); int count = 0; } }", 0);
        verify(VariableTypeNameCheck.class, "class Example<InputT> { InputT value; <ResultT> void run(ResultT output) {} }", 0);
        verify(VariableTypeNameCheck.class, "class Example { T value; }", 1);
    }

    /**
     * 验证限定类型的注解、注解参数和泛型不参与类型名匹配
     */
    @Test
    void scan_annotatedQualifiedTypes_matchesOnlyTypeSegments() throws Exception {
        String source = """
                import java.lang.annotation.*;
                @Target(ElementType.TYPE_USE) @interface Mark { Class<?> value() default Void.class; }
                class Outer<T> { class Middle<U> { class Inner {} } }
                class Example {
                    Outer<@Mark String>.@Mark(Integer.class) Middle<Boolean>.@Mark Inner inner;
                    java.util.Map.@Mark Entry<String, Integer> entry;
                    java.util.Map.@Mark Entry<String, Integer> row;
                }
                """;
        verify(VariableTypeNameCheck.class, source, 1);
        verify(VariableTypeNameCheck.class, source, 0, "aliases", "java.util.Map.Entry=row");
    }

    /**
     * 验证接收者参数、可变参数、隐式静态字段和未命名变量无需按类型命名
     */
    @Test
    void scan_declarationsWithoutInstanceNames_skipsOnlyExemptBindings() throws Exception {
        verify(VariableTypeNameCheck.class, """
                class Example {
                    class Inner { Inner(Example Example.this) {} }
                    interface Constants { Example DEFAULT = null; }
                    @interface Config { Example DEFAULT = null; }
                    record Group(Example... entries) {}
                    void read(Example this, Example... entries) {
                        Example _ = null;
                        Example wrong;
                    }
                }
                """, 1);
    }

    /**
     * 验证资源声明和具名模式变量触发检查，已有资源引用和未命名模式跳过
     */
    @Test
    void scan_resourceAndPatternBindings_checksExplicitNames() throws Exception {
        verify(VariableTypeNameCheck.class, """
                class Example {
                    void read(AutoCloseable autoCloseable, Object object) throws Exception {
                        try (autoCloseable; AutoCloseable second = null) {}
                        if (object instanceof String bad) {}
                        if (object instanceof String string) {}
                        if (object instanceof String _) {}
                    }
                }
                """, 2);
    }

    /**
     * 验证通配导入的类型豁免只用于当前位置可见的类型
     */
    @Test
    void scan_wildcardExemptions_respectsLexicalTypeAndExplicitImports() throws Exception {
        String ignoredType = "java.util.List";
        verify(VariableTypeNameCheck.class, """
                package demo;
                import java.util.*;
                import other.*;
                class Example {
                    List<String> names;
                    class Local { class List {} List wrong; }
                }
                """, 1, "ignoredTypes", ignoredType);
        verify(VariableTypeNameCheck.class, """
                package demo;
                import java.util.*;
                import other.List;
                class Example { List wrong; }
                """, 1, "ignoredTypes", ignoredType);
        verify(VariableTypeNameCheck.class, """
                package demo;
                import java.util.List;
                class Example { class Local { class List {} List wrong; } }
                """, 1, "ignoredTypes", ignoredType);
        verify(VariableTypeNameCheck.class, """
                package demo;
                import java.util.*;
                class Example { Method wrong; OtherType value; }
                """, 2, "ignoredTypes", "java.lang.reflect.Method,DefaultPackageType");
    }

    /**
     * 验证同包源码中的类型先于通配导入，显式导入优先于同包其他文件
     */
    @Test
    void scan_siblingAndTypeParameter_respectsJavaTypeScope() throws Exception {
        CheckTestSupport.write(directoryPath, "List.java", "package demo; class List {}");
        verify(VariableTypeNameCheck.class, """
                package demo;
                import java.util.*;
                class Example<ListParam> {
                    List wrong;
                    ListParam any;
                    <List> void use(List any) {}
                }
                """, 1, "ignoredTypes", "java.util.List");
        verify(VariableTypeNameCheck.class, """
                package demo;
                import java.util.List;
                class Example { List<String> names; }
                """, 0, "ignoredTypes", "java.util.List");
    }

}
