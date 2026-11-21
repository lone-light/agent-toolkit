package lonelight.checks.framework;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证分层依赖检查的声明范围
 *
 * @author lonelight
 */
class LayerDependencyCheckTest extends CustomCheckTest {
    /**
     * 验证禁止依赖按配置包名和已知 HTTP 类型匹配
     */
    @Test
    void scan_layerDeclarations_recognizesForbiddenTypes() throws Exception {
        verify(LayerDependencyCheck.class, "import app.mapper.UserMapper; @org.springframework.web.bind.annotation.RestController class Example { UserMapper userMapper; }", 1);
        verify(LayerDependencyCheck.class, "@org.springframework.web.bind.annotation.RestController class Example { java.util.List<app.mapper.UserMapper> userMappers; }", 1);
        verify(LayerDependencyCheck.class, "import com.fasterxml.jackson.databind.ObjectMapper; @org.springframework.web.bind.annotation.RestController class Example { ObjectMapper objectMapper; }", 0);
        verify(LayerDependencyCheck.class, "@org.springframework.stereotype.Service class Example { void run(jakarta.servlet.http.HttpServletRequest request) {} }", 1);
        verify(LayerDependencyCheck.class, "import app.User; @org.springframework.stereotype.Service class Example { User get() { return null; } }", 0);
    }

    /**
     * 验证分层检查只处理声明类型
     */
    @Test
    void scan_declaredAndReferencedTypes_flagsDeclarationsOnly() throws Exception {
        String controller = "import app.mapper.UserMapper; @org.springframework.stereotype.Controller ";
        verify(LayerDependencyCheck.class, controller
                + "class Example { java.util.List<@app.Doc(UserMapper.class) String> list; }", 0);
        verify(LayerDependencyCheck.class, controller
                + "class Example { Object get(Object value) { return (UserMapper) value; } }", 0);
        verify(LayerDependencyCheck.class, controller
                + "class Example { void get(UserMapper... mappers) {} }", 1);
    }

    /**
     * 验证多组通配导入下的控制器注解及同名声明的作用域
     */
    @Test
    void scan_wildcardImports_recognizesOnlyKnownController() throws Exception {
        verify(LayerDependencyCheck.class, """
                import app.mapper.UserMapper;
                import org.springframework.web.bind.annotation.*;

                import java.util.*;

                @RestController
                class Example {
                    List<UserMapper> userMappers;
                }
                """, 1);
        verify(LayerDependencyCheck.class, """
                import app.mapper.UserMapper;
                import org.springframework.web.bind.annotation.*;

                import java.util.*;

                class Example {
                    @interface RestController {}

                    @RestController class Local {
                        List<UserMapper> userMappers;
                    }
                }
                """, 0);
    }
}
