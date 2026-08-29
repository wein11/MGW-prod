package com.mgwprod.users.config;

import com.mgwprod.users.repository.SessionRepository;
import com.mgwprod.users.security.SessionAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SessionRepository sessionRepository;

    public WebConfig(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SessionAuthInterceptor(sessionRepository))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }
}
