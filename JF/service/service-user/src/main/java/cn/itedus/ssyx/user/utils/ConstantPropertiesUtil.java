package cn.itedus.ssyx.user.utils;

import cn.itedus.ssyx.common.utils.DesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * @author: Guanghao Wei
 * @date: 2023-06-16 17:09
 * @description: 获取配置工具类
 */
@Component
public class ConstantPropertiesUtil implements InitializingBean {

    @Value("${wx.open.app_id}")
    private String appId;
    @Value("${wx.open.app_secret}")
    private String appSecret;

    public static String WX_OPEN_APP_ID;
    public static String WX_OPEN_APP_SECRET;

    private static final Logger logger = LoggerFactory.getLogger(ConstantPropertiesUtil.class);
    private static final String DECRYPT_KEY = "abcdefgh";
    private static final Pattern WECHAT_APP_ID_PATTERN = Pattern.compile("^wx[0-9a-fA-F]{16}$");
    private static final Pattern WECHAT_APP_SECRET_PATTERN = Pattern.compile("^[0-9a-zA-Z]{32}$");

    @Override
    public void afterPropertiesSet() throws Exception {
        WX_OPEN_APP_ID = resolveValue(appId, WECHAT_APP_ID_PATTERN, "wx.open.app_id");
        WX_OPEN_APP_SECRET = resolveValue(appSecret, WECHAT_APP_SECRET_PATTERN, "wx.open.app_secret");
    }

    private static String resolveValue(String value, Pattern plaintextPattern, String keyName) {
        if (!StringUtils.hasText(value)) return "";

        final String trimmed = value.trim();
        if (plaintextPattern.matcher(trimmed).matches()) return trimmed;

        try {
            final String decrypted = DesUtils.decrypt(trimmed, DesUtils.Mode.DES, DECRYPT_KEY);
            if (StringUtils.hasText(decrypted)) return decrypted.trim();
        } catch (Exception e) {
            logger.warn("Failed to decrypt {}, falling back to raw value.", keyName);
        }

        return trimmed;
    }
}
