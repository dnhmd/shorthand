package com.shorthand.backend.infrastructure.adapter.inbound.web.v1;

import com.shorthand.backend.domain.model.Link;
import com.shorthand.backend.domain.port.inbound.CreateLinkUseCase;
import com.shorthand.backend.infrastructure.adapter.inbound.web.v1.dto.request.CreateLinkRequest;
import com.shorthand.backend.infrastructure.adapter.inbound.web.v1.dto.response.CreateLinkResponse;
import com.shorthand.backend.infrastructure.adapter.inbound.web.v1.mapper.LinkWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Link Management", description = "Create and manage short links")
@RestController
@RequestMapping("/api/v1/links")
public class LinkController {

    private final CreateLinkUseCase createLinkUseCase;
    private final LinkWebMapper linkWebMapper;

    public LinkController(CreateLinkUseCase createLinkUseCase, LinkWebMapper linkWebMapper) {
        this.createLinkUseCase = createLinkUseCase;
        this.linkWebMapper = linkWebMapper;
    }

    @Operation(summary = "Create Short Link", description = "Accepts a long URL and returns a Base62-encoded short code with an optional expiry")
    @PostMapping
    public ResponseEntity<CreateLinkResponse> createLink(@Valid @RequestBody CreateLinkRequest createLinkRequest) {
        Link link = createLinkUseCase.createLink(
                createLinkRequest.originalLink(),
                createLinkRequest.expiresInDays()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(linkWebMapper.toResponse(link));
    }
}
