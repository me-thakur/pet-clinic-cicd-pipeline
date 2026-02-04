package com.petclinic.frontend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for the Pet Clinic Frontend
 * 
 * Handles exceptions that occur in the frontend application,
 * particularly those related to backend service communication.
 * Provides user-friendly error pages and messages.
 * 
 * Validates: Requirements All (Error Handling)
 */
@ControllerAdvice
public class FrontendExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(FrontendExceptionHandler.class);

    /**
     * Handle WebClient response exceptions (backend API errors)
     */
    @ExceptionHandler(WebClientResponseException.class)
    public String handleWebClientResponseException(WebClientResponseException ex, Model model, RedirectAttributes redirectAttributes) {
        logger.error("Backend API error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        
        String errorMessage;
        String redirectUrl = "/error";
        
        switch (ex.getStatusCode().value()) {
            case 400:
                errorMessage = "Invalid request. Please check your input and try again.";
                redirectUrl = "redirect:/";
                break;
            case 401:
                errorMessage = "Authentication required. Please log in.";
                redirectUrl = "redirect:/login";
                break;
            case 403:
                errorMessage = "Access denied. You don't have permission to perform this action.";
                break;
            case 404:
                errorMessage = "The requested resource was not found.";
                break;
            case 409:
                errorMessage = "Conflict occurred. The resource may already exist or be in use.";
                redirectUrl = "redirect:/";
                break;
            case 422:
                errorMessage = "Validation failed. Please check your input.";
                redirectUrl = "redirect:/";
                break;
            case 500:
                errorMessage = "Internal server error. Please try again later.";
                break;
            default:
                errorMessage = "An unexpected error occurred. Please try again.";
        }
        
        if (redirectUrl.startsWith("redirect:")) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            redirectAttributes.addFlashAttribute("errorDetails", ex.getMessage());
            return redirectUrl;
        } else {
            model.addAttribute("errorMessage", errorMessage);
            model.addAttribute("errorDetails", ex.getMessage());
            model.addAttribute("statusCode", ex.getStatusCode().value());
            return "error/api-error";
        }
    }

    /**
     * Handle backend service unavailable
     */
    @ExceptionHandler(org.springframework.web.reactive.function.client.WebClientRequestException.class)
    public String handleWebClientRequestException(org.springframework.web.reactive.function.client.WebClientRequestException ex, 
                                                 Model model, RedirectAttributes redirectAttributes) {
        logger.error("Backend service unavailable: {}", ex.getMessage());
        
        String errorMessage = "Backend service is currently unavailable. Please try again later.";
        
        redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        redirectAttributes.addFlashAttribute("errorDetails", "Unable to connect to the backend service");
        
        return "redirect:/error/service-unavailable";
    }

    /**
     * Handle timeout exceptions
     */
    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public String handleTimeoutException(java.util.concurrent.TimeoutException ex, Model model, RedirectAttributes redirectAttributes) {
        logger.error("Request timeout: {}", ex.getMessage());
        
        String errorMessage = "Request timed out. The service may be busy. Please try again.";
        
        redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        redirectAttributes.addFlashAttribute("errorDetails", ex.getMessage());
        
        return "redirect:/";
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public String handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException ex, 
                                          Model model, RedirectAttributes redirectAttributes) {
        logger.error("Validation error: {}", ex.getMessage());
        
        StringBuilder errorMessage = new StringBuilder("Validation failed: ");
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errorMessage.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ")
        );
        
        redirectAttributes.addFlashAttribute("errorMessage", errorMessage.toString());
        redirectAttributes.addFlashAttribute("validationErrors", ex.getBindingResult().getFieldErrors());
        
        return "redirect:/";
    }

    /**
     * Handle access denied exceptions (Spring Security)
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex, Model model) {
        logger.error("Access denied: {}", ex.getMessage());
        
        model.addAttribute("errorMessage", "Access denied. You don't have permission to access this resource.");
        model.addAttribute("errorDetails", "Please contact an administrator if you believe you should have access.");
        model.addAttribute("statusCode", 403);
        
        return "error/access-denied";
    }

    /**
     * Handle generic runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, Model model) {
        logger.error("Runtime exception: {}", ex.getMessage(), ex);
        
        model.addAttribute("errorMessage", "An unexpected error occurred");
        model.addAttribute("errorDetails", ex.getMessage());
        
        return "error/generic-error";
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model) {
        logger.error("Unexpected exception: {}", ex.getMessage(), ex);
        
        model.addAttribute("errorMessage", "An unexpected error occurred");
        model.addAttribute("errorDetails", "Please contact support if this problem persists");
        model.addAttribute("statusCode", 500);
        
        return "error/generic-error";
    }
}