package com.tg.Config;

import com.tg.Service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra a rota, sem parâmetros, corpo, credenciais ou dados pessoais da requisição. */
@Configuration
@RequiredArgsConstructor
public class AuditoriaWebConfig implements WebMvcConfigurer {
  private final AuditoriaService auditoria;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(
            new HandlerInterceptor() {
              @Override
              public void afterCompletion(
                  HttpServletRequest request,
                  HttpServletResponse response,
                  Object handler,
                  Exception exception) {
                Object pattern =
                    request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                if (pattern != null && !"/api/health".equals(pattern.toString())) {
                  auditoria.registrar(
                      "HTTP",
                      request.getMethod() + "_" + response.getStatus(),
                      pattern.toString(),
                      "BACKEND_HTTP");
                }
              }
            })
        .addPathPatterns("/api/**");
  }
}
