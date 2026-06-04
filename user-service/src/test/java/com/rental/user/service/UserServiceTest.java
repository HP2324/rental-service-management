package com.rental.user.service;

import com.rental.shared.exception.ResourceNotFoundException;
import com.rental.user.dto.*;
import com.rental.user.model.Role;
import com.rental.user.model.User;
import com.rental.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks UserService userService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private User tenantUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        tenantUser = User.builder()
                .id("user-1")
                .email("tenant@example.com")
                .password("hashed-password")
                .firstName("Alice")
                .lastName("Smith")
                .role(Role.TENANT)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        adminUser = User.builder()
                .id("admin-1")
                .email("admin@example.com")
                .password("hashed-password")
                .firstName("Bob")
                .lastName("Admin")
                .role(Role.ADMIN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("returns auth response with JWT when email is new")
        void success() {
            RegisterRequest request = new RegisterRequest();
            request.setFirstName("Alice");
            request.setLastName("Smith");
            request.setEmail("tenant@example.com");
            request.setPassword("password123");
            request.setRole(Role.TENANT);

            when(userRepository.existsByEmail("tenant@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
            when(userRepository.save(any(User.class))).thenReturn(tenantUser);
            when(jwtService.generateToken(eq("tenant@example.com"), anyMap())).thenReturn("jwt-token");

            AuthResponse result = userService.register(request);

            assertThat(result.getToken()).isEqualTo("jwt-token");
            assertThat(result.getEmail()).isEqualTo("tenant@example.com");
            assertThat(result.getRole()).isEqualTo("TENANT");
            assertThat(result.getTokenType()).isEqualTo("Bearer");
            assertThat(result.getUserId()).isEqualTo("user-1"); // ensures save() return value is used

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when email is already registered")
        void duplicateEmail() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("tenant@example.com");
            request.setPassword("password123");

            when(userRepository.existsByEmail("tenant@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already registered");

            verify(userRepository, never()).save(any());
        }
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns auth response when credentials are valid")
        void success() {
            LoginRequest request = new LoginRequest();
            request.setEmail("tenant@example.com");
            request.setPassword("password123");

            when(userRepository.findByEmail("tenant@example.com")).thenReturn(Optional.of(tenantUser));
            when(jwtService.generateToken(eq("tenant@example.com"), anyMap())).thenReturn("jwt-token");

            AuthResponse result = userService.login(request);

            assertThat(result.getToken()).isEqualTo("jwt-token");
            assertThat(result.getUserId()).isEqualTo("user-1");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("propagates BadCredentialsException from AuthenticationManager")
        void invalidCredentials() {
            LoginRequest request = new LoginRequest();
            request.setEmail("tenant@example.com");
            request.setPassword("wrong-password");

            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user is not in DB after auth")
        void userNotFound() {
            LoginRequest request = new LoginRequest();
            request.setEmail("ghost@example.com");
            request.setPassword("password123");

            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── getMe ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMe")
    class GetMe {

        @Test
        @DisplayName("returns mapped UserResponse for authenticated user")
        void success() {
            when(userRepository.findByEmail("tenant@example.com")).thenReturn(Optional.of(tenantUser));

            UserResponse result = userService.getMe("tenant@example.com");

            assertThat(result.getId()).isEqualTo("user-1");
            assertThat(result.getEmail()).isEqualTo("tenant@example.com");
            assertThat(result.getFirstName()).isEqualTo("Alice");
            assertThat(result.getRole()).isEqualTo("TENANT");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when email has no user")
        void notFound() {
            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMe("nobody@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("updates only non-null fields and returns updated response")
        void success() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Alicia");
            // lastName left null — should not change

            when(userRepository.findByEmail("tenant@example.com")).thenReturn(Optional.of(tenantUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse result = userService.updateProfile("tenant@example.com", request);

            assertThat(result.getFirstName()).isEqualTo("Alicia");
            assertThat(result.getLastName()).isEqualTo("Smith"); // unchanged
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user does not exist")
        void notFound() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateProfile("ghost@example.com", new UpdateProfileRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── getUserById ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("returns UserResponse when ID exists")
        void success() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(tenantUser));

            UserResponse result = userService.getUserById("user-1");

            assertThat(result.getId()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown ID")
        void notFound() {
            when(userRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById("bad-id"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── getAllUsers ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("returns mapped list of all users")
        void success() {
            when(userRepository.findAll()).thenReturn(List.of(tenantUser, adminUser));

            List<UserResponse> result = userService.getAllUsers();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(UserResponse::getEmail)
                    .containsExactlyInAnyOrder("tenant@example.com", "admin@example.com");
        }

        @Test
        @DisplayName("returns empty list when no users exist")
        void empty() {
            when(userRepository.findAll()).thenReturn(List.of());

            assertThat(userService.getAllUsers()).isEmpty();
        }
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("deletes user by ID when found")
        void success() {
            when(userRepository.existsById("user-1")).thenReturn(true);

            userService.deleteUser("user-1");

            verify(userRepository).deleteById("user-1");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when ID does not exist")
        void notFound() {
            when(userRepository.existsById("bad-id")).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser("bad-id"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).deleteById(any());
        }
    }
}
