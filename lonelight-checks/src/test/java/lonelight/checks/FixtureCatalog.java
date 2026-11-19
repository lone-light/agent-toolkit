package lonelight.checks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从主题样例的文件头建立规则与预期违规文件的索引
 *
 * @param files       可扫描的主题样例和配套源码
 * @param markedFiles 以规则短名和可选标识索引的样例文件
 * @author lonelight
 */
public record FixtureCatalog(List<Path> files, Map<String, List<Path>> markedFiles) {
    private static final Path FIXTURES = Path.of("src/test/resources/fixtures");
    private static final String RULE_MARKER = "// 检查：";

    /**
     * 定位一组自动测试样例的目录
     *
     * @param group 样例分组名称
     * @return 样例目录的绝对路径
     */
    public static Path directory(String group) {
        return FIXTURES.resolve(group).toAbsolutePath();
    }

    /**
     * 加载指定配置的样例源码及文件头中的规则标记
     *
     * @param group 样例分组名称
     * @return 该分组的样例索引
     * @throws IOException 样例目录或文件读取失败时
     */
    public static FixtureCatalog load(String group) throws IOException {
        return load(directory(group));
    }

    /**
     * 加载指定源码目录的样例及文件头标记
     *
     * @param folderPath 样例源码根目录
     * @return 按规则索引的样例
     * @throws IOException 样例读取失败时
     */
    public static FixtureCatalog load(Path folderPath) throws IOException {
        try (var paths = Files.walk(folderPath.toAbsolutePath())) {
            var files = paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java"))
                    .sorted().toList();
            Map<String, List<Path>> marks = new LinkedHashMap<>();
            for (Path filePath : files) {
                try (var lines = Files.lines(filePath)) {
                    lines.takeWhile(line -> line.isBlank() || line.startsWith("//"))
                            .filter(line -> line.startsWith(RULE_MARKER))
                            .flatMap(line -> java.util.Arrays.stream(line.substring(RULE_MARKER.length()).split(", ")))
                            .forEach(rule -> marks.computeIfAbsent(rule, ignored -> new ArrayList<>()).add(filePath));
                }
            }
            marks.replaceAll((rule, examples) -> List.copyOf(examples));
            return new FixtureCatalog(files, Map.copyOf(marks));
        }
    }

    /**
     * 查找标明会触发指定规则的文件
     *
     * @param rule 检查模块名称和可选配置标识
     * @return 标明该规则的绝对路径
     */
    public List<Path> filesFor(String rule) {
        return markedFiles.getOrDefault(JavaSource.simpleName(rule), List.of());
    }
}
