package lonelight.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;

/**
 * 为自定义语法树规则统一消息资源和节点订阅
 *
 * @author lonelight
 */
public abstract class RuleCheck extends AbstractCheck {
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
