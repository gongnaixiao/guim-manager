package com.sinoiift.comm.filter;

import com.sinoiift.comm.Const;
import com.sinoiift.domain.User;
import com.sinoiift.repository.UserRepository;
import com.sinoiift.utils.Des3EncryptionUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SecurityFilter implements Filter {

    protected Logger logger = Logger.getLogger(this.getClass());
    private static Set<String> GreenUrlSet = new HashSet<String>();

    @Autowired
    private UserRepository userRepository;

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub
        GreenUrlSet.add("/login");
        GreenUrlSet.add("/register");
        GreenUrlSet.add("/index");
        GreenUrlSet.add("/forgotPassword");
        GreenUrlSet.add("/newPassword");
        GreenUrlSet.add("/tool");
    }

    @Override
    public void doFilter(ServletRequest srequest, ServletResponse sresponse, FilterChain filterChain)
            throws IOException, ServletException {
        // TODO Auto-generated method stub
        HttpServletRequest request = (HttpServletRequest) srequest;
        String uri = request.getRequestURI();
        String html = "<script type=\"text/javascript\">window.location.href=\"_BP_login\"</script>";
        if (request.getSession().getAttribute(Const.LOGIN_SESSION_KEY) == null) {
            Cookie[] cookies = request.getCookies();
            if (containsSuffix(uri) || GreenUrlSet.contains(uri) || containsKey(uri)) {
                logger.debug("don't check  url , " + request.getRequestURI());
                filterChain.doFilter(srequest, sresponse);
                return;
            } else if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    Cookie cookie = cookies[i];
                    if (cookie.getName().equals(Const.LOGIN_SESSION_KEY)) {
                        String value = getUserId(cookie.getValue());

                        if (StringUtils.isNotBlank(value)) {
                            if (userRepository == null) {
                                BeanFactory factory = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());
                                userRepository = (UserRepository) factory.getBean("userRepository");
                            }
                            Long userId = Long.parseLong(value);
                            User user = userRepository.findOne(userId);

                            logger.info("userId :" + user.getId());
                            request.getSession().setAttribute(Const.LOGIN_SESSION_KEY, user);

                            html = "<script type=\"text/javascript\">window.location.href=\"_BP_\"</script>";
                        }
                    }
                }
            }
            html = html.replace("_BP_", Const.BASE_PATH);
            sresponse.getWriter().write(html);
        } else {
            filterChain.doFilter(srequest, sresponse);
        }
    }


    /**
     * @param url
     * @return
     * @author neo
     * @date 2016-5-4
     */
    private boolean containsSuffix(String url) {
        if (url.endsWith(".js")
                || url.endsWith(".css")
                || url.endsWith(".jpg")
                || url.endsWith(".gif")
                || url.endsWith(".png")
                || url.endsWith(".html")
                || url.endsWith(".eot")
                || url.endsWith(".svg")
                || url.endsWith(".ttf")
                || url.endsWith(".woff")
                || url.endsWith(".ico")
                || url.endsWith(".woff2")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param url
     * @return
     * @author neo
     * @date 2016-5-4
     */
    private boolean containsKey(String url) {

        if (url.contains("/media/")
                || url.contains("/login") || url.contains("/user/login")
                || url.contains("/forgotPassword") || url.contains("/user/sendForgotPasswordEmail")
                || url.startsWith("/user/")) {
            return true;
        } else {
            return false;
        }
    }


    @Override
    public void destroy() {
        // TODO Auto-generated method stub
    }

    public String codeToString(String str) {
        String strString = str;
        try {
            byte tempB[] = strString.getBytes("ISO-8859-1");
            strString = new String(tempB);
            return strString;
        } catch (Exception e) {
            return strString;
        }
    }

    public String getUserId(String value) {
        try {
            String userId = Des3EncryptionUtil.decode(Const.DES3_KEY, value);
            userId = userId.substring(0, userId.indexOf(Const.PASSWORD_KEY));
            return userId;
        } catch (Exception e) {
            logger.error("解析cookie异常：", e);
        }
        return null;
    }
}