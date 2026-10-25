package dev.beneficios.application.service;

import dev.beneficios.application.exception.NotFoundException;
import dev.beneficios.application.exception.RequestInProgressException;
import dev.beneficios.domain.exception.IdempotencyConflictException;
import dev.beneficios.domain.model.authorization.Authorization;
import dev.beneficios.domain.model.authorization.AuthorizationId;
import dev.beneficios.domain.model.authorization.DenialReason;
import dev.beneficios.domain.model.authorization.IdempotencyKey;
import dev.beneficios.domain.model.wallet.BenefitCategory;
import dev.beneficios.domain.model.wallet.Wallet;
import dev.beneficios.ports.inbound.AuthorizePurchase;
import dev.beneficios.ports.inbound.ReversePurchase;
import dev.beneficios.ports.outbound.AntifraudPort;
import dev.beneficios.ports.outbound.AuthorizationRepository;
import dev.beneficios.ports.outbound.DomainEventPublisher;
import dev.beneficios.ports.outbound.WalletRepository;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PurchaseService implements AuthorizePurchase, ReversePurchase {

    private final WalletRepository wallets;
    private final AuthorizationRepository authorizations;
    private final AntifraudPort antifraud;
    private final DomainEventPublisher events;
    private final WalletLocks locks;
    private final Set<IdempotencyKey> inFlight = ConcurrentHashMap.newKeySet();

    @Override
    public Authorization authorize(AuthorizePurchaseCommand command) {
        Optional<Authorization> previous = replay(command);
        if (previous.isPresent()) {
            return previous.get();
        }
        if (!inFlight.add(command.key())) {
            throw new RequestInProgressException("A request with this idempotency key is being processed");
        }
        try {
            // Another request may have finished between the first lookup and acquiring the key.
            return replay(command).orElseGet(() -> process(command));
        } finally {
            inFlight.remove(command.key());
        }
    }

    private Optional<Authorization> replay(AuthorizePurchaseCommand command) {
        return authorizations.findByIdempotencyKey(command.key()).map(existing -> {
            if (!existing.sameRequest(command.walletId(), command.merchantType(), command.amount())) {
                throw new IdempotencyConflictException("Idempotency key reused with different content");
            }
            return existing;
        });
    }

    private Authorization process(AuthorizePurchaseCommand command) {
        synchronized (locks.of(command.walletId())) {
            Wallet wallet = wallets.findById(command.walletId())
                    .orElseThrow(() -> new NotFoundException("Wallet not found: " + command.walletId().value()));

            // RN-05: merchant, then balance, then antifraud.
            Optional<BenefitCategory> category = command.merchantType().category();
            if (category.isEmpty()) {
                return deny(command, DenialReason.MERCHANT_NOT_ACCEPTED);
            }
            if (!wallet.canPay(category.get(), command.amount())) {
                return deny(command, DenialReason.INSUFFICIENT_BALANCE);
            }
            if (!antifraud.approves(command.walletId(), command.amount())) {
                return deny(command, DenialReason.FRAUD_SUSPECTED);
            }

            wallet.debit(category.get(), command.amount());
            Authorization authorization = Authorization.approved(command.key(), command.walletId(),
                    command.merchantType(), command.amount(), category.get());
            wallets.save(wallet);
            authorizations.save(authorization);
            events.publish(wallet.pullEvents());
            return authorization;
        }
    }

    private Authorization deny(AuthorizePurchaseCommand command, DenialReason reason) {
        Authorization authorization = Authorization.denied(command.key(), command.walletId(),
                command.merchantType(), command.amount(), reason);
        return authorizations.save(authorization);
    }

    @Override
    public Authorization reverse(AuthorizationId authorizationId) {
        Authorization authorization = authorizations.findById(authorizationId)
                .orElseThrow(() -> new NotFoundException("Authorization not found: " + authorizationId.value()));
        synchronized (locks.of(authorization.walletId())) {
            authorization.reverse();
            Wallet wallet = wallets.findById(authorization.walletId())
                    .orElseThrow(() -> new NotFoundException("Wallet not found: " + authorization.walletId().value()));
            wallet.refund(authorization.category().orElseThrow(), authorization.amount());
            wallets.save(wallet);
            authorizations.save(authorization);
            events.publish(wallet.pullEvents());
            return authorization;
        }
    }
}
