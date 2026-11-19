package lonelight.checks.framework;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证 REST 返回类型中的持久化实体识别
 *
 * @author lonelight
 */
class ControllerEntityReturnCheckTest extends CustomCheckTest {
    private static final String IMPORTS = "import org.springframework.web.bind.annotation.*; import app.entity.User; ";

    /**
     * 验证只有 REST 返回类型中的已识别实体触发提示
     */
    @Test
    void scan_restAndViewMethods_reportsRestEntitiesOnly() throws Exception {
        verify(ControllerEntityReturnCheck.class, IMPORTS + "@RestController class Example { @GetMapping Result<List<User>> list() { return null; } }", 1);
        verify(ControllerEntityReturnCheck.class, IMPORTS + "@RestController class Example { @GetMapping User[] list() { return null; } }", 1);
        verify(ControllerEntityReturnCheck.class, IMPORTS + "@RestController class Example { User helper() { return null; } @GetMapping long count() { return 0; } }", 0);
        verify(ControllerEntityReturnCheck.class, IMPORTS + "@org.springframework.stereotype.Service class Example { User getUser() { return null; } }", 0);
        verify(ControllerEntityReturnCheck.class, "@org.springframework.web.bind.annotation.RestController class Example { @org.springframework.web.bind.annotation.GetMapping app.entity.User get() { return null; } }", 1);
        verify(ControllerEntityReturnCheck.class, IMPORTS + "@RestController class Example { @GetMapping User get() { return null; } }", 0, "entityPattern", ".*\\.persistence\\..*");
    }

    /**
     * 验证常规响应模型、文件、空返回和流式响应可以通过
     */
    @Test
    void scan_nonEntityResponses_acceptsModelsAndInfrastructureTypes() throws Exception {
        verify(ControllerEntityReturnCheck.class, IMPORTS + "@RestController class Example { @GetMapping ResultSnapshot get() { return null; } @GetMapping void export() {} @GetMapping org.springframework.core.io.Resource file() { return null; } }", 0);
    }

    /**
     * 验证返回类型泛型实参与类型注解的区分
     */
    @Test
    void scan_typeArgumentsAndAnnotationValues_checksReturnTypesOnly() throws Exception {
        verify(ControllerEntityReturnCheck.class, IMPORTS
                + "@RestController class Example { @GetMapping List<@app.Doc(User.class) String> list() { return null; } }", 0);
        verify(ControllerEntityReturnCheck.class, IMPORTS
                + "@RestController class Example { @GetMapping List<? extends User[]> list() { return null; } }", 1);
        verify(ControllerEntityReturnCheck.class, IMPORTS
                + "@RestController class Example { @GetMapping app.entity.User[][] list() { return null; } }", 1);
    }
}
