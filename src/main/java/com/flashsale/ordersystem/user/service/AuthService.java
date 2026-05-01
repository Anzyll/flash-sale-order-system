package com.flashsale.ordersystem.user.service;

import com.flashsale.ordersystem.user.presentation.dto.RegisterRequest;
import com.flashsale.ordersystem.shared.exception.CustomException;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final Keycloak keycloak;
    private final UserService userService;
    @Value("${keycloak.app-realm}")
    private String realm;
    public void register(RegisterRequest request) {
        if (userService.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        UserRepresentation user = new UserRepresentation();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setEnabled(true);
        user.setEmailVerified(true);
        Response response = keycloak.realm(realm).users().create(user);

        if (response.getStatus() == 409) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (response.getStatus() != 201) {
            throw new RuntimeException("Keycloak user creation failed");
        }

        String location = response.getLocation().getPath();
        String keycloakId = location.substring(location.lastIndexOf("/") + 1);

        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.password());
            credential.setTemporary(false);

            keycloak.realm(realm)
                    .users()
                    .get(keycloakId)
                    .resetPassword(credential);

            var realmResource = keycloak.realm(realm);
            var userResource = realmResource.users().get(keycloakId);

            RoleRepresentation userRole = realmResource.roles().get("USER").toRepresentation();
            userResource.roles().realmLevel().add(List.of(userRole));

            userService.createUser(keycloakId,request.email());

        } catch (Exception e) {
            keycloak.realm(realm).users().delete(keycloakId);
            throw e;
        }
    }
}
