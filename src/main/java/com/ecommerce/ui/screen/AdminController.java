package com.ecommerce.ui.screen;

import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.model.Order;
import com.ecommerce.model.Review;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.UserService;
import com.ecommerce.service.OrderService;
import com.ecommerce.repository.ReviewRepository;
import com.ecommerce.util.SpringContextBridge;
import com.ecommerce.util.DataEventBus;
import com.ecommerce.service.ReportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminController manages all back-office operations.
 * Updated to fully implement search and sort logic across all management panels.
 */
public class AdminController {
    private final ProductService productService = SpringContextBridge.getBean(ProductService.class);
    private final CategoryService categoryService = SpringContextBridge.getBean(CategoryService.class);
    private final UserService userService = SpringContextBridge.getBean(UserService.class);
    private final OrderService orderService = SpringContextBridge.getBean(OrderService.class);
    private final ReviewRepository reviewRepository = SpringContextBridge.getBean(ReviewRepository.class);
    private final ReportService reportService = SpringContextBridge.getBean(ReportService.class);

    @FXML private HBox toggleBar;
    @FXML private StackPane contentPane;
    @FXML private Button prodBtn, catBtn, userBtn, orderBtn, invBtn, reviewBtn, reportBtn;

    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private final ObservableList<Order> orderList = FXCollections.observableArrayList();
    private final ObservableList<Review> reviewList = FXCollections.observableArrayList();

    private TextField productSearch, catSearch, userSearch, orderSearch, reviewSearch, invSearch;
    private ComboBox<String> productSort, catSort, userSort, orderSort, reviewSort, invSort;
    private Label prodPageLabel, catPageLabel, userPageLabel, orderPageLabel, reviewPageLabel, invPageLabel;
    private int prodPage = 1, catPage = 1, userPage = 1, orderPage = 1, reviewPage = 1, invPage = 1;
    private final int pageSize = 10;
    private int prodTotal = 0, catTotal = 0, userTotal = 0, orderTotal = 0, reviewTotal = 0, invTotal = 0;
    private List<Category> categories;

    private VBox prodPanel, catPanel, userPanel, orderPanel, reviewPanel, invPanel, reportPanel;

