package com.flashsale.ordersystem.auth.service;

import com.flashsale.ordersystem.auth.dto.RegisterRequest;
import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.user.domain.User;
import com.flashsale.ordersystem.user.infrastructure.UserRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final Keycloak keycloak;
    private static final String APP_REALM = "flash-sale";
    @Value("${keycloak.realm}")
    private String realm;
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        UserRepresentation user = new UserRepresentation();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setEnabled(true);
        user.setEmailVerified(true);
        Response response = keycloak.realm(APP_REALM).users().create(user);

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

            keycloak.realm(APP_REALM)
                    .users()
                    .get(keycloakId)
                    .resetPassword(credential);

            var realmResource = keycloak.realm(APP_REALM);
            var userResource = realmResource.users().get(keycloakId);

            RoleRepresentation userRole = realmResource.roles().get("USER").toRepresentation();
            userResource.roles().realmLevel().add(List.of(userRole));

            User dbUser = new User();
            dbUser.setKeycloakId(keycloakId);
            dbUser.setEmail(request.email());

            userRepository.save(dbUser);

        } catch (Exception e) {
            keycloak.realm(APP_REALM).users().delete(keycloakId);
            throw e;
        }
    }
}
