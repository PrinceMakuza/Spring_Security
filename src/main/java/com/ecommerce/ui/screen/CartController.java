package com.ecommerce.ui.screen;

import com.ecommerce.model.CartItem;
import com.ecommerce.service.CartService;
import com.ecommerce.util.SpringContextBridge;
import com.ecommerce.util.UserContext;
import com.ecommerce.util.DataEventBus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CartController handles shopping cart interactions using a TableView.
 * Uses ObservableList for data binding and forces UI refresh after modifications.
 */
public class CartController {
    private final CartService cartService = SpringContextBridge.getBean(CartService.class);
    
    @FXML private TableView<CartItem> cartTable;
    @FXML private Label totalLabel;
    @FXML private Label pageLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    
    private final ObservableList<CartItem> cartData = FXCollections.observableArrayList();
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private int currentPage = 1;
    private final int pageSize = 10;
    private int totalItems = 0;

    @FXML
    public void initialize() {
        setupTable();
        
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList("Name (A-Z)", "Price (Low to High)", "Price (High to Low)"));
        }
        
        // Subscribe to real-time sync from other views (e.g. ProductController adding items)
        DataEventBus.subscribe(this::loadCart);
        
        loadCart();
    }

    private void setupTable() {
        TableColumn<CartItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductName()));
        nameCol.setPrefWidth(250);

        TableColumn<CartItem, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("$%.2f", d.getValue().getUnitPrice())));
        priceCol.setPrefWidth(100);

        TableColumn<CartItem, Void> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setPrefWidth(150);
        qtyCol.setCellFactory(param -> new TableCell<>() {
            private final Button minBtn = new Button("-");
            private final Button plusBtn = new Button("+");
            private final Label qtyLabel = new Label();
            private final HBox container = new HBox(10, minBtn, qtyLabel, plusBtn);
            {
                container.setAlignment(Pos.CENTER);
                minBtn.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    updateQty(item, -1);
                });
                plusBtn.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    updateQty(item, 1);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    qtyLabel.setText(String.valueOf(getTableView().getItems().get(getIndex()).getQuantity()));
                    setGraphic(container);
                }
            }
        });

        TableColumn<CartItem, String> subtotalCol = new TableColumn<>("Subtotal");
        subtotalCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("$%.2f", d.getValue().getSubtotal())));
        subtotalCol.setPrefWidth(120);

        TableColumn<CartItem, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(180);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button buyBtn = new Button("Buy");
            private final Button removeBtn = new Button("🗑");
            private final HBox container = new HBox(10, buyBtn, removeBtn);
            {
                container.setAlignment(Pos.CENTER);
                buyBtn.getStyleClass().add("button-success");
                removeBtn.getStyleClass().add("button-danger");
                buyBtn.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    handleSingleCheckout(item);
                });
                removeBtn.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    handleRemove(item);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(container);
            }
        });

        cartTable.getColumns().setAll(nameCol, priceCol, qtyCol, subtotalCol, actionCol);
        cartTable.setItems(cartItems);
    }

    @FXML
    public void loadCart() {
        // No Platform.runLater here — DataEventBus.publish() already dispatches on FX thread
        try {
            int userId = UserContext.getCurrentUserId();
            System.out.println("DEBUG CartController.loadCart: userId=" + userId);
            List<CartItem> items = cartService.getCartItems(userId);
            System.out.println("DEBUG CartController.loadCart: items=" + (items != null ? items.size() : "null"));
            
            // Apply search/sort filter on the fetched items directly
            String search = (searchField != null && searchField.getText() != null) 
                    ? searchField.getText().toLowerCase() : "";
            
            List<CartItem> filtered = items.stream()
                .filter(i -> search.isEmpty() || i.getProductName().toLowerCase().contains(search))
                .collect(Collectors.toList());
            
            if (sortCombo != null && sortCombo.getValue() != null) {
                String sort = sortCombo.getValue();
                if ("Name (A-Z)".equals(sort)) filtered.sort(Comparator.comparing(CartItem::getProductName));
                else if ("Price (Low to High)".equals(sort)) filtered.sort(Comparator.comparing(CartItem::getUnitPrice));
                else if ("Price (High to Low)".equals(sort)) filtered.sort(Comparator.comparing(CartItem::getUnitPrice).reversed());
            }
            
            cartData.setAll(filtered);
            
            double total = filtered.stream().mapToDouble(i -> i.getUnitPrice() * i.getQuantity()).sum();
            if (totalLabel != null) {
                totalLabel.setText(String.format("Total: $%.2f", total));
            }

            totalItems = filtered.size();
            updatePaginationUI();

            int from = (currentPage - 1) * pageSize;
            int to = Math.min(from + pageSize, totalItems);
            
            if (from < totalItems) {
                cartItems.setAll(filtered.subList(from, to));
            } else {
                cartItems.clear();
            }
            
            cartTable.refresh();
            System.out.println("DEBUG CartController.loadCart: table refreshed, rows=" + cartItems.size());
        } catch (Exception e) {
            System.err.println("ERROR CartController.loadCart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePaginationUI() {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
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
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (currentPage < totalPages) {
            currentPage++;
            filterAndDisplay();
        }
    }

    @FXML
    private void filterAndDisplay() {
        // Delegate to loadCart which handles filtering
        loadCart();
    }

    private void updateQty(CartItem item, int delta) {
        try {
            int newQty = item.getQuantity() + delta;
            if (newQty <= 0) {
                cartService.removeFromCart(item.getCartItemId());
            } else {
                cartService.updateQuantity(item.getCartItemId(), newQty);
            }
            DataEventBus.publish();
        } catch (Exception e) {
            showError("Update Error", e.getMessage());
        }
    }

    private void handleRemove(CartItem item) {
        try {
            cartService.removeFromCart(item.getCartItemId());
            DataEventBus.publish();
        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }

    private void handleSingleCheckout(CartItem item) {
        try {
            cartService.checkoutSingleItem(item.getCartItemId());
            DataEventBus.publish();
            showInfo("Order Placed", "Ordered " + item.getProductName() + " successfully!");
        } catch (Exception e) {
            showError("Order Failed", e.getMessage());
        }
    }

    @FXML
    private void handleCheckout() {
        try {
            if (cartService.checkout(UserContext.getCurrentUserId())) {
                DataEventBus.publish();
                showInfo("Order Placed", "Your full order has been placed successfully!");
            } else {
                showError("Checkout", "Your cart is empty.");
            }
        } catch (Exception e) {
            showError("Checkout Error", e.getMessage());
        }
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setTitle(title);
        a.setHeaderText(null);
        a.show();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setTitle(title);
        a.show();
    }
}
