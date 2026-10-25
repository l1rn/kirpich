package org.kirpich.smart_profile.services;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.kirpich.smart_profile.dto.authorization.AuthorizationRequest;
import org.kirpich.smart_profile.enums.UserRole;
import org.kirpich.smart_profile.models.User;
import org.kirpich.smart_profile.repositories.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.CredentialNotFoundException;

@Service
@RequiredArgsConstructor
public class AuthorizationService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public void signUp(AuthorizationRequest r) throws BadRequestException, CredentialNotFoundException {
        if(userRepository.findByEmail(r.getEmail()).isPresent()){
            throw new CredentialNotFoundException("Пользователь с таким email уже существует");
        }
        if(r.getPassword().length() < 6){
            throw new BadRequestException("Пароль слишком маленький (нужен пароль > 6)");
        }

        User user = User.builder()
                .email(r.getEmail())
                .password(passwordEncoder.encode(r.getPassword()))
                .role(UserRole.CLIENT)
                .build();

        userRepository.save(user);
    }

    public String signIn(AuthorizationRequest r) throws CredentialNotFoundException{
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(r.getEmail(), r.getPassword())
            );
        } catch (BadCredentialsException | DisabledException e) {
            throw new CredentialNotFoundException("email или пароль неверны");
        }

        User user = userRepository.findByEmail(r.getEmail())
                .orElseThrow(() -> new CredentialNotFoundException("email или пароль неверны"));
        if(!passwordEncoder.matches(r.getPassword(), user.getPassword())){
            throw new CredentialNotFoundException("email или пароль неверны");
        }

        return jwtService.generateAccessToken(user);
    }
}
