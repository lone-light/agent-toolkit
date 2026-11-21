package lonelight.checks.framework;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证 Spring 字段注入和两类构造器注入的约束
 *
 * @author lonelight
 */
class ConstructorInjectionCheckTest extends CustomCheckTest {
    /**
     * 验证字段注入和构造器赋值同时出现时优先报告字段注入
     */
    @Test
    void scan_fieldAndConstructorInjection_reportsFieldOnce() throws Exception {
        verify(ConstructorInjectionCheck.class, """
                import jakarta.annotation.Resource;
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.stereotype.Service;
                @Service class Example {
                    @Resource private Client client;
                    @Autowired private Cache cache;
                    Example(Client client, Cache cache) {
                        this.client = client;
                        this.cache = cache;
                    }
                }
                """, 2);
    }

    /**
     * 验证直接字段注入和非 final 的 Lombok 构造器参数
     */
    @Test
    void scan_injectedFields_flagsFieldInjectionAndMutableDependencies() throws Exception {
        verify(ConstructorInjectionCheck.class, """
                import jakarta.annotation.Resource;
                import jakarta.inject.Inject;
                import lombok.NonNull;
                import lombok.RequiredArgsConstructor;
                import org.springframework.beans.factory.annotation.*;
                import org.springframework.stereotype.Service;
                @Service @RequiredArgsConstructor class Example {
                    private final Client client;
                    @NonNull private Cache cache;
                    @Autowired private Gateway gateway;
                    @Resource private Store store;
                    @Inject private Sender sender;
                    @Value("1") private int timeout;
                    private String state;
                    private final int LIMIT = 1;
                }
                """, 4);
    }

    /**
     * 验证构造器直接赋值可识别并区别配置参数与同名局部变量
     */
    @Test
    void scan_constructorAssignments_requiresPrivateFinalFields() throws Exception {
        verify(ConstructorInjectionCheck.class, """
                import org.springframework.stereotype.Service;
                import org.springframework.beans.factory.annotation.Value;
                @Service class Example {
                    private Client client;
                    private final Cache cache;
                    private int timeout;
                    Example(Client client, Cache cache, @Value("1") int timeout) {
                        this.client = client;
                        this.cache = cache;
                        this.timeout = timeout;
                    }
                }
                class Ordinary {
                    private Client client;
                    Ordinary(Client client) { this.client = client; }
                }
                """, 1);
    }

    /**
     * 验证明确标注的多构造器及局部变量遮蔽
     */
    @Test
    void scan_multipleConstructors_checksExplicitInjection() throws Exception {
        verify(ConstructorInjectionCheck.class, """
                import org.springframework.beans.factory.annotation.Autowired;
                class Example {
                    private Client client;
                    private Cache cache;
                    private Store store;
                    Example() {}
                    @Autowired Example(Client supplied, Cache source, Store other) {
                        Client client = supplied;
                        client = supplied;
                        cache = source;
                        other.store = other;
                    }
                }
                """, 1);
    }

    /**
     * 验证内部类构造器的限定接收者不占用注入参数
     */
    @Test
    void scan_qualifiedReceiver_checksOnlyInjectedFields() throws Exception {
        verify(ConstructorInjectionCheck.class, """
                class Example {
                    class Service {
                        String label;
                        @jakarta.inject.Inject Service(Example Example.this, String label) { this.label = label; }
                    }
                }
                """, 1);
    }
}
