package edu.software.project.backend.config;

import edu.software.project.backend.security.AuthInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final edu.software.project.backend.security.CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebConfig(
            AuthInterceptor authInterceptor,
            edu.software.project.backend.security.CurrentUserArgumentResolver currentUserArgumentResolver
    ) {
        this.authInterceptor = authInterceptor;
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .excludePathPatterns("/api/auth/register", "/api/auth/login", "/actuator/health", "/error");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
