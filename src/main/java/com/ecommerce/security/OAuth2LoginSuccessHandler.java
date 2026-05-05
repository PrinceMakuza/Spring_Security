package com.ecommerce.security;

import com.ecommerce.model.User;
import com.ecommerce.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setRole("CUSTOMER");
            newUser.setPassword(UUID.randomUUID().toString()); // Dummy password for OAuth users
            newUser.setLocation("Google OAuth");
            userRepository.save(newUser);
        } else {
            User existingUser = userOptional.get();
            existingUser.setName(name); // Sync name from Google
            userRepository.save(existingUser);
        }

        // Populate UserContext for JavaFX app (sharing JVM if running in same process)
        User user = userRepository.findByEmail(email).orElseThrow();
        com.ecommerce.util.UserContext.setCurrentUserId(user.getUserId());
        com.ecommerce.util.UserContext.setCurrentUserName(user.getName());
        com.ecommerce.util.UserContext.setCurrentUserEmail(user.getEmail());
        com.ecommerce.util.UserContext.setCurrentUserRole(user.getRole());
        com.ecommerce.util.UserContext.setCurrentUserLocation(user.getLocation());

        // Signal to external JavaFX process via shared file
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("shared-auth.tmp"), email);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Redirect to a frontend success page
        getRedirectStrategy().sendRedirect(request, response, "/auth-success.html");
    }
}
