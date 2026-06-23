package com.shorthand.backend.infrastructure.adapter.inbound.web.v1;

import com.shorthand.backend.domain.port.inbound.RedirectLinkUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/")
public class RedirectController {

    private final RedirectLinkUseCase redirectLinkUseCase;

    public RedirectController(RedirectLinkUseCase redirectLinkUseCase) {
        this.redirectLinkUseCase = redirectLinkUseCase;
    }

    @GetMapping("{code}")
    public ResponseEntity<Void>redirect(@PathVariable("code") String code) {
        String link = redirectLinkUseCase.redirect(code);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(link)).build();
    }
}
