package com.ecommerce.ui.dialog;

import com.ecommerce.model.Review;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.ReviewRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.util.SpringContextBridge;
import com.ecommerce.util.UserContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.List;

/**
 * ReviewDialog provides a UI for viewing and submitting product reviews.
 */
public class ReviewDialog {
    private final ReviewRepository reviewRepository = SpringContextBridge.getBean(ReviewRepository.class);
    private final ProductRepository productRepository = SpringContextBridge.getBean(ProductRepository.class);
    private final UserRepository userRepository = SpringContextBridge.getBean(UserRepository.class);
    private final int productId;
    private final String productName;

    public ReviewDialog(int productId, String productName) {
        this.productId = productId;
        this.productName = productName;
    }

    public void show() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Reviews: " + productName);

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("main-content");

        // 1. Existing Reviews Section
        VBox reviewsBox = new VBox(15);
        Label reviewsTitle = new Label("Product Feedback");
        reviewsTitle.getStyleClass().add("content-title");
        reviewsBox.getChildren().add(reviewsTitle);

        ScrollPane scrollPane = new ScrollPane(reviewsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 2. Add Review Section
        VBox addReviewBox = new VBox(10);
        addReviewBox.getStyleClass().add("card");
        Label addTitle = new Label("Add Your Review");
        addTitle.getStyleClass().add("label-bright");

        HBox ratingBox = new HBox(10);
        ratingBox.setAlignment(Pos.CENTER_LEFT);
        Label ratingLabel = new Label("Rating:");
        ComboBox<Integer> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll(1, 2, 3, 4, 5);
        ratingCombo.setValue(5);
        ratingBox.getChildren().addAll(ratingLabel, ratingCombo);

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Write your feedback here...");
        commentArea.setPrefRowCount(3);

        Button submitBtn = new Button("Submit Review");
        submitBtn.getStyleClass().add("button-primary");
        submitBtn.setOnAction(e -> {
            try {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                User user = userRepository.findById(UserContext.getCurrentUserId())
                        .orElseThrow(() -> new RuntimeException("User not found"));

                Review r = new Review();
                r.setProduct(product);
                r.setUser(user);
                r.setRating(ratingCombo.getValue());
                r.setComment(commentArea.getText().trim());
                reviewRepository.save(r);
                
                // Evict cache to ensure average rating updates in UI
                org.springframework.cache.CacheManager cacheManager = SpringContextBridge.getBean(org.springframework.cache.CacheManager.class);
                if (cacheManager != null) {
                    org.springframework.cache.Cache prodCache = cacheManager.getCache("products");
                    if (prodCache != null) prodCache.clear();
                    org.springframework.cache.Cache pCache = cacheManager.getCache("product");
                    if (pCache != null) pCache.evict(productId);
                }

                commentArea.clear();
                loadReviews(reviewsBox); // Refresh locally
                com.ecommerce.util.DataEventBus.publish(); // Notify other views (admin reviews table, etc)
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Review submitted successfully!");
                alert.show();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to submit review: " + ex.getMessage());
                alert.show();
            }
        });

        addReviewBox.getChildren().addAll(addTitle, ratingBox, commentArea, submitBtn);

        root.getChildren().addAll(scrollPane, addReviewBox);

        loadReviews(reviewsBox);

        Scene scene = new Scene(root, 500, 600);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void loadReviews(VBox container) {
        javafx.application.Platform.runLater(() -> {
            container.getChildren().clear();
            Label title = new Label("Product Feedback");
            title.getStyleClass().add("content-title");
            container.getChildren().add(title);

            try {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                List<Review> reviews = reviewRepository.findByProduct(product);
                if (reviews.isEmpty()) {
                    container.getChildren().add(new Label("No reviews yet. Be the first to review!"));
                } else {
                    for (Review r : reviews) {
                        VBox rCard = new VBox(5);
                        rCard.getStyleClass().add("card");
                        rCard.setStyle("-fx-border-color: #333; -fx-background-color: #1e1e1e;");
                        
                        Label user = new Label(r.getUserName() + "  (" + "⭐".repeat(r.getRating()) + ")");
                        user.getStyleClass().add("label-bright");
                        user.setStyle("-fx-font-weight: bold;");
                        
                        Label comment = new Label(r.getComment());
                        comment.setWrapText(true);
                        
                        Label date = new Label(r.getReviewDate() != null ? r.getReviewDate().toString().split("T")[0] : "New");
                        date.getStyleClass().add("label-muted");
                        date.setStyle("-fx-font-size: 10px;");
                        
                        rCard.getChildren().addAll(user, comment, date);
                        container.getChildren().add(rCard);
                    }
                }
            } catch (Exception e) {
                container.getChildren().add(new Label("Error loading reviews."));
            }
        });
    }
}
