package com.ecommerce.ui.screen;

import com.ecommerce.service.AuthService;
import com.ecommerce.util.SpringContextBridge;
import com.ecommerce.util.UserContext;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import com.ecommerce.ui.FrontendApplication;
import java.util.regex.Pattern;
import javafx.application.Platform;
import java.util.Timer;
import java.util.TimerTask;

/**
 * LoginController manages authentications and handles UI interactions for the login screen.
 * Refactored to use FXML for a clean separation of concerns.
 */
public class LoginController {
    private final AuthService authService = SpringContextBridge.getBean(AuthService.class);
    private Runnable onLoginSuccess;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s]+$");

    @FXML private Label title;
    @FXML private Label subtitle;
    @FXML private TextField nameField;
    @FXML private Label nameLabel;
    @FXML private TextField emailField;
    @FXML private TextField locationField;
    @FXML private Label locationLabel;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisible;
    @FXML private Button togglePasswordBtn;
    @FXML private Label confirmLabel;
    @FXML private HBox confirmPasswordBox;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisible;
    @FXML private Button toggleConfirmBtn;
    @FXML private Button mainBtn;
    @FXML private Button googleBtn;
    @FXML private Label statusLabel;
    @FXML private Hyperlink toggleLink;
    @FXML private HBox demoBox;

    private boolean isRegisterMode = false;
    private boolean passwordShown = false;
    private boolean confirmPasswordShown = false;

    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    @FXML
    public void initialize() {
        // Synchronize password fields
        passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());
        confirmPasswordVisible.textProperty().bindBidirectional(confirmPasswordField.textProperty());
    }

    @FXML
    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        statusLabel.setText("");
        
        if (isRegisterMode) {
            title.setText("Create Account");
            subtitle.setText("Join our high-performance shopping system");
            mainBtn.setText("Sign Up");
            toggleLink.setText("Already have an account? Sign In");
            setRegistrationFieldsVisible(true);
        } else {
            title.setText("Welcome Back");
            subtitle.setText("Please sign in to continue");
            mainBtn.setText("Sign In");
            toggleLink.setText("Don't have an account? Create one");
            setRegistrationFieldsVisible(false);
        }
    }

    private void setRegistrationFieldsVisible(boolean visible) {
        nameField.setVisible(visible);
        nameField.setManaged(visible);
        nameLabel.setVisible(visible);
        nameLabel.setManaged(visible);
        confirmLabel.setVisible(visible);
        confirmLabel.setManaged(visible);
        confirmPasswordBox.setVisible(visible);
        confirmPasswordBox.setManaged(visible);
        locationLabel.setVisible(visible);
        locationLabel.setManaged(visible);
        locationField.setVisible(visible);
        locationField.setManaged(visible);
        demoBox.setVisible(!visible);
        demoBox.setManaged(!visible);
    }

    @FXML
    private void handleGoogleLogin() {
        try {
            statusLabel.setText("Opening browser for Google Sign-In...");
            statusLabel.setTextFill(Color.web("#3498db"));
            
            FrontendApplication.getInstance().getHostServices().showDocument("http://localhost:8080/oauth2/authorization/google");
            
            // Poll for login success (checks both local UserContext and shared file)
            Timer timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    boolean success = false;
                    
                    // Case 1: Same process (UserContext populated directly)
                    if (UserContext.isLoggedIn()) {
                        success = true;
                    } 
                    // Case 2: Different process (External backend wrote to file)
                    else {
                        java.nio.file.Path sharedPath = java.nio.file.Path.of("shared-auth.tmp");
                        if (java.nio.file.Files.exists(sharedPath)) {
                            try {
                                String email = java.nio.file.Files.readString(sharedPath).trim();
                                java.nio.file.Files.deleteIfExists(sharedPath);
                                
                                com.ecommerce.service.UserService userService = SpringContextBridge.getBean(com.ecommerce.service.UserService.class);
                                java.util.Optional<com.ecommerce.model.User> userOpt = userService.getUserByEmail(email);
                                
                                if (userOpt.isPresent()) {
                                    com.ecommerce.model.User user = userOpt.get();
                                    UserContext.setCurrentUserId(user.getUserId());
                                    UserContext.setCurrentUserName(user.getName());
                                    UserContext.setCurrentUserEmail(user.getEmail());
                                    UserContext.setCurrentUserRole(user.getRole());
                                    UserContext.setCurrentUserLocation(user.getLocation());
                                    success = true;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (success) {
                        Platform.runLater(() -> {
                            System.out.println("Google Login detected!");
                            if (onLoginSuccess != null) onLoginSuccess.run();
                        });
                        timer.cancel();
                    }
                }
            }, 2000, 1000);
            
        } catch (Exception e) {
            showError("Could not open browser: " + e.getMessage());
        }
    }

    @FXML
    private void handleAction() {
        String email = emailField.getText().trim().toLowerCase();
        String pass = passwordField.getText();

        if (isRegisterMode) {
            handleRegister(email, pass);
        } else {
            handleLogin(email, pass);
        }
    }

    private void handleLogin(String email, String pass) {
        if (!validateLoginInputs(email, pass)) return;

        try {
            authService.login(email, pass);
            
            // CLEAR ROLE BASED DIRECTION
            if (UserContext.isAdmin()) {
                System.out.println("Admin logged in. Redirecting to Management Dashboard.");
            } else if (UserContext.isCustomer()) {
                System.out.println("Customer logged in. Redirecting to Catalog.");
            }
            
            if (onLoginSuccess != null) onLoginSuccess.run();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleRegister(String email, String pass) {
        String name = nameField.getText().trim();
        String location = locationField.getText().trim();
        String confirmPass = confirmPasswordField.getText();

        if (!validateRegistrationInputs(name, email, location, pass, confirmPass)) return;

        try {
            authService.register(name, email, pass, "CUSTOMER", location);
            statusLabel.setText("Registration successful! You can now sign in.");
            statusLabel.setTextFill(Color.web("#38b86c"));
            toggleMode();
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("duplicate")) {
                showError("An account with this email already exists.");
            } else {
                showError("Registration failed: " + ex.getMessage());
            }
        }
    }

    private boolean validateLoginInputs(String email, String pass) {
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            showError("Please enter a valid email address.");
            return false;
        }
        if (pass.isEmpty()) {
            showError("Please enter your password.");
            return false;
        }
        return true;
    }

    private boolean validateRegistrationInputs(String name, String email, String location, String pass, String confirmPass) {
        if (name.length() < 2 || !NAME_PATTERN.matcher(name).matches()) {
            showError("Name can only contain letters and spaces.");
            return false;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Please enter a valid email.");
            return false;
        }
        if (location.isEmpty()) {
            showError("Please enter your location.");
            return false;
        }
        if (pass.length() < 6) {
            showError("Password must be at least 6 characters.");
            return false;
        }
        if (!pass.equals(confirmPass)) {
            showError("Passwords do not match.");
            return false;
        }
        return true;
    }

    @FXML
    private void togglePasswordVisibility() {
        passwordShown = !passwordShown;
        passwordField.setVisible(!passwordShown);
        passwordField.setManaged(!passwordShown);
        passwordVisible.setVisible(passwordShown);
        passwordVisible.setManaged(passwordShown);
        togglePasswordBtn.setText(passwordShown ? "⊙" : "◎");
    }

    @FXML
    private void toggleConfirmVisibility() {
        confirmPasswordShown = !confirmPasswordShown;
        confirmPasswordField.setVisible(!confirmPasswordShown);
        confirmPasswordField.setManaged(!confirmPasswordShown);
        confirmPasswordVisible.setVisible(confirmPasswordShown);
        confirmPasswordVisible.setManaged(confirmPasswordShown);
        toggleConfirmBtn.setText(confirmPasswordShown ? "⊙" : "◎");
    }

    @FXML
    private void fillAdminDemo() {
        emailField.setText("admin@ecommerce.com");
        passwordField.setText("admin123");
    }

    @FXML
    private void fillUserDemo() {
        emailField.setText("john@example.com");
        passwordField.setText("password123");
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(Color.web("#e74c3c"));
    }
}
