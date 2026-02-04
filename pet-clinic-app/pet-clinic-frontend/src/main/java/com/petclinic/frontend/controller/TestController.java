package com.petclinic.frontend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller for debugging POST request issues
 */
@Controller
@RequestMapping("/test")
public class TestController {

    @GetMapping("/get")
    public ResponseEntity<String> testGet() {
        return ResponseEntity.ok("GET request works");
    }

    @PostMapping("/post")
    public ResponseEntity<String> testPost(@RequestParam(required = false) String data) {
        return ResponseEntity.ok("POST request works with data: " + data);
    }

    @PostMapping("/post-json")
    public ResponseEntity<String> testPostJson(@RequestBody(required = false) String data) {
        return ResponseEntity.ok("POST JSON request works with data: " + data);
    }

    @GetMapping("/auth-info")
    public ResponseEntity<Map<String, Object>> getAuthInfo() {
        Map<String, Object> authInfo = new HashMap<>();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null) {
            authInfo.put("authenticated", auth.isAuthenticated());
            authInfo.put("username", auth.getName());
            authInfo.put("authorities", auth.getAuthorities().toString());
            authInfo.put("principal", auth.getPrincipal().toString());
        } else {
            authInfo.put("authenticated", false);
            authInfo.put("message", "No authentication found");
        }
        
        return ResponseEntity.ok(authInfo);
    }
}