package dev.beneficios.application.service;

import dev.beneficios.application.exception.NotFoundException;
import dev.beneficios.ports.inbound.CreditBenefit;
import dev.beneficios.ports.inbound.GetWallet;
import dev.beneficios.ports.inbound.OpenWallet;
import dev.beneficios.ports.outbound.DomainEventPublisher;
import dev.beneficios.domain.model.wallet.Wallet;
import dev.beneficios.domain.model.wallet.WalletId;
import dev.beneficios.ports.outbound.WalletRepository;

public class WalletService implements OpenWallet, CreditBenefit, GetWallet {

    private final WalletRepository wallets;
    private final DomainEventPublisher events;
    private final WalletLocks locks;

    public WalletService(WalletRepository wallets, DomainEventPublisher events, WalletLocks locks) {
        this.wallets = wallets;
        this.events = events;
        this.locks = locks;
    }

    @Override
    public Wallet open(OpenWalletCommand command) {
        Wallet wallet = Wallet.open(command.beneficiaryName());
        wallets.save(wallet);
        events.publish(wallet.pullEvents());
        return wallet;
    }

    @Override
    public Wallet credit(CreditBenefitCommand command) {
        synchronized (locks.of(command.walletId())) {
            Wallet wallet = get(command.walletId());
            wallet.credit(command.key(), command.category(), command.amount());
            wallets.save(wallet);
            events.publish(wallet.pullEvents());
            return wallet;
        }
    }

    @Override
    public Wallet get(WalletId walletId) {
        return wallets.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet not found: " + walletId.value()));
    }
}
