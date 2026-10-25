package dev.beneficios.adapters.inbound.rest;

import dev.beneficios.adapters.inbound.rest.mapper.WalletRestMapper;
import dev.beneficios.adapters.inbound.rest.request.CreditRequest;
import dev.beneficios.adapters.inbound.rest.request.OpenWalletRequest;
import dev.beneficios.adapters.inbound.rest.response.WalletResponse;
import dev.beneficios.domain.model.wallet.WalletId;
import dev.beneficios.ports.inbound.CreditBenefit;
import dev.beneficios.ports.inbound.GetWallet;
import dev.beneficios.ports.inbound.OpenWallet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallets")
@Tag(name = "Wallets")
@RequiredArgsConstructor
public class WalletController {

    private final OpenWallet openWallet;
    private final CreditBenefit creditBenefit;
    private final GetWallet getWallet;
    private final WalletRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a wallet with zero balance in every category")
    public WalletResponse open(@RequestBody OpenWalletRequest request) {
        return mapper.toResponse(openWallet.open(mapper.toCommand(request)));
    }

    @PostMapping("/{id}/credits")
    @Operation(summary = "Credit a benefit category; idempotent by Idempotency-Key")
    public WalletResponse credit(@PathVariable String id,
                                 @RequestHeader("Idempotency-Key") String idempotencyKey,
                                 @RequestBody CreditRequest request) {
        if (request.category() == null) {
            throw new IllegalArgumentException("category is required");
        }
        return mapper.toResponse(creditBenefit.credit(mapper.toCommand(id, idempotencyKey, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get the balances of a wallet")
    public WalletResponse get(@PathVariable String id) {
        return mapper.toResponse(getWallet.get(WalletId.of(id)));
    }
}
