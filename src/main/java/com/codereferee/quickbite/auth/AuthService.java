package com.codereferee.quickbite.auth;

import com.codereferee.quickbite.auth.AuthDtos.LoginRequest;
import com.codereferee.quickbite.auth.AuthDtos.SignupRequest;
import com.codereferee.quickbite.auth.AuthDtos.TokenResponse;
import com.codereferee.quickbite.auth.AuthDtos.UserView;
import com.codereferee.quickbite.common.BusinessException;
import com.codereferee.quickbite.user.UserAccount;
import com.codereferee.quickbite.user.UserAccountRepository;
import com.codereferee.quickbite.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email is already registered");
        }
        UserAccount user = new UserAccount();
        user.setEmail(request.email().trim().toLowerCase());
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.CUSTOMER);
        users.save(user);
        return tokenResponse(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(request.email())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> invalidCredentials());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return tokenResponse(user);
    }

    private TokenResponse tokenResponse(UserAccount user) {
        var issued = tokens.issue(user);
        return new TokenResponse(issued.value(), "Bearer", issued.expiresAt(),
                new UserView(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name()));
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
    }
}
