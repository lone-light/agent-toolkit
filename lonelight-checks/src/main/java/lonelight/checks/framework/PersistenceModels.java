package lonelight.checks.framework;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 识别项目中的持久化实体声明
 *
 * @author lonelight
 */
final class PersistenceModels {
    private static final String ENTITY_PACKAGE_SEGMENT = ".entity.";
    static final String PACKAGE_PATTERN = ".*" + Pattern.quote(ENTITY_PACKAGE_SEGMENT) + ".*";
    private static final Set<String> ANNOTATIONS = Set.of("jakarta.persistence.Entity", "javax.persistence.Entity",
            "com.baomidou.mybatisplus.annotation.TableName", "org.springframework.data.relational.core.mapping.Table",
            "org.springframework.data.mongodb.core.mapping.Document");

    /**
     * 限制工具类实例化
     */
    private PersistenceModels() {
    }

    /**
     * 判断类型是否按包名或框架注解声明为持久化实体
     *
     * @param model 待识别的源码模型
     * @return 属于持久化实体时为 true
     */
    static boolean isEntity(ApiSourceModels.Model model) {
        return model.name().contains(ENTITY_PACKAGE_SEGMENT)
                || ANNOTATIONS.stream().anyMatch(name -> model.source().hasKnownAnnotation(model.tree(), name));
    }
}
