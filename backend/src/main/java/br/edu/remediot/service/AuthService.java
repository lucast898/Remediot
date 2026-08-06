package br.edu.remediot.service;

import br.edu.remediot.dto.AuthResponse;
import br.edu.remediot.dto.LoginRequest;
import br.edu.remediot.dto.RegisterCaregiverRequest;
import br.edu.remediot.entity.Role;
import br.edu.remediot.entity.User;
import br.edu.remediot.exception.EmailAlreadyInUseException;
import br.edu.remediot.repository.PatientRepository;
import br.edu.remediot.repository.UserRepository;
import br.edu.remediot.security.JwtService;
import br.edu.remediot.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PatientRepository patientRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registerCaregiver(RegisterCaregiverRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }
        User user = new User(request.fullName(), request.email(), passwordEncoder.encode(request.password()), Role.CUIDADOR);
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado: " + request.email()));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(new UserPrincipal(user));
        Long patientId = null;
        if (user.getRole() == Role.PACIENTE) {
            patientId = patientRepository.findByUserId(user.getId()).map(p -> p.getId()).orElse(null);
        }
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getRole(), patientId);
    }
}
