package com.shorthand.backend.infrastructure.adapter.inbound.web.v1;

import com.shorthand.backend.domain.port.inbound.RedirectLinkUseCase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/")
public class RedirectController {

    private final RedirectLinkUseCase redirectLinkUseCase;

    public RedirectController(RedirectLinkUseCase redirectLinkUseCase) {
        this.redirectLinkUseCase = redirectLinkUseCase;
    }

    @GetMapping("{code}")
    public ResponseEntity<Void>redirect(HttpServletRequest request, @PathVariable("code") String code) {
        Instant now = Instant.now();
        String ipAddress = getClientIpAddress(request);
        String userAgent = getUserAgent(request);
        String link = redirectLinkUseCase.redirect(code, ipAddress, userAgent, now);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(link)).build();
    }

    private static String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        } else {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }

    private static String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "Unknown";
        }
        return userAgent;
    }
}
