package com.mgwprod.users.config;

import com.mgwprod.users.repository.SessionRepository;
import com.mgwprod.users.security.SessionAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Clase de configuración: acá es donde SessionAuthInterceptor queda enganchado al
// pipeline de requests. Sin este registro, la clase del interceptor existiría pero
// nunca se ejecutaría.
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SessionRepository sessionRepository;

    public WebConfig(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    // Spring llama a este método al arrancar para registrar nuestros interceptores.
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Aplica a todo endpoint /api/** EXCEPTO /api/auth/** (register/login), porque
        // no tendría sentido exigir ya tener un token de sesión para poder loguearse.
        registry.addInterceptor(new SessionAuthInterceptor(sessionRepository))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }
}
