package dev.beneficios.adapters.inbound.rest.mapper;

import dev.beneficios.adapters.inbound.rest.request.AuthorizeRequest;
import dev.beneficios.adapters.inbound.rest.response.AuthorizationResponse;
import dev.beneficios.domain.model.authorization.Authorization;
import dev.beneficios.ports.inbound.AuthorizePurchase.AuthorizePurchaseCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ValueObjectConverters.class)
public interface AuthorizationRestMapper {

    @Mapping(target = "key", source = "idempotencyKey")
    @Mapping(target = "walletId", source = "request.walletId")
    @Mapping(target = "merchantType", source = "request.merchantType")
    @Mapping(target = "amount", source = "request.amountCents")
    AuthorizePurchaseCommand toCommand(String idempotencyKey, AuthorizeRequest request);

    @Mapping(target = "id", expression = "java(authorization.id().value())")
    @Mapping(target = "walletId", expression = "java(authorization.walletId().value())")
    @Mapping(target = "merchantType", expression = "java(authorization.merchantType())")
    @Mapping(target = "amountCents", expression = "java(authorization.amount().cents())")
    @Mapping(target = "status", expression = "java(authorization.status())")
    @Mapping(target = "category", expression = "java(authorization.category().orElse(null))")
    @Mapping(target = "denialReason", expression = "java(authorization.denialReason().orElse(null))")
    AuthorizationResponse toResponse(Authorization authorization);
}
