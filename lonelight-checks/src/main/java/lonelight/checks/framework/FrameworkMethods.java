package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;

import java.util.Set;

/**
 * 从源码注解识别 JUnit 方法和 Spring HTTP 接口
 *
 * @author lonelight
 */
public final class FrameworkMethods {
    static final String WEB = "org.springframework.web.bind.annotation.";
    private static final String RESPONSE_BODY = WEB + "ResponseBody";
    private static final Set<String> TEST_ANNOTATIONS = Set.of("org.junit.Test", "org.junit.jupiter.api.Test",
            "org.junit.jupiter.params.ParameterizedTest", "org.junit.jupiter.api.RepeatedTest",
            "org.junit.jupiter.api.TestFactory", "org.junit.jupiter.api.TestTemplate");
    private static final Set<String> MAPPINGS = Set.of("RequestMapping", "GetMapping", "PostMapping",
            "PutMapping", "DeleteMapping", "PatchMapping");

    private static final Set<String> INTERNAL_ANNOTATIONS = Set.of(WEB + "RequestAttribute", WEB + "SessionAttribute",
            "org.springframework.security.core.annotation.AuthenticationPrincipal",
            "org.springframework.security.core.annotation.CurrentSecurityContext");
    private static final Set<String> INTERNAL_TYPES = Set.of(
            "jakarta.servlet.ServletRequest", "jakarta.servlet.ServletResponse",
            "jakarta.servlet.http.HttpServletRequest", "jakarta.servlet.http.HttpServletResponse",
            "jakarta.servlet.http.HttpSession", "javax.servlet.ServletRequest", "javax.servlet.ServletResponse",
            "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse",
            "javax.servlet.http.HttpSession", "java.security.Principal",
            "org.springframework.security.core.Authentication", "org.springframework.web.context.request.WebRequest",
            "org.springframework.web.context.request.NativeWebRequest",
            "org.springframework.web.server.ServerWebExchange", "org.springframework.validation.BindingResult",
            "org.springframework.validation.Errors", "org.springframework.ui.Model", "org.springframework.ui.ModelMap");

    /**
     * 限制工具类实例化
     */
    private FrameworkMethods() {
    }

    /**
     * 判断方法是否使用已知的 JUnit 测试注解
     *
     * @param javaSource      当前文件的名称索引
     * @param methodDetailAST 待检查的方法声明
     * @return 含有 JUnit 测试注解时为 true
     */
    public static boolean isJUnitTest(JavaSource javaSource, DetailAST methodDetailAST) {
        return TEST_ANNOTATIONS.stream().anyMatch(annotation -> javaSource.hasAnnotation(methodDetailAST, annotation));
    }

    /**
     * 判断方法是否声明 Spring HTTP 映射
     *
     * @param javaSource      当前文件的名称索引
     * @param methodDetailAST 待检查的方法声明
     * @return 含有 HTTP 映射注解时为 true
     */
    static boolean hasHttpMapping(JavaSource javaSource, DetailAST methodDetailAST) {
        return MAPPINGS.stream().anyMatch(name -> javaSource.hasKnownAnnotation(methodDetailAST, WEB + name));
    }

    /**
     * 识别不需要对外说明的框架参数
     *
     * @param parameterDetailAST 待检查的参数声明
     * @param javaSource         当前文件的名称索引
     * @return 参数由框架提供时为 true
     */
    static boolean isInternalParameter(DetailAST parameterDetailAST, JavaSource javaSource) {
        String name = JavaSource.typeName(parameterDetailAST.findFirstToken(TokenTypes.TYPE));
        return INTERNAL_ANNOTATIONS.stream().anyMatch(key -> javaSource.hasKnownAnnotation(parameterDetailAST, key))
                || INTERNAL_TYPES.stream().anyMatch(key -> javaSource.isKnownType(name, key));
    }

    /**
     * 识别直接提供响应数据的 Spring HTTP 方法
     *
     * @param javaSource      当前文件的名称索引
     * @param methodDetailAST 待识别的方法声明
     * @return 方法是 REST 接口时为 true
     */
    static boolean isRestEndpoint(JavaSource javaSource, DetailAST methodDetailAST) {
        var owner = JavaSource.enclosingType(methodDetailAST);
        if (owner == null || !hasHttpMapping(javaSource, methodDetailAST)) {
            return false;
        }
        if (javaSource.hasKnownAnnotation(owner, WEB + "RestController")) {
            return true;
        }
        String returns = JavaSource.typeName(methodDetailAST.findFirstToken(TokenTypes.TYPE));
        return javaSource.hasKnownAnnotation(owner, "org.springframework.stereotype.Controller")
                && (javaSource.hasKnownAnnotation(owner, RESPONSE_BODY)
                || javaSource.hasKnownAnnotation(methodDetailAST, RESPONSE_BODY)
                || javaSource.isKnownType(returns, "org.springframework.http.ResponseEntity")
                || javaSource.isKnownType(returns, "org.springframework.http.HttpEntity"));
    }
}