    @FXML
    public void initialize() {
        try {
            prodPanel = buildProductPanel();
            catPanel = buildCategoryPanel();
            userPanel = buildUserPanel();
            orderPanel = buildOrderPanel();
            reviewPanel = buildReviewPanel();
            invPanel = buildInventoryPanel();
            reportPanel = buildReportPanel();
            
            contentPane.getChildren().addAll(prodPanel, catPanel, userPanel, orderPanel, reviewPanel, invPanel, reportPanel);
            showProducts();
            
            // Subscribe to real-time events
            DataEventBus.subscribe(this::loadAllData);
        } catch (Exception e) {
            System.err.println("[AdminController] Error during initialization: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to let FXMLLoader know it failed
        }
    }

    private void loadAllData() {
        loadInitialData();
        loadUsers();
        applyOrderFilters();
        loadInventory();
        applyReviewFilters();
    }

    @FXML private void showProducts() { switchTab(prodPanel, prodBtn); loadInitialData(); }
    @FXML private void showCategories() { switchTab(catPanel, catBtn); loadInitialData(); }
    @FXML private void showUsers() { loadUsers(); switchTab(userPanel, userBtn); }
    @FXML private void showOrders() { loadOrders(); switchTab(orderPanel, orderBtn); }
    @FXML private void showInventory() { loadInventory(); switchTab(invPanel, invBtn); }
    @FXML private void showReviews() { loadReviews(); switchTab(reviewPanel, reviewBtn); }
    @FXML private void showReports() { switchTab(reportPanel, reportBtn); }

    private void switchTab(VBox panel, Button btn) {
        contentPane.getChildren().forEach(c -> c.setVisible(false));
        panel.setVisible(true);
        toggleBar.getChildren().forEach(b -> b.getStyleClass().remove("button-primary"));
        btn.getStyleClass().add("button-primary");
    }

    // --- PANEL BUILDERS ---

    private VBox buildProductPanel() {
        VBox panel = new VBox(15);
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        Button add = new Button("➕ Add New Product"); add.getStyleClass().add("button-success");
        Button edit = new Button("✏ Edit Selected"); edit.getStyleClass().add("button-primary");
        Button del = new Button("🗑 Delete Selected"); del.getStyleClass().add("button-danger");
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        productSearch = new TextField(); productSearch.setPromptText("🔍 Search products...");
        productSearch.textProperty().addListener((o, ov, nv) -> loadInitialData());

        productSort = new ComboBox<>(FXCollections.observableArrayList("ID (Oldest First)", "ID (Newest First)", "Name (A-Z)", "Name (Z-A)", "Price (Low to High)", "Price (High to Low)"));
        productSort.setValue("ID (Oldest First)");
        productSort.setOnAction(e -> loadInitialData());

        Button refresh = new Button("🔄");
        refresh.setOnAction(e -> {
            productSearch.clear();
            loadInitialData();
        });
        bar.getChildren().addAll(add, edit, del, spacer, productSearch, productSort, refresh);
        TableView<Product> table = createProductTable();
        add.setOnAction(e -> handleProductDialog(null));
        edit.setOnAction(e -> { Product s = table.getSelectionModel().getSelectedItem(); if (s != null) handleProductDialog(s); });
        del.setOnAction(e -> { Product s = table.getSelectionModel().getSelectedItem(); if (s != null) handleDeleteProduct(s); });
        VBox.setVgrow(table, Priority.ALWAYS);
        prodPageLabel = new Label("Page 1 of 1");
        HBox pagination = createPaginationBar(prodPageLabel, e -> { if (prodPage > 1) { prodPage--; loadInitialData(); } }, e -> { if (prodPage < (int)Math.ceil((double)prodTotal/pageSize)) { prodPage++; loadInitialData(); } });
        panel.getChildren().addAll(bar, table, pagination);
        return panel;
    }

    private VBox buildCategoryPanel() {
        VBox panel = new VBox(15);
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        Button add = new Button("➕ Add Category"); add.getStyleClass().add("button-success");
        Button edit = new Button("✏ Edit Selected"); edit.getStyleClass().add("button-primary");
        Button del = new Button("🗑 Delete Selected"); del.getStyleClass().add("button-danger");
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        catSearch = new TextField(); catSearch.setPromptText("🔍 Search categories...");
        catSearch.textProperty().addListener((o, ov, nv) -> loadInitialData());

        catSort = new ComboBox<>(FXCollections.observableArrayList("ID ASC", "ID DESC", "Name A-Z", "Name Z-A"));
        catSort.setValue("ID ASC");
        catSort.setOnAction(e -> loadInitialData());

        Button refresh = new Button("🔄");
        refresh.setOnAction(e -> {
            catSearch.clear();
            loadInitialData();
        });
        bar.getChildren().addAll(add, edit, del, spacer, catSearch, catSort, refresh);

        TableView<Category> table = createCategoryTable();
        add.setOnAction(e -> handleCategoryDialog(null));
        edit.setOnAction(e -> { Category s = table.getSelectionModel().getSelectedItem(); if (s != null) handleCategoryDialog(s); });
        del.setOnAction(e -> { Category s = table.getSelectionModel().getSelectedItem(); if (s != null) handleDeleteCategory(s); });
        VBox.setVgrow(table, Priority.ALWAYS);
        catPageLabel = new Label("Page 1 of 1");
        HBox pagination = createPaginationBar(catPageLabel, e -> { if (catPage > 1) { catPage--; loadInitialData(); } }, e -> { if (catPage < (int)Math.ceil((double)catTotal/pageSize)) { catPage++; loadInitialData(); } });
        panel.getChildren().addAll(bar, table, pagination);
        return panel;
    }

    private VBox buildUserPanel() {
        VBox panel = new VBox(15);
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        Button add = new Button("➕ Add New User"); add.getStyleClass().add("button-success");
        Button edit = new Button("✏ Edit Selected"); edit.getStyleClass().add("button-primary");
        Button del = new Button("🗑 Delete Selected"); del.getStyleClass().add("button-danger");
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        userSearch = new TextField(); userSearch.setPromptText("🔍 Search users by name/email...");
        userSort = new ComboBox<>(FXCollections.observableArrayList("ID ASC", "ID DESC", "Name A-Z", "Name Z-A", "Role"));
        userSort.setValue("ID ASC");

        userSort.setOnAction(e -> loadUsers());
        Button refresh = new Button("🔄");
        refresh.setOnAction(e -> {
            userSearch.clear();
            loadUsers();
        });
        bar.getChildren().addAll(add, edit, del, spacer, userSearch, userSort, refresh);

        TableView<User> table = createUserTable();
        add.setOnAction(e -> handleUserDialog(null));
        edit.setOnAction(e -> { User s = table.getSelectionModel().getSelectedItem(); if (s != null) handleUserDialog(s); });
        del.setOnAction(e -> { User s = table.getSelectionModel().getSelectedItem(); if (s != null) handleDeleteUser(s); });
        VBox.setVgrow(table, Priority.ALWAYS);
        userPageLabel = new Label("Page 1 of 1");
        HBox pagination = createPaginationBar(userPageLabel, e -> { if (userPage > 1) { userPage--; loadUsers(); } }, e -> { if (userPage < (int)Math.ceil((double)userTotal/pageSize)) { userPage++; loadUsers(); } });
        panel.getChildren().addAll(bar, table, pagination);
        return panel;
    }

    private VBox buildOrderPanel() {
        VBox panel = new VBox(15);
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("System Orders History"); lbl.getStyleClass().add("label-bright");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        orderSearch = new TextField(); orderSearch.setPromptText("🔍 Search by Order ID or Customer...");
        orderSearch.textProperty().addListener((o, ov, nv) -> applyOrderFilters());
        
        orderSort = new ComboBox<>(FXCollections.observableArrayList("Date (Newest)", "Date (Oldest)", "Amount (High)", "Status"));
        orderSort.setValue("Date (Newest)");
        orderSort.setOnAction(e -> applyOrderFilters());

        Button refresh = new Button("🔄");
        refresh.setOnAction(e -> {
            orderSearch.clear();
            applyOrderFilters();
        });
        bar.getChildren().addAll(lbl, spacer, orderSearch, orderSort, refresh);
        TableView<Order> table = createOrderTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        orderPageLabel = new Label("Page 1 of 1");
        HBox pagination = createPaginationBar(orderPageLabel, e -> { if (orderPage > 1) { orderPage--; applyOrderFilters(); } }, e -> { if (orderPage < (int)Math.ceil((double)orderTotal/pageSize)) { orderPage++; applyOrderFilters(); } });
        panel.getChildren().addAll(bar, table, pagination);
        return panel;
    }

    private VBox buildReviewPanel() {
        VBox panel = new VBox(15);
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("Product Reviews Moderation"); lbl.getStyleClass().add("label-bright");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        reviewSearch = new TextField(); reviewSearch.setPromptText("🔍 Search reviews text or product...");
        reviewSearch.textProperty().addListener((o, ov, nv) -> applyReviewFilters());

        reviewSort = new ComboBox<>(FXCollections.observableArrayList("Rating (High)", "Rating (Low)", "Product Name"));
        reviewSort.setValue("Rating (High)");
        reviewSort.setOnAction(e -> applyReviewFilters());

        Button refresh = new Button("🔄");
        refresh.setOnAction(e -> {
            reviewSearch.clear();
            applyReviewFilters();
        });
        bar.getChildren().addAll(lbl, spacer, reviewSearch, reviewSort, refresh);
        TableView<Review> table = createReviewTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        reviewPageLabel = new Label("Page 1 of 1");
        HBox pagination = createPaginationBar(reviewPageLabel, e -> { if (reviewPage > 1) { reviewPage--; applyReviewFilters(); } }, e -> { if (reviewPage < (int)Math.ceil((double)reviewTotal/pageSize)) { reviewPage++; applyReviewFilters(); } });
        panel.getChildren().addAll(bar, table, pagination);
        return panel;
    }

    private VBox buildInventoryPanel() {
        VBox panel = new VBox(15);
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        Button edit = new Button("🔧 Edit Stock Level"); edit.getStyleClass().add("button-primary");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        invSearch = new TextField(); invSearch.setPromptText("🔍 Search stock...");
        invSearch.textProperty().addListener((o, ov, nv) -> loadInventory());

        invSort = new ComboBox<>(FXCollections.observableArrayList("ID (Oldest First)", "ID (Newest First)", "Stock (Low to High)", "Stock (High to Low)"));
        invSort.setValue("ID (Oldest First)");
        invSort.setOnAction(e -> loadInventory());

        Button refresh = new Button("🔄");
        refresh.setOnAction(e -> {
            invSearch.clear();
            loadInventory();
        });
        bar.getChildren().addAll(edit, spacer, invSearch, invSort, refresh);
        TableView<Product> table = createInventoryTable();
        edit.setOnAction(e -> { Product s = table.getSelectionModel().getSelectedItem(); if (s != null) handleInventoryDialog(s); });
        VBox.setVgrow(table, Priority.ALWAYS);
        invPageLabel = new Label("Page 1 of 1");
        HBox pagination = createPaginationBar(invPageLabel, e -> { if (invPage > 1) { invPage--; loadInventory(); } }, e -> { if (invPage < (int)Math.ceil((double)invTotal/pageSize)) { invPage++; loadInventory(); } });
        panel.getChildren().addAll(bar, table, pagination);
        return panel;
    }

    private HBox createPaginationBar(Label label, javafx.event.EventHandler<javafx.event.ActionEvent> prev, javafx.event.EventHandler<javafx.event.ActionEvent> next) {
        HBox hbox = new HBox(15);
        hbox.setAlignment(Pos.CENTER);
        hbox.setPadding(new Insets(10));
        hbox.getStyleClass().add("pagination-bar");
        Button prevBtn = new Button("◀ Previous");
        Button nextBtn = new Button("Next ▶");
        prevBtn.setOnAction(prev);
        nextBtn.setOnAction(next);
        label.getStyleClass().add("page-label");
        hbox.getChildren().addAll(prevBtn, label, nextBtn);
        return hbox;
    }

    private VBox buildReportPanel() {
        VBox panel = new VBox(25); panel.setAlignment(Pos.CENTER); panel.setPadding(new Insets(50));
        VBox reportBox = new VBox(20); reportBox.setAlignment(Pos.CENTER); reportBox.setMaxWidth(500); reportBox.setPadding(new Insets(40));
        reportBox.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 15; -fx-border-color: #333; -fx-border-radius: 15;");

        Button perfBtn = new Button("🚀 Generate Performance Report"); perfBtn.getStyleClass().add("button-primary");
        Button valBtn = new Button("✅ Generate Validation Report"); valBtn.getStyleClass().add("button-success");
        Button docBtn = new Button("📚 Generate System Documentation"); docBtn.getStyleClass().add("button-primary");
        ProgressIndicator progress = new ProgressIndicator(); progress.setVisible(false);

        perfBtn.setOnAction(e -> runReportTask("Performance Report", () -> reportService.generatePerformanceReport(), progress));
        valBtn.setOnAction(e -> runReportTask("Validation Report", () -> reportService.generateValidationReport(), progress));
        docBtn.setOnAction(e -> runReportTask("System Documentation", () -> reportService.generateSystemDocumentation(), progress));
        reportBox.getChildren().addAll(perfBtn, valBtn, docBtn, progress);
        panel.getChildren().addAll(reportBox);
        return panel;
    }

    private void runReportTask(String title, ReportTask task, ProgressIndicator progress) {
        progress.setVisible(true);
        new Thread(() -> {
            try {
                String path = task.execute();
                javafx.application.Platform.runLater(() -> {
                    progress.setVisible(false);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Report saved to: " + path);
                    alert.setTitle(title); alert.show();
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> { progress.setVisible(false); showAlert(Alert.AlertType.ERROR, "Failed", ex.getMessage()); });
            }
        }).start();
    }

    @FunctionalInterface interface ReportTask { String execute() throws Exception; }

    // --- DIALOGS & HANDLERS ---

    private void handleProductDialog(Product existing) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add New Product" : "Edit Product");
        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField name = new TextField(existing != null ? existing.getName() : "");
        TextArea desc = new TextArea(existing != null ? existing.getDescription() : ""); desc.setPrefRowCount(3);
        TextField price = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "");
        TextField stock = new TextField(existing != null ? String.valueOf(existing.getStockQuantity()) : "10");
        ComboBox<String> cat = new ComboBox<>(FXCollections.observableArrayList(categories.stream().map(Category::getName).toList()));
        if (existing != null) cat.setValue(existing.getCategoryName());

        grid.add(new Label("Name:"), 0, 0); grid.add(name, 1, 0);
        grid.add(new Label("Description:"), 0, 1); grid.add(desc, 1, 1);
        grid.add(new Label("Price:"), 0, 2); grid.add(price, 1, 2);
        grid.add(new Label("Stock:"), 0, 3); grid.add(stock, 1, 3);
        grid.add(new Label("Category:"), 0, 4); grid.add(cat, 1, 4);
        dialog.getDialogPane().setContent(grid);
        
        Button okButton = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                if (name.getText().trim().isEmpty()) throw new Exception("Name is required.");
                Double.parseDouble(price.getText());
                Integer.parseInt(stock.getText());
                if (cat.getValue() == null) throw new Exception("Category is required.");
            } catch (Exception ex) { showAlert(Alert.AlertType.ERROR, "Validation Error", ex.getMessage()); event.consume(); }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveBtnType) {
                Product p = existing != null ? existing : new Product();
                p.setName(name.getText().trim()); p.setDescription(desc.getText().trim());
                p.setPrice(Double.parseDouble(price.getText()));
                p.setStockQuantity(Integer.parseInt(stock.getText()));
                categories.stream().filter(c -> c.getName().equals(cat.getValue())).findFirst().ifPresent(c -> p.setCategoryId(c.getCategoryId()));
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            try { 
                if (existing == null) productService.createProduct(convertToDTO(p)); 
                else productService.updateProduct(p.getProductId(), convertToDTO(p)); 
                DataEventBus.publish(); 
            }
            catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
        });
    }

    private com.ecommerce.dto.ProductDTO convertToDTO(Product p) {
        return new com.ecommerce.dto.ProductDTO(p.getProductId(), p.getName(), p.getDescription(), p.getPrice(), p.getCategoryId(), p.getStockQuantity());
    }

    private void handleDeleteProduct(Product selected) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remove " + selected.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { 
                    productService.deleteProduct(selected.getProductId()); 
                    DataEventBus.publish(); 
                }
                catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
            }
        });
    }

    private void handleCategoryDialog(Category existing) {
        TextInputDialog dialog = new TextInputDialog(existing != null ? existing.getName() : "");
        dialog.setTitle(existing == null ? "Add Category" : "Edit Category");
        dialog.setHeaderText("Enter category name:");
        dialog.showAndWait().ifPresent(name -> {
            try {
                com.ecommerce.dto.CategoryDTO dto = new com.ecommerce.dto.CategoryDTO(existing != null ? existing.getCategoryId() : null, name, "");
                if (existing == null) { categoryService.createCategory(dto); }
                else { categoryService.updateCategory(existing.getCategoryId(), dto); }
                DataEventBus.publish();
            } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
        });
    }

    private void handleDeleteCategory(Category selected) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remove category " + selected.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { 
                    categoryService.deleteCategory(selected.getCategoryId()); 
                    DataEventBus.publish(); 
                }
                catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
            }
        });
    }

    private void handleInventoryDialog(Product product) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(product.getStockQuantity()));
        dialog.setTitle("Update Stock");
        dialog.setHeaderText("Update stock for: " + product.getName());
        dialog.showAndWait().ifPresent(qty -> {
            try { 
                product.setStockQuantity(Integer.parseInt(qty)); 
                productService.updateProduct(product.getProductId(), convertToDTO(product)); 
                DataEventBus.publish(); 
            }
            catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", "Invalid quantity: " + e.getMessage()); }
        });
    }

    private void handleUserDialog(User existing) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add New User" : "Edit User");
        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField name = new TextField(existing != null ? existing.getName() : "");
        TextField email = new TextField(existing != null ? existing.getEmail() : "");
        TextField location = new TextField(existing != null ? existing.getLocation() : "");
        ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList("CUSTOMER", "ADMIN"));
        role.setValue(existing != null ? existing.getRole() : "CUSTOMER");
        grid.add(new Label("Name:"), 0, 0); grid.add(name, 1, 0);
        grid.add(new Label("Email:"), 0, 1); grid.add(email, 1, 1);
        grid.add(new Label("Location:"), 0, 2); grid.add(location, 1, 2);
        grid.add(new Label("Role:"), 0, 3); grid.add(role, 1, 3);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == saveBtnType) {
                User u = existing != null ? existing : new User();
                u.setName(name.getText().trim()); u.setEmail(email.getText().trim().toLowerCase());
                u.setRole(role.getValue()); u.setLocation(location.getText().trim());
                if (existing == null) { u.setPassword(BCrypt.hashpw(u.getName().split("\\s+")[0] + "@123", BCrypt.gensalt())); }
                return u;
            }
            return null;
        });
        dialog.showAndWait().ifPresent(u -> {
            try { 
                if (existing == null) userService.createUser(convertToUserDTO(u)); 
                else userService.updateUser(u.getUserId(), convertToUserDTO(u)); 
                DataEventBus.publish(); 
            }
            catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
        });
    }

    private com.ecommerce.dto.UserDTO convertToUserDTO(User u) {
        return new com.ecommerce.dto.UserDTO(u.getUserId() != 0 ? u.getUserId() : null, u.getName(), u.getEmail(), u.getRole(), u.getPassword(), u.getLocation());
    }

    private void handleDeleteUser(User selected) {
        if ("ADMIN".equalsIgnoreCase(selected.getRole())) { showAlert(Alert.AlertType.WARNING, "Guard", "Admins cannot be removed."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remove user " + selected.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { 
                    userService.deleteUser(selected.getUserId()); 
                    DataEventBus.publish(); 
                }
                catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
            }
        });
    }


    // --- TABLE GENERATORS ---

    private TableView<Product> createProductTable() {
        TableView<Product> t = new TableView<>(productList);
        addColumn(t, "ID", 60, d -> new SimpleObjectProperty<Object>(d.getProductId()));
        addColumn(t, "Name", 200, d -> new SimpleStringProperty(d.getName()));
        addColumn(t, "Price", 100, d -> new SimpleStringProperty(String.format("$%.2f", d.getPrice())));
        addColumn(t, "Category", 150, d -> new SimpleStringProperty(d.getCategoryName()));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

    private TableView<Category> createCategoryTable() {
        FilteredList<Category> filtered = new FilteredList<>(categoryList, p -> true);
        if (catSearch != null) catSearch.textProperty().addListener((o, ov, nv) -> filtered.setPredicate(c -> nv == null || nv.isEmpty() || c.getName().toLowerCase().contains(nv.toLowerCase())));
        TableView<Category> t = new TableView<>(filtered);
        addColumn(t, "ID", 60, d -> new SimpleObjectProperty<Object>(d.getCategoryId()));
        addColumn(t, "Name", 300, d -> new SimpleStringProperty(d.getName()));
        return t;
    }

    private TableView<User> createUserTable() {
        FilteredList<User> filtered = new FilteredList<>(userList, p -> true);
        userSearch.textProperty().addListener((o, ov, nv) -> filtered.setPredicate(u -> {
            if (nv == null || nv.isEmpty()) return true;
            String lower = nv.toLowerCase();
            return u.getName().toLowerCase().contains(lower) || u.getEmail().toLowerCase().contains(lower);
        }));

        SortedList<User> sorted = new SortedList<>(filtered);
        userSort.setOnAction(e -> applyUserSorting(sorted));
        
        TableView<User> t = new TableView<>(sorted);
        addColumn(t, "ID", 60, d -> new SimpleObjectProperty<Object>(d.getUserId()));
        addColumn(t, "Name", 200, d -> new SimpleStringProperty(d.getName()));
        addColumn(t, "Email", 250, d -> new SimpleStringProperty(d.getEmail()));
        addColumn(t, "Location", 150, d -> new SimpleStringProperty(d.getLocation()));
        addColumn(t, "Role", 100, d -> new SimpleStringProperty(d.getRole()));
        return t;
    }

    private void applyUserSorting(SortedList<User> sorted) {
        String mode = userSort.getValue();
        if ("ID ASC".equals(mode)) sorted.setComparator(Comparator.comparing(User::getUserId));
        else if ("ID DESC".equals(mode)) sorted.setComparator(Comparator.comparing(User::getUserId).reversed());
        else if ("Name A-Z".equals(mode)) sorted.setComparator(Comparator.comparing(User::getName));
        else if ("Role".equals(mode)) sorted.setComparator(Comparator.comparing(User::getRole));
    }

    private TableView<Order> createOrderTable() {
        FilteredList<Order> filtered = new FilteredList<>(orderList, p -> true);
        TableView<Order> t = new TableView<>(new SortedList<>(filtered)); // Initial empty sorted list
        // Note: The actual sorted logic is handled in applyOrderFilters() which updates orderList
        addColumn(t, "Order ID", 80, d -> new SimpleObjectProperty<Object>(d.getOrderId()));
        addColumn(t, "Customer", 200, d -> new SimpleStringProperty(d.getUserName()));
        addColumn(t, "Total", 100, d -> new SimpleStringProperty(String.format("$%.2f", d.getTotalAmount())));
        addColumn(t, "Status", 100, d -> new SimpleStringProperty(d.getStatus()));
        return t;
    }

    private void applyOrderFilters() {
        try {
            String sortBy = "orderDate";
            String dir = "desc";
            String sort = orderSort.getValue();
            if ("Date (Oldest)".equals(sort)) dir = "asc";
            else if ("Amount (High)".equals(sort)) { sortBy = "totalAmount"; dir = "desc"; }
            else if ("Status".equals(sort)) { sortBy = "status"; dir = "asc"; }

            var orderPageObj = orderService.getAllOrders(orderPage - 1, pageSize, sortBy, dir);
            String search = orderSearch.getText().toLowerCase();
            List<Order> filtered = orderPageObj.getContent().stream().filter(o -> 
                String.valueOf(o.getOrderId()).contains(search) || 
                o.getUserName().toLowerCase().contains(search)
            ).collect(Collectors.toList());

            orderList.setAll(filtered);
            orderTotal = (int) orderPageObj.getTotalElements();
            if (orderPageLabel != null) orderPageLabel.setText(String.format("Page %d of %d", orderPage, Math.max(1, (int)Math.ceil((double)orderTotal/pageSize))));
        } catch (Exception e) {
            System.err.println("[AdminController] Error loading orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private TableView<Review> createReviewTable() {
        TableView<Review> t = new TableView<>(reviewList);
        addColumn(t, "ID", 60, d -> new SimpleObjectProperty<Object>(d.getReviewId()));
        addColumn(t, "Product", 180, d -> new SimpleStringProperty(d.getProductName()));
        addColumn(t, "User", 120, d -> new SimpleStringProperty(d.getUserName()));
        
        TableColumn<Review, String> ratingCol = new TableColumn<>("Rating");
        ratingCol.setPrefWidth(120);
        ratingCol.setCellValueFactory(data -> new SimpleStringProperty("⭐".repeat(data.getValue().getRating())));
        ratingCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #ffcc00; -fx-font-weight: bold;");
                }
            }
        });
        t.getColumns().add(ratingCol);
        
        addColumn(t, "Comment", 350, d -> new SimpleStringProperty(d.getComment()));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

    private void applyReviewFilters() {
        try {
            List<Review> all = reviewRepository.findAllWithUserAndProduct();
            String search = reviewSearch.getText().toLowerCase();
            List<Review> filtered = all.stream().filter(r -> 
                r.getProductName().toLowerCase().contains(search) || 
                r.getComment().toLowerCase().contains(search)
            ).collect(Collectors.toList());

            String sort = reviewSort.getValue();
            if ("Rating (High)".equals(sort)) filtered.sort(Comparator.comparing(Review::getRating).reversed());
            else if ("Rating (Low)".equals(sort)) filtered.sort(Comparator.comparing(Review::getRating));
            else if ("Product Name".equals(sort)) filtered.sort(Comparator.comparing(Review::getProductName));

            // Review doesn't have paginated service yet, using manual paging on list
            reviewTotal = filtered.size();
            int from = (reviewPage - 1) * pageSize;
            int to = Math.min(from + pageSize, reviewTotal);
            if (from < reviewTotal) reviewList.setAll(filtered.subList(from, to));
            else reviewList.clear();
            
            if (reviewPageLabel != null) reviewPageLabel.setText(String.format("Page %d of %d", reviewPage, Math.max(1, (int)Math.ceil((double)reviewTotal/pageSize))));
        } catch (Exception e) {
            System.err.println("[AdminController] Error loading reviews: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private TableView<Product> createInventoryTable() {
        TableView<Product> t = new TableView<>(productList);
        addColumn(t, "ID", 60, d -> new SimpleObjectProperty<>(d.getProductId()));
        addColumn(t, "Product Name", 250, d -> new SimpleStringProperty(d.getName()));
        
        TableColumn<Product, Integer> stockCol = new TableColumn<>("In Stock");
        stockCol.setPrefWidth(120);
        stockCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStockQuantity()));
        stockCol.setCellFactory(column -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    if (item <= 5) setStyle("-fx-text-fill: #ff4d4d; -fx-font-weight: bold;"); // Red
                    else if (item <= 20) setStyle("-fx-text-fill: #ffcc00; -fx-font-weight: bold;"); // Yellow
                    else setStyle("-fx-text-fill: #38b86c; -fx-font-weight: bold;"); // Green
                }
            }
        });
        t.getColumns().add(stockCol);
        
        addColumn(t, "Price", 100, d -> new SimpleStringProperty(String.format("$%.2f", d.getPrice())));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

    // --- UTILS ---

    private void loadInitialData() { 
        try { 
            String catSortVal = (catSort != null) ? catSort.getValue() : "ID ASC";
            String catSortBy = "categoryId";
            String catDir = "asc";
            if ("ID DESC".equals(catSortVal)) { catSortBy = "categoryId"; catDir = "desc"; }
            else if ("Name A-Z".equals(catSortVal)) { catSortBy = "name"; catDir = "asc"; }
            else if ("Name Z-A".equals(catSortVal)) { catSortBy = "name"; catDir = "desc"; }

            var catPageObj = categoryService.getCategories(catPage - 1, pageSize, catSortBy, catDir, (catSearch != null ? catSearch.getText() : null));
            categories = catPageObj.getContent();
            categoryList.setAll(categories);
            catTotal = (int) catPageObj.getTotalElements();
            if (catPageLabel != null) catPageLabel.setText(String.format("Page %d of %d", catPage, Math.max(1, (int)Math.ceil((double)catTotal/pageSize))));
            
            String search = (productSearch != null) ? productSearch.getText() : "";
            String sortVal = (productSort != null) ? productSort.getValue() : "ID (Oldest First)";
            String sortBy = "productId";
            String dir = "asc";

            if ("ID (Newest First)".equals(sortVal)) { sortBy = "productId"; dir = "desc"; }
            else if ("Name (A-Z)".equals(sortVal)) { sortBy = "name"; dir = "asc"; }
            else if ("Name (Z-A)".equals(sortVal)) { sortBy = "name"; dir = "desc"; }
            else if ("Price (Low to High)".equals(sortVal)) { sortBy = "price"; dir = "asc"; }
            else if ("Price (High to Low)".equals(sortVal)) { sortBy = "price"; dir = "desc"; }

            var prodPageObj = productService.getProducts(prodPage - 1, pageSize, sortBy, dir, search, null, null, null);
            productList.setAll(prodPageObj.getContent());
            prodTotal = (int) prodPageObj.getTotalElements();
            if (prodPageLabel != null) prodPageLabel.setText(String.format("Page %d of %d", prodPage, Math.max(1, (int)Math.ceil((double)prodTotal/pageSize))));
        } catch (Exception e) {
            System.err.println("[AdminController] Error loading initial data: " + e.getMessage());
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Data Loading Error", "Failed to load products/categories: " + e.getMessage()));
        } 
    }

    private void loadUsers() { 
        try { 
            String sortVal = (userSort != null) ? userSort.getValue() : "ID ASC";
            String sortBy = "userId";
            String dir = "asc";
            
            if ("ID DESC".equals(sortVal)) { sortBy = "userId"; dir = "desc"; }
            else if ("Name A-Z".equals(sortVal)) { sortBy = "name"; dir = "asc"; }
            else if ("Name Z-A".equals(sortVal)) { sortBy = "name"; dir = "desc"; }
            else if ("Role".equals(sortVal)) { sortBy = "role"; dir = "asc"; }

            var userPageObj = userService.getAllUsers(userPage - 1, pageSize, sortBy, dir, (userSearch != null ? userSearch.getText() : null));
            userList.setAll(userPageObj.getContent().stream().filter(u -> !"ADMIN".equals(u.getRole())).toList());
            userTotal = (int) userPageObj.getTotalElements();
            if (userPageLabel != null) userPageLabel.setText(String.format("Page %d of %d", userPage, Math.max(1, (int)Math.ceil((double)userTotal/pageSize))));
        } catch (Exception e) {} 
    }

    private void loadOrders() { applyOrderFilters(); }
    private void loadReviews() { applyReviewFilters(); }
    
    private void loadInventory() { 
        try {
            String search = (invSearch != null) ? invSearch.getText() : "";
            String sortVal = (invSort != null) ? invSort.getValue() : "ID (Oldest First)";
            String sortBy = "productId";
            String dir = "asc";

            if ("ID (Newest First)".equals(sortVal)) { sortBy = "productId"; dir = "desc"; }
            else if ("Stock (Low to High)".equals(sortVal)) { sortBy = "stockQuantity"; dir = "asc"; }
            else if ("Stock (High to Low)".equals(sortVal)) { sortBy = "stockQuantity"; dir = "desc"; }

            var invPageObj = productService.getProducts(invPage - 1, pageSize, sortBy, dir, search, null, null, null);
            productList.setAll(invPageObj.getContent()); 
            invTotal = (int) invPageObj.getTotalElements();
            if (invPageLabel != null) invPageLabel.setText(String.format("Page %d of %d", invPage, Math.max(1, (int)Math.ceil((double)invTotal/pageSize))));
        } catch (Exception e) {}
    }

    private <T> void addColumn(TableView<T> t, String name, double width, java.util.function.Function<T, ObservableValue<?>> f) {
        TableColumn<T, Object> col = new TableColumn<>(name); col.setPrefWidth(width);
        col.setCellValueFactory(data -> (ObservableValue<Object>) f.apply(data.getValue()));
        t.getColumns().add(col);
    }
    private void showAlert(Alert.AlertType t, String ti, String m) { Alert a = new Alert(t, m); a.setTitle(ti); a.show(); }
}
