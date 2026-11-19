package lonelight.checks.naming;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
    void scansFieldsParametersLocalsAndRecordComponents() throws Exception {
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
    void preservesMechanicalPrefixesWithoutSemanticGuessing() throws Exception {
        verify(VariableTypeNameCheck.class,
                "class Example { UserDTO userDTO; UserDTO sourceUserDTO; UserDTO abcUserDTO; ObjectMapper mapper; }", 1);
        verify(VariableTypeNameCheck.class, "class Example { UserDTO sourceUserDTO; }", 1, "allowPrefixes", "false");
    }

    /**
     * 验证缩写、限定名和泛型类型的变量命名
     */
    @Test
    void handlesAcronymsQualifiedNamesGenericObjectsAndReuse() throws Exception {
        verify(VariableTypeNameCheck.class,
                "class Example { java.time.Instant instant; URL url; XMLReader xmlReader; Request<User> request; }", 0);
        verify(VariableTypeNameCheck.class, "class Example { Request<User> value; }", 1);
    }

    /**
     * 验证类型豁免和别名只应用于指定类型
     */
    @Test
    void appliesOnlyExplicitTypeAndAliasExemptions() throws Exception {
        String code = "import java.util.List; import app.UserDTO; class Example { List<UserDTO> users; UserDTO request; }";
        verify(VariableTypeNameCheck.class, code, 0, "ignoredTypes", "java.util.List", "aliases", "app.UserDTO=request");
        verify(VariableTypeNameCheck.class, code, 1, "ignoredTypes", "java.util.List", "aliases", "other.UserDTO=request");
        assertThrows(IllegalArgumentException.class, () -> new VariableTypeNameCheck().setAliases("invalid"));
    }

    /**
     * 验证变量命名规则跳过推断类型和声明的泛型参数
     */
    @Test
    void skipsInferredPrimitiveArrayAndTypeParameterDeclarations() throws Exception {
        verify(VariableTypeNameCheck.class,
                "class Example<T> { static UserDTO DEFAULT; T item; UserDTO[] users; void run() { var thing = create(); int count = 0; } }", 0);
        verify(VariableTypeNameCheck.class, "class Example<InputT> { InputT value; <ResultT> void run(ResultT output) {} }", 0);
        verify(VariableTypeNameCheck.class, "class Example { T value; }", 1);
    }
}
