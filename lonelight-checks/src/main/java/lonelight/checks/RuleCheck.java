package lonelight.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;

import java.nio.file.Path;

/**
 * 为自定义语法树规则统一消息资源和节点订阅
 *
 * @author lonelight
 */
public abstract class RuleCheck extends AbstractCheck {
    /**
     * 为当前源码建立可查找同包声明的名称索引
     *
     * @param root 源码语法树根节点
     * @return 当前文件的名称索引
     */
    protected final JavaSource indexSource(DetailAST root) {
        return new JavaSource(root, Path.of(getFilePath()));
    }

    /**
     * 指定自定义规则的消息资源
     *
     * @return 消息资源包名
     */
    @Override
    protected String getMessageBundle() {
        return "lonelight.checks.messages";
    }

    /**
     * 按规则的默认节点类别限制可监听范围
     *
     * @return 规则接受的节点类别
     */
    @Override
    public int[] getAcceptableTokens() {
        return getDefaultTokens();
    }

    /**
     * 要求规则始终监听默认节点类别
     *
     * @return 规则必需的节点类别
     */
    @Override
    public int[] getRequiredTokens() {
        return getDefaultTokens();
    }
}
