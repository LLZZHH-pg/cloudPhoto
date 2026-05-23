package com.LAB.study.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.LAB.study.dto.JwtUserDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class UserContextHolder {

    private static final String HEADER_USER_INFO = "X-User-Info";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private UserContextHolder() {
    }

    public static Integer getCurrentUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new SecurityException("无法获取请求上下文");
        }

        HttpServletRequest request = attrs.getRequest();
        String userJson = request.getHeader(HEADER_USER_INFO);

        if (!StringUtils.hasText(userJson)) {
            throw new SecurityException("用户未认证，请登录后再试");
        }

        try {
            // 如果网关以后做了 URLEncoder 编码，这里可以解一下码；如果网关没做，这一步解普通JSON也不受影响
            String decodedUserJson = URLDecoder.decode(userJson, StandardCharsets.UTF_8.name());
            JwtUserDTO jwtUser = OBJECT_MAPPER.readValue(decodedUserJson, JwtUserDTO.class);
            if (jwtUser.getUid() == null) {
                throw new SecurityException("Token 中缺少用户ID");
            }
            return jwtUser.getUid();
        } catch (Exception e) {
            throw new SecurityException("解析用户信息失败", e);
        }
    }
}