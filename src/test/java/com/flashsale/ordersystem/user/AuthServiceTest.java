package com.flashsale.ordersystem.user;

import com.flashsale.ordersystem.user.domain.User;
import com.flashsale.ordersystem.user.repository.UserRepository;
import com.flashsale.ordersystem.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;


    @Test
    void shouldCreateUserSuccessfully() {
        User user = new User();
        user.setKeycloakId("user-1");
        user.setEmail("test@gmail.com");

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        User savedUser =
                userService.createUser(
                        "user-1",
                        "test@gmail.com"
                );
        assertEquals(
                "test@gmail.com",
                savedUser.getEmail()
        );
        verify(userRepository)
                .save(any(User.class));
    }
}