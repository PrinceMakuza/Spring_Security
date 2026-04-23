package com.ecommerce.ui.screen;

import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.CartService;
import com.ecommerce.util.SpringContextBridge;
import com.ecommerce.util.PerformanceMonitor;
import com.ecommerce.util.UserContext;
import com.ecommerce.util.DataEventBus;
import com.ecommerce.ui.dialog.ReviewDialog;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Optional;

/**
 * ProductController manages the customer-facing product browsing view.
 * Now features real-time synchronization and improved quantity selection.
 */
public class ProductController {
    private final ProductService productService = SpringContextBridge.getBean(ProductService.class);
    private final CartService cartService = SpringContextBridge.getBean(CartService.class);
    private final CategoryService categoryService = SpringContextBridge.getBean(CategoryService.class);
    
    @FXML private TableView<Product> productTable;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label pageLabel;
    @FXML private Label resultCountLabel;
    @FXML private Label timingLabel;
    
    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private int currentPage = 1;
    private final int pageSize = 500;
    private int totalProducts = 0;
    private List<Category> categories;

    @FXML
    public void initialize() {
        setupTable();
        loadInitialData();
        
        // Subscribe to real-time events
        DataEventBus.subscribe(this::loadData);
        
        // Listeners for instant filtering
        categoryFilter.setOnAction(e -> handleSearch());
        sortCombo.setOnAction(e -> handleSearch());
        searchField.textProperty().addListener((o, ov, nv) -> handleSearch());
    }

    private void setupTable() {
        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(220);

        TableColumn<Product, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("$%.2f", d.getValue().getPrice())));
        priceCol.setPrefWidth(100);
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryName()));
        catCol.setPrefWidth(140);

        TableColumn<Product, String> stockCol = new TableColumn<>("In Stock");
        stockCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getStockQuantity())));
        stockCol.setPrefWidth(100);
        stockCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    int stock = Integer.parseInt(item);
                    if (stock <= 0) setStyle("-fx-text-fill: #e05555; -fx-font-weight: bold;");
                    else if (stock < 5) setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: #38b86c; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<Product, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(120);
        actionCol.setCellFactory(param -> new TableCell<Product, Void>() {
            private final Button cartBtn = new Button("Add to Cart");
            {
                cartBtn.getStyleClass().add("button-success");
                cartBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
                cartBtn.setOnAction(event -> handleAddToCart(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    HBox container = new HBox(cartBtn);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
                }
            }
        });

        TableColumn<Product, Product> ratingCol = new TableColumn<>("Rating");
        ratingCol.setPrefWidth(160);
        ratingCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        ratingCol.setCellFactory(param -> new TableCell<Product, Product>() {
            private final Label starLabel = new Label();
            private final Button rateBtn = new Button("⭐ Rate");
            private final HBox container = new HBox(10);
            {
                starLabel.setStyle("-fx-text-fill: #ffcc00; -fx-font-weight: bold;");
                rateBtn.getStyleClass().add("button-primary");
                rateBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8;");
                container.setAlignment(Pos.CENTER_LEFT);
                container.getChildren().addAll(starLabel, rateBtn);
            }
            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) setGraphic(null);
                else {
                    starLabel.setText(p.getRatingStars());
                    rateBtn.setOnAction(e -> new ReviewDialog(p.getProductId(), p.getName()).show());
                    setGraphic(container);
                }
            }
        });

        productTable.getColumns().setAll(nameCol, priceCol, catCol, stockCol, actionCol, ratingCol);
        productTable.setItems(productList);
    }

    private void loadInitialData() {
        try {
            categories = categoryService.getAllCategories();
            categoryFilter.getItems().clear();
            categoryFilter.getItems().add("All Categories");
            for (Category cat : categories) categoryFilter.getItems().add(cat.getName());
            categoryFilter.setValue("All Categories");
            
            sortCombo.getItems().setAll("Name (A-Z)", "Name (Z-A)", "Price (Low to High)", "Price (High to Low)");
            sortCombo.setValue("Name (A-Z)");
            
            loadData();
        } catch (Exception e) {}
    }

    private void loadData() {
        PerformanceMonitor.start("ProductSearch");
        try {
            String search = searchField.getText();
            String sortBy = "name", dir = "asc";
            String sortVal = sortCombo.getValue();
            if ("Name (Z-A)".equals(sortVal)) dir = "desc";
            else if ("Price (Low to High)".equals(sortVal)) sortBy = "price";
            else if ("Price (High to Low)".equals(sortVal)) { sortBy = "price"; dir = "desc"; }

            var page = productService.getProducts(currentPage - 1, pageSize, sortBy, dir, search, getSelectedCategoryId(), null, null);
            productList.setAll(page.getContent());
            totalProducts = (int) page.getTotalElements();
            
            updatePaginationUI();
            resultCountLabel.setText("Showing " + productList.size() + " products");
            timingLabel.setText("Fetched in " + PerformanceMonitor.stop("ProductSearch") + "ms");
        } catch (Exception e) {}
    }

    private Integer getSelectedCategoryId() {
        String selected = categoryFilter.getValue();
        if (selected == null || "All Categories".equals(selected)) return null;
        return categories.stream().filter(c -> c.getName().equals(selected)).findFirst().map(Category::getCategoryId).orElse(null);
    }

    private void updatePaginationUI() {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalProducts / pageSize));
        pageLabel.setText(String.format("Page %d of %d", currentPage, totalPages));
    }

    @FXML
    private void handleSearch() {
        currentPage = 1;
        loadData();
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        categoryFilter.setValue("All Categories");
        handleSearch();
    }

    @FXML
    private void handlePrevPage() { if (currentPage > 1) { currentPage--; loadData(); } }
    @FXML
    private void handleNextPage() {
        int totalPages = (int) Math.ceil((double) totalProducts / pageSize);
        if (currentPage < totalPages) { currentPage++; loadData(); }
    }

    private void handleAddToCart(Product product) {
        if (product.getStockQuantity() <= 0) {
            showError("Out of Stock", "This item is currently unavailable.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Select Quantity");
        dialog.setHeaderText("Add " + product.getName() + " to Cart");
        dialog.setContentText("Please enter quantity:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(qtyStr -> {
            try {
                int qty = Integer.parseInt(qtyStr);
                if (qty <= 0 || qty > product.getStockQuantity()) {
                    showError("Invalid Quantity", "Current stock: " + product.getStockQuantity());
                    return;
                }
                cartService.addToCart(UserContext.getCurrentUserId(), product.getProductId(), qty);
                DataEventBus.publish(); // Force refresh across all views
                showInfo("Added to Cart ✓", qty + " unit(s) of " + product.getName() + " added to your cart!");
            } catch (NumberFormatException e) {
                showError("Input Error", "Please enter a valid number.");
            } catch (Exception e) {
                System.err.println("ERROR adding to cart: " + e.getMessage());
                e.printStackTrace();
                showError("Cart Error", e.getMessage());
            }
        });
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setTitle(title); a.setHeaderText(null); a.show();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setTitle(title); a.show();
    }
}
