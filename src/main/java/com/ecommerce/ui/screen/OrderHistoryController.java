package com.ecommerce.ui.screen;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.service.OrderService;
import com.ecommerce.util.SpringContextBridge;
import com.ecommerce.util.UserContext;
import com.ecommerce.util.DataEventBus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OrderHistoryController handles the display and filtering of past orders.
 * Refactored for FXML compatibility and clean orchestration.
 */
public class OrderHistoryController {
    private final OrderService orderService = SpringContextBridge.getBean(OrderService.class);
    
    @FXML private VBox ordersContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label pageLabel;
    
    private List<Order> allOrders;
    private int currentPage = 1;
    private final int pageSize = 5;
    private int totalOrders = 0;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @FXML
    public void initialize() {
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList("Date (Newest)", "Date (Oldest)", "Amount (High)", "Amount (Low)"));
        }
        
        // Subscribe to real-time events
        DataEventBus.subscribe(this::loadOrders);
        
        loadOrders();
    }

    @FXML
    public void loadOrders() {
        try {
            if (searchField != null) searchField.clear();
            allOrders = orderService.getUserOrders(UserContext.getCurrentUserId());
            filterAndDisplay();
        } catch (Exception e) {
            showError("Load Error", e.getMessage());
        }
    }

    @FXML
    private void filterAndDisplay() {
        ordersContainer.getChildren().clear();
        if (allOrders == null) return;

        String search = searchField.getText().trim();
        List<Order> filtered = allOrders.stream()
            .filter(o -> search.isEmpty() || String.valueOf(o.getOrderId()).contains(search))
            .collect(Collectors.toList());

        String sort = sortCombo.getValue();
        if (sort != null) {
            if (sort.equals("Date (Newest)")) filtered.sort(java.util.Comparator.comparing(Order::getOrderDate).reversed());
            else if (sort.equals("Date (Oldest)")) filtered.sort(java.util.Comparator.comparing(Order::getOrderDate));
            else if (sort.equals("Amount (High)")) filtered.sort(java.util.Comparator.comparing(Order::getTotalAmount).reversed());
            else if (sort.equals("Amount (Low)")) filtered.sort(java.util.Comparator.comparing(Order::getTotalAmount));
        }

        if (filtered.isEmpty()) {
            Label emptyLabel = new Label(allOrders.isEmpty() ? "No orders found yet." : "No matching orders found.");
            emptyLabel.setTextFill(Color.GRAY);
            ordersContainer.getChildren().add(emptyLabel);
            return;
        }

        totalOrders = filtered.size();
        updatePaginationUI();

        int from = (currentPage - 1) * pageSize;
        int to = Math.min(from + pageSize, totalOrders);
        
        List<Order> paged = (from < totalOrders) ? filtered.subList(from, to) : java.util.Collections.emptyList();

        for (Order order : paged) {
            ordersContainer.getChildren().add(createOrderCard(order));
        }
    }

    private void updatePaginationUI() {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalOrders / pageSize));
        if (pageLabel != null) {
            pageLabel.setText(String.format("Page %d of %d", currentPage, totalPages));
        }
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            filterAndDisplay();
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = (int) Math.ceil((double) totalOrders / pageSize);
        if (currentPage < totalPages) {
            currentPage++;
            filterAndDisplay();
        }
    }

    private VBox createOrderCard(Order order) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #2a2a44; -fx-background-radius: 10; -fx-border-color: #3f3f5f; -fx-border-radius: 10;");

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox orderInfo = new VBox(2);
        Label orderIdLabel = new Label("Order #" + order.getOrderId());
        orderIdLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        orderIdLabel.setTextFill(Color.WHITE);
        Label dateLabel = new Label(order.getOrderDate().format(DATE_FORMAT));
        dateLabel.setTextFill(Color.web("#a0a8c0"));
        orderInfo.getChildren().addAll(orderIdLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label amountLabel = new Label(String.format("$%.2f", order.getTotalAmount()));
        amountLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        amountLabel.setTextFill(Color.web("#4cd137"));

        Button viewDetailsBtn = new Button("View Items");
        viewDetailsBtn.setStyle("-fx-background-color: #3f3f5f; -fx-text-fill: white;");
        
        VBox detailsContainer = new VBox(5);
        detailsContainer.setPadding(new Insets(10, 0, 0, 20));
        detailsContainer.setVisible(false);
        detailsContainer.setManaged(false);

        viewDetailsBtn.setOnAction(e -> {
            boolean isVisible = !detailsContainer.isVisible();
            detailsContainer.setVisible(isVisible);
            detailsContainer.setManaged(isVisible);
            viewDetailsBtn.setText(isVisible ? "Hide Items" : "View Items");
            
            if (isVisible && detailsContainer.getChildren().isEmpty()) {
                fetchOrderDetails(order.getOrderId(), detailsContainer);
            }
        });

        header.getChildren().addAll(orderInfo, spacer, amountLabel, viewDetailsBtn);
        card.getChildren().addAll(header, detailsContainer);
        
        return card;
    }

    private void fetchOrderDetails(int orderId, VBox container) {
        try {
            Order order = orderService.getOrderDetails(orderId);
            List<OrderItem> items = order.getItems();
            for (OrderItem item : items) {
                HBox row = new HBox(10);
                Label name = new Label(item.getProductName());
                name.setTextFill(Color.WHITE);
                name.setPrefWidth(200);
                
                Label qty = new Label("x" + item.getQuantity());
                qty.setTextFill(Color.web("#a0a8c0"));
                qty.setPrefWidth(50);
                
                Label price = new Label(String.format("$%.2f", item.getUnitPrice()));
                price.setTextFill(Color.web("#a0a8c0"));
                price.setPrefWidth(80);
                
                row.getChildren().addAll(name, qty, price);
                container.getChildren().add(row);
            }
        } catch (Exception e) {
            container.getChildren().add(new Label("Error: " + e.getMessage()));
        }
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.show();
    }
}
