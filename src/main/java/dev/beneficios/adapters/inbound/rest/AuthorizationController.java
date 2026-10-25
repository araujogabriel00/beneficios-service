package dev.beneficios.adapters.inbound.rest;

import dev.beneficios.adapters.inbound.rest.mapper.AuthorizationRestMapper;
import dev.beneficios.adapters.inbound.rest.request.AuthorizeRequest;
import dev.beneficios.adapters.inbound.rest.response.AuthorizationResponse;
import dev.beneficios.domain.model.authorization.AuthorizationId;
import dev.beneficios.ports.inbound.AuthorizePurchase;
import dev.beneficios.ports.inbound.ReversePurchase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authorizations")
@Tag(name = "Authorizations")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizePurchase authorizePurchase;
    private final ReversePurchase reversePurchase;
    private final AuthorizationRestMapper mapper;

    @PostMapping
    @Operation(summary = "Authorize a purchase; approved or denied, idempotent by Idempotency-Key")
    public AuthorizationResponse authorize(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                           @RequestBody AuthorizeRequest request) {
        if (request.walletId() == null || request.merchantType() == null) {
            throw new IllegalArgumentException("walletId and merchantType are required");
        }
        return mapper.toResponse(authorizePurchase.authorize(mapper.toCommand(idempotencyKey, request)));
    }

    @PostMapping("/{id}/reversal")
    @Operation(summary = "Reverse an approved authorization, returning the amount to its category")
    public AuthorizationResponse reverse(@PathVariable String id) {
        return mapper.toResponse(reversePurchase.reverse(AuthorizationId.of(id)));
    }
}
