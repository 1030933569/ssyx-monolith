package cn.itedus.ssyx.common.auth;

import cn.itedus.ssyx.common.constant.RedisConst;
import cn.itedus.ssyx.common.utils.JwtHelper;
import cn.itedus.ssyx.vo.user.UserLoginVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author: Guanghao Wei
 * @date: 2023-06-19 18:17
 * @description: 自定义拦截器
 */
public class UserLoginInterceptor implements HandlerInterceptor {
    private RedisTemplate redisTemplate;

    public UserLoginInterceptor(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    //前置拦截方法
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        AuthContextHolder.clear();
        this.initUserLoginVo(request);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        AuthContextHolder.clear();
    }

    private void initUserLoginVo(HttpServletRequest request) {
        //从请求头获取token
        String token = request.getHeader("token");
        if (!StringUtils.isEmpty(token)) {
            Long userId;
            try {
                userId = JwtHelper.getUserId(token);
            } catch (Exception e) {
                return;
            }
            if (userId == null) {
                return;
            }

            // token 能解析出 userId 时，先设置用户上下文，不依赖 Redis 当前是否命中
            AuthContextHolder.setUserId(userId);

            UserLoginVo userLoginVo = (UserLoginVo) redisTemplate.opsForValue().get(RedisConst.USER_LOGIN_KEY_PREFIX + userId);
            if (null != userLoginVo) {
                AuthContextHolder.setWareId(userLoginVo.getWareId());
                AuthContextHolder.setUserLoginVo(userLoginVo);
            }
        }
    }
}
