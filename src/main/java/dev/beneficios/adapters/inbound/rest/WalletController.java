package dev.beneficios.adapters.inbound.rest;

import dev.beneficios.adapters.inbound.rest.response.WalletResponse;
import dev.beneficios.ports.inbound.CreditBenefit;
import dev.beneficios.ports.inbound.CreditBenefit.CreditBenefitCommand;
import dev.beneficios.ports.inbound.GetWallet;
import dev.beneficios.ports.inbound.OpenWallet;
import dev.beneficios.ports.inbound.OpenWallet.OpenWalletCommand;
import dev.beneficios.domain.model.authorization.IdempotencyKey;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.BenefitCategory;
import dev.beneficios.domain.model.wallet.Wallet;
import dev.beneficios.domain.model.wallet.WalletId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
public class WalletController {

    public record OpenWalletRequest(String beneficiaryName) {
    }

    public record CreditRequest(BenefitCategory category, long amountCents) {
    }

    private final OpenWallet openWallet;
    private final CreditBenefit creditBenefit;
    private final GetWallet getWallet;

    public WalletController(OpenWallet openWallet, CreditBenefit creditBenefit, GetWallet getWallet) {
        this.openWallet = openWallet;
        this.creditBenefit = creditBenefit;
        this.getWallet = getWallet;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a wallet with zero balance in every category")
    public WalletResponse open(@RequestBody OpenWalletRequest request) {
        Wallet wallet = openWallet.open(new OpenWalletCommand(request.beneficiaryName()));
        return WalletResponse.from(wallet);
    }

    @PostMapping("/{id}/credits")
    @Operation(summary = "Credit a benefit category; idempotent by Idempotency-Key")
    public WalletResponse credit(@PathVariable String id,
                                 @RequestHeader("Idempotency-Key") String idempotencyKey,
                                 @RequestBody CreditRequest request) {
        if (request.category() == null) {
            throw new IllegalArgumentException("category is required");
        }
        Wallet wallet = creditBenefit.credit(new CreditBenefitCommand(WalletId.of(id),
                new IdempotencyKey(idempotencyKey), request.category(), Money.ofCents(request.amountCents())));
        return WalletResponse.from(wallet);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get the balances of a wallet")
    public WalletResponse get(@PathVariable String id) {
        return WalletResponse.from(getWallet.get(WalletId.of(id)));
    }
}
