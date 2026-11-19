// 检查：MagicNumber, MultipleStringLiterals, LayerDependencyCheck, IllegalImport#reflectionCopyImports
// 检查：BeanUtilsCallCheck, LoggingPlaceholderCheck

package project;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import static org.springframework.beans.BeanUtils.copyProperties;

@Service
class FrameworkCases {
    static final int DEFAULT_SIZE = 42;

    void conversion(Object source, Object target, HttpServletRequest httpServletRequest) {
        BeanUtils.copyProperties(source, target);
        copyProperties(source, target);
        org.springframework.beans.BeanUtils.copyProperties(source, target);
    }

    void logs(Logger logger, Marker marker, String orderNumber) {
        logger.info("订单编号：" + orderNumber);
        logger.info(marker, "标记订单：" + orderNumber);
        logger.info("订单编号：{}", orderNumber);
        logger.info("订单" + "信息");
    }

    int size(boolean custom) {
        System.out.println("同一提示");
        System.out.println("同一提示");
        return custom ? 42 : DEFAULT_SIZE;
    }
}
