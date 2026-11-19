package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.JavaParser;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lonelight.checks.JavaSource;

/**
 * 按类型引用查找同一源码根目录下的模型
 *
 * @author lonelight
 */
final class ApiSourceModels {
    private final Map<Path, Map<String, Model>> files = new HashMap<>();
    private final Path sourceRoot;
    private final Map<String, Model> currentModels;

    /**
     * 为当前文件建立接口模型索引
     *
     * @param file 当前文件路径
     * @param root 当前文件的语法树根节点
     * @param source 当前文件的名称索引
     */
    ApiSourceModels(Path file, DetailAST root, JavaSource source) {
        sourceRoot = sourceRoot(file, source.packageName());
        currentModels = declarations(root, source);
        files.put(file, currentModels);
    }

    /**
     * 按当前声明的上下文查找模型源码
     *
     * @param written 源码中的类型名
     * @param source 引用所在文件的名称索引
     * @param context 类型引用所在的声明
     * @return 解析到的模型，缺失或有歧义时为 null
     */
    Model find(String written, JavaSource source, DetailAST context) {
        if (written.indexOf('.') < 0) {
            for (DetailAST owner = JavaSource.enclosingType(context); owner != null; owner = JavaSource.enclosingType(owner)) {
                String ownerName = qualifiedName(source.packageName(), owner);
                Model nested = findCanonical(ownerName + "." + written);
                if (nested != null) {
                    return nested;
                }
            }
        }
        List<String> candidates = source.sourceTypeNames(written);
        Model primary = findCanonical(candidates.getFirst());
        if (primary != null) {
            return primary;
        }
        Model match = null;
        for (String candidate : candidates.subList(1, candidates.size())) {
            Model model = findCanonical(candidate);
            if (model != null) {
                if (match != null && match != model) {
                    return null;
                }
                match = model;
            }
        }
        return match;
    }

    /**
     * 按全限定名查找当前文件或同一源码根下的模型
     *
     * @param name 模型的全限定名
     * @return 查找到的模型，缺失时为 null
     */
    private Model findCanonical(String name) {
        Model current = currentModels.get(name);
        if (current != null || sourceRoot == null) {
            return current;
        }
        for (String prefix = name; !prefix.isEmpty(); ) {
            Path file = sourceRoot.resolve(prefix.replace('.', '/') + ".java");
            Model model = files.computeIfAbsent(file, ApiSourceModels::read).get(name);
            if (model != null) {
                return model;
            }
            int dot = prefix.lastIndexOf('.');
            prefix = dot < 0 ? "" : prefix.substring(0, dot);
        }
        return null;
    }

    /**
     * 解析已有源码文件中的模型声明
     *
     * @param file 候选源码文件路径
     * @return 解析到的模型，文件不可读或无法解析时为空
     */
    private static Map<String, Model> read(Path file) {
        if (Files.isRegularFile(file)) {
            try {
                DetailAST root = JavaParser.parseFile(file.toFile(), JavaParser.Options.WITHOUT_COMMENTS);
                return declarations(root, new JavaSource(root));
            } catch (IOException | CheckstyleException ignored) {
                // 源码不可读或无法解析时跳过该文件
            }
        }
        return Map.of();
    }

    /**
     * 从文件路径和包名推算源码根目录
     *
     * @param file 当前源码文件路径
     * @param packageName 文件声明的包名
     * @return 源码根目录，路径与包名不匹配时为 null
     */
    private static Path sourceRoot(Path file, String packageName) {
        Path directory = file.getParent();
        if (packageName.isEmpty()) {
            return directory;
        }
        Path packagePath = Path.of(packageName.replace('.', '/'));
        if (directory == null || !directory.endsWith(packagePath)) {
            return null;
        }
        for (int index = 0; index < packagePath.getNameCount(); index++) {
            directory = directory.getParent();
        }
        return directory;
    }

