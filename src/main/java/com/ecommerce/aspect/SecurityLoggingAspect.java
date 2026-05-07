package com.ecommerce.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ecommerce.service.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class SecurityLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(SecurityLoggingAspect.class);
    private final SecurityAuditService auditService;

    public SecurityLoggingAspect(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    @Before("execution(* com.ecommerce.controller.AuthController.login(..))")
    public void logLoginAttempt(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String username = "unknown";
        if (args.length > 0) {
            // Assuming the first argument is LoginRequest
            try {
                java.lang.reflect.Field emailField = args[0].getClass().getDeclaredField("email");
                emailField.setAccessible(true);
                username = (String) emailField.get(args[0]);
            } catch (Exception e) {
                username = "error-extracting";
            }
        }
        
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ip = request.getRemoteAddr();
        
        logger.info("Login attempt for user: {} from IP: {}", username, ip);
        auditService.recordEndpointHit("/api/auth/login");
    }

    @AfterReturning("execution(* com.ecommerce.controller.AuthController.login(..))")
    public void logLoginSuccess(JoinPoint joinPoint) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Login successful for user: {}", username);
        auditService.recordLoginSuccess(username);
    }

    @AfterThrowing(pointcut = "execution(* com.ecommerce.controller.AuthController.login(..))", throwing = "ex")
    public void logLoginFailure(JoinPoint joinPoint, Exception ex) {
        logger.warn("Login failed: {}", ex.getMessage());
        auditService.recordLoginFailure("system-wide"); // Simplification
    }

    @Before("execution(* com.ecommerce.controller.*.*(..)) && @annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public void logAccessAttempt(JoinPoint joinPoint) {
        String method = joinPoint.getSignature().getName();
        String user = SecurityContextHolder.getContext().getAuthentication() != null ? 
                       SecurityContextHolder.getContext().getAuthentication().getName() : "anonymous";
        logger.debug("Access attempt by user: {} to method: {}", user, method);
        auditService.recordEndpointHit(method);
    }
}
