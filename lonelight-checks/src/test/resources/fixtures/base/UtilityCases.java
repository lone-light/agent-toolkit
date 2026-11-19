// 检查：HideUtilityClassConstructor
// 反例：只提供静态方法却暴露了默认构造器；下方的 ProperUtility 使用私有构造器
package base;

public final class UtilityCases {
    public static int count() {
        return 0;
    }

    static final class ProperUtility {
        private ProperUtility() { }

        public static int count() {
            return 0;
        }
    }
}
