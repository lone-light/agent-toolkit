package lonelight.checks;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * 为自动测试提供 Checkstyle 配置加载、扫描和诊断查询
 *
 * @author lonelight
 */
public final class CheckTestSupport {
    private static final Pattern PACKAGE = Pattern.compile("\\bpackage\\s+([A-Za-z0-9_.]+)\\s*;");

    /**
     * 限制工具类实例化
     */
    private CheckTestSupport() { }

    /**
     * 按名称加载测试配置
     *
     * @param profile 不含扩展名的配置名称
     * @return 读取出的检查配置
     * @throws Exception 配置读取失败时
     */
    public static Configuration load(String profile) throws Exception {
        return load(profile, new Properties());
    }

    /**
     * 加载配置并解析外部属性
     *
     * @param profile 不含扩展名的配置名称
     * @param properties 配置使用的外部属性
     * @return 读取出的检查配置
     * @throws Exception 配置读取失败时
     */
    public static Configuration load(String profile, Properties properties) throws Exception {
        Path file = Path.of("config", profile + ".xml");
        return ConfigurationLoader.loadConfiguration(file.toString(), new PropertiesExpander(properties));
    }

    /**
     * 为单条规则创建测试配置
     *
     * @param name 检查模块名称
     * @param properties 交替排列的属性名和属性值
     * @return 配置好的检查模块
     */
    public static DefaultConfiguration rule(String name, String... properties) {
        DefaultConfiguration configuration = new DefaultConfiguration(name);
        for (int index = 0; index < properties.length; index += 2) {
            configuration.addProperty(properties[index], properties[index + 1]);
        }
        return configuration;
    }

    /**
     * 把一条规则包装成可独立运行的配置
     *
     * @param rule 待包装的规则配置
     * @param tree 是否需要放在 TreeWalker 下
     * @return 只有目标规则的根配置
     */
    public static Configuration isolate(Configuration rule, boolean tree) {
        DefaultConfiguration checker = rule("Checker");
        if (tree) {
            DefaultConfiguration walker = rule("TreeWalker");
            walker.addChild(rule);
            checker.addChild(walker);
        } else {
            checker.addChild(rule);
        }
        return checker;
    }

    /**
     * 使用测试类加载器扫描单个源码文件
     *
     * @param configuration 检查配置
     * @param file 待扫描的源码文件
     * @return 产生的诊断
     * @throws Exception 配置加载或扫描失败时
     */
    public static List<AuditEvent> scan(Configuration configuration, Path file) throws Exception {
        return scan(configuration, List.of(file), CheckTestSupport.class.getClassLoader());
    }

    /**
     * 用指定类加载器扫描源码并收集诊断
     *
     * @param configuration 检查配置
     * @param files 待扫描的源码文件列表
     * @param loader 加载检查模块的类加载器
     * @return 产生的诊断
     * @throws Exception 配置加载或扫描失败时
     */
    public static List<AuditEvent> scan(Configuration configuration, List<Path> files, ClassLoader loader) throws Exception {
        List<AuditEvent> result = new ArrayList<>();
        Checker checker = new Checker();
        checker.setModuleClassLoader(loader);
        checker.addListener(new AuditListener() {
            /**
             * 忽略扫描开始的通知
             *
             * @param event 扫描事件
             */
            public void auditStarted(AuditEvent event) { }
            /**
             * 忽略扫描结束的通知
             *
             * @param event 扫描事件
             */
            public void auditFinished(AuditEvent event) { }
            /**
             * 忽略文件扫描开始的通知
             *
             * @param event 文件扫描事件
             */
            public void fileStarted(AuditEvent event) { }
            /**
             * 忽略文件扫描结束的通知
             *
             * @param event 文件扫描事件
             */
            public void fileFinished(AuditEvent event) { }
            /**
             * 记录规则产生的诊断
             *
             * @param event 待记录的诊断事件
             */
            public void addError(AuditEvent event) { result.add(event); }
            /**
             * 将扫描异常转为测试失败
             *
             * @param event 异常关联的扫描事件
             * @param error 扫描异常
             */
            public void addException(AuditEvent event, Throwable error) {
                throw new AssertionError("Checkstyle failed for " + event.getFileName(), error);
            }
        });
        try {
            checker.configure(configuration);
            checker.process(files.stream().map(Path::toFile).toList());
        } finally {
            checker.destroy();
        }
        return result;
    }

    /**
     * 按声明的包名写入临时源码
     *
     * @param base 写入目录
     * @param filename Java 文件名
     * @param source 待写入的源码
     * @return 已写入的文件路径
     * @throws Exception 目录创建或文件写入失败时
     */
    public static Path write(Path base, String filename, String source) throws Exception {
        var matcher = PACKAGE.matcher(source);
        Path directory = matcher.find() ? base.resolve(matcher.group(1).replace('.', File.separatorChar)) : base;
        Files.createDirectories(directory);
        Path file = directory.resolve(filename);
        Files.writeString(file, source);
        return file;
    }

    /**
     * 按名称和可选标识收集已启用规则
     *
     * @param configuration 根检查配置
     * @return 按规则名称和标识索引的规则
     * @throws Exception 读取规则标识失败时
     */
    public static Map<String, LocatedRule> rules(Configuration configuration) throws Exception {
        Map<String, LocatedRule> result = new LinkedHashMap<>();
        collect(configuration, false, result);
        return result;
    }

    /**
     * 提取当前配置层级中显式设置等级的规则
     *
     * @param parent 当前层级的父配置
     * @param tree 当前层级是否属于 TreeWalker
     * @param result 用于累积规则的映射
     * @throws Exception 读取规则标识失败时
     */
    private static void collect(Configuration parent, boolean tree, Map<String, LocatedRule> result) throws Exception {
        for (Configuration child : parent.getChildren()) {
            if (child.getName().equals("TreeWalker")) {
                collect(child, true, result);
            } else if (List.of(child.getPropertyNames()).contains("severity")) {
                String name = child.getName();
                if (List.of(child.getPropertyNames()).contains("id")) {
                    name += "#" + child.getProperty("id");
                }
                if (result.put(name, new LocatedRule(child, tree)) != null) {
                    throw new AssertionError("Duplicate rule " + name);
                }
            }
        }
    }

    /**
     * 按检查名和可选标识匹配诊断
     *
     * @param event 待匹配的诊断事件
     * @param key 由检查名和可选模块标识组成的键
     * @return 诊断符合该键时为 true
     */
    public static boolean matches(AuditEvent event, String key) {
        String[] parts = key.split("#", 2);
        String simple = JavaSource.simpleName(parts[0]);
        if (!simple.endsWith("Check")) {
            simple += "Check";
        }
        return event.getSourceName().endsWith("." + simple)
                && (parts.length == 1 || parts[1].equals(event.getModuleId()));
    }

    /**
     * 保存规则配置及其在配置树中的位置
     *
     * @param configuration 待验证的检查配置
     * @param tree 检查是否位于 TreeWalker 下
     * @author lonelight
     */
    public record LocatedRule(Configuration configuration, boolean tree) {
        /**
         * 构建当前规则的独立运行配置
         *
         * @return 仅包含当前规则的根配置
         */
        public Configuration isolated() {
            return isolate(configuration, tree);
        }
    }
}
