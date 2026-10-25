package dev.beneficios.adapters.inbound.rest;

import dev.beneficios.adapters.inbound.rest.response.AuthorizationResponse;
import dev.beneficios.ports.inbound.AuthorizePurchase;
import dev.beneficios.ports.inbound.AuthorizePurchase.AuthorizePurchaseCommand;
import dev.beneficios.ports.inbound.ReversePurchase;
import dev.beneficios.domain.model.authorization.AuthorizationId;
import dev.beneficios.domain.model.authorization.IdempotencyKey;
import dev.beneficios.domain.model.authorization.MerchantType;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.WalletId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authorizations")
@Tag(name = "Authorizations")
public class AuthorizationController {

    public record AuthorizeRequest(String walletId, MerchantType merchantType, long amountCents) {
    }

    private final AuthorizePurchase authorizePurchase;
    private final ReversePurchase reversePurchase;

    public AuthorizationController(AuthorizePurchase authorizePurchase, ReversePurchase reversePurchase) {
        this.authorizePurchase = authorizePurchase;
        this.reversePurchase = reversePurchase;
    }

    @PostMapping
    @Operation(summary = "Authorize a purchase; approved or denied, idempotent by Idempotency-Key")
    public AuthorizationResponse authorize(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                           @RequestBody AuthorizeRequest request) {
        if (request.walletId() == null || request.merchantType() == null) {
            throw new IllegalArgumentException("walletId and merchantType are required");
        }
        return AuthorizationResponse.from(authorizePurchase.authorize(new AuthorizePurchaseCommand(
                new IdempotencyKey(idempotencyKey), WalletId.of(request.walletId()), request.merchantType(),
                Money.ofCents(request.amountCents()))));
    }

    @PostMapping("/{id}/reversal")
    @Operation(summary = "Reverse an approved authorization, returning the amount to its category")
    public AuthorizationResponse reverse(@PathVariable String id) {
        return AuthorizationResponse.from(reversePurchase.reverse(AuthorizationId.of(id)));
    }
}
