package dev.beneficios.ports.inbound;

import dev.beneficios.domain.model.authorization.Authorization;
import dev.beneficios.domain.model.authorization.AuthorizationId;

public interface ReversePurchase {

    Authorization reverse(AuthorizationId authorizationId);
}