    /**
     * 索引源码中的顶层和嵌套模型
     *
     * @param root 当前文件的语法树根节点
     * @param source 当前文件的名称索引
     * @return 按全限定名索引的模型声明
     */
    private static Map<String, Model> declarations(DetailAST root, JavaSource source) {
        Map<String, Model> result = new LinkedHashMap<>();
        for (DetailAST node = root; node != null; node = node.getNextSibling()) {
            if (node.getType() == TokenTypes.COMPILATION_UNIT) {
                addDeclarations(node.getFirstChild(), source, result);
            } else {
                addDeclaration(node, source, result);
            }
        }
        return result;
    }

    /**
     * 收集同级节点及其嵌套类型
     *
     * @param node 首个同级节点
     * @param source 当前文件的名称索引
     * @param result 模型索引
     */
    private static void addDeclarations(DetailAST node, JavaSource source, Map<String, Model> result) {
        for (DetailAST child = node; child != null; child = child.getNextSibling()) {
            addDeclaration(child, source, result);
        }
    }

    /**
     * 将当前类型及其嵌套声明加入模型索引
     *
     * @param node 待识别的声明节点
     * @param source 当前文件的名称索引
     * @param result 模型索引
     */
    private static void addDeclaration(DetailAST node, JavaSource source, Map<String, Model> result) {
        switch (node.getType()) {
            case TokenTypes.CLASS_DEF, TokenTypes.RECORD_DEF, TokenTypes.ENUM_DEF, TokenTypes.INTERFACE_DEF -> {
                String name = qualifiedName(source.packageName(), node);
                result.put(name, new Model(name, node, source));
                var members = node.findFirstToken(TokenTypes.OBJBLOCK);
                if (members != null) {
                    addDeclarations(members.getFirstChild(), source, result);
                }
            }
            default -> { }
        }
    }

    /**
     * 拼接包名和各层外部类型名
     *
     * @param pkg 包名
     * @param declaration 类型声明节点
     * @return 当前类型的全限定名
     */
    private static String qualifiedName(String pkg, DetailAST declaration) {
        DetailAST owner = JavaSource.enclosingType(declaration);
        String prefix = owner == null ? pkg : qualifiedName(pkg, owner);
        String name = JavaSource.declarationName(declaration).getText();
        return prefix.isEmpty() ? name : prefix + "." + name;
    }

    /**
     * 遍历类型引用及其泛型实参
     *
     * @param node 类型子树根节点，为 null 时跳过
     * @param consumer 接收源码类型名的回调
     */
    static void typeNames(DetailAST node, Consumer<String> consumer) {
        if (node == null || node.getType() == TokenTypes.ANNOTATION) {
            return;
        }
        if (node.getType() == TokenTypes.DOT) {
            String name = FullIdent.createFullIdent(node).getText();
            int bracket = name.indexOf('[');
            consumer.accept(bracket < 0 ? name : name.substring(0, bracket));
            for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getType() != TokenTypes.IDENT) {
                    typeArguments(child, consumer);
                }
            }
        } else if (node.getType() == TokenTypes.IDENT) {
            consumer.accept(node.getText());
        } else {
            for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                typeNames(child, consumer);
            }
        }
    }

    /**
     * 读取限定类型各层的泛型实参
     *
     * @param node 限定类型子树节点
     * @param consumer 接收源码类型名的回调
     */
    private static void typeArguments(DetailAST node, Consumer<String> consumer) {
        if (node.getType() == TokenTypes.TYPE_ARGUMENTS) {
            typeNames(node, consumer);
        } else if (node.getType() == TokenTypes.DOT) {
            for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                typeArguments(child, consumer);
            }
        }
    }

    /**
     * 保存模型声明和所在文件的名称索引
     *
     * @param name 模型的完整类型名称
     * @param tree 模型的类型声明节点
     * @param source 模型所在文件的名称解析信息
     * @author lonelight
     */
    record Model(String name, DetailAST tree, JavaSource source) { }
}
