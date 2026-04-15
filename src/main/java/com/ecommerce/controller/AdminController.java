package com.ecommerce.controller;

import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.model.Order;
import com.ecommerce.model.Review;
import com.ecommerce.service.ProductService;
import com.ecommerce.dao.CategoryDAO;
import com.ecommerce.dao.UserDAO;
import com.ecommerce.dao.OrderDAO;
import com.ecommerce.dao.ReviewDAO;
import com.ecommerce.dao.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import java.util.List;
import java.util.regex.Pattern;

/**
 * Enhanced AdminController provides comprehensive management for:
 * Products, Categories, Users (CRUD), Orders, Reviews, and Inventory.
 */
public class AdminController extends VBox {
    private final ProductService productService;
    private final CategoryDAO categoryDAO;
    private final UserDAO userDAO;
    private final OrderDAO orderDAO;
    private final ReviewDAO reviewDAO;
    private final com.ecommerce.service.ReportService reportService;

    // Observable Lists
    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private final ObservableList<Order> orderList = FXCollections.observableArrayList();
    private final ObservableList<Review> reviewList = FXCollections.observableArrayList();

    // Search & Sort Controls
    private TextField productSearch, catSearch, userSearch, orderSearch, reviewSearch, invSearch;
    private ComboBox<String> productSort, userSort, orderSort, reviewSort, invSort;

    // Fields for Product/Category Forms (keep for quick inline add)
    private List<Category> categories;

    public AdminController() {
        this.productService = new ProductService();
        this.categoryDAO = new CategoryDAO();
        this.userDAO = new UserDAO();
        this.orderDAO = new OrderDAO();
        this.reviewDAO = new ReviewDAO();
        this.reportService = new com.ecommerce.service.ReportService();
        
        this.setSpacing(20);
        this.getStyleClass().add("main-content");

        // Header
        VBox header = new VBox(5);
        Label title = new Label("⚙  System Administration");
        title.getStyleClass().addAll("content-title", "label-bright");
        Label subtitle = new Label("Full control over products, customers, and system monitoring");
        subtitle.getStyleClass().add("label-bright");
        header.getChildren().addAll(title, subtitle);

        // Sidebar-like Tab Bar
        HBox toggleBar = new HBox(10);
        toggleBar.setAlignment(Pos.CENTER_LEFT);
        toggleBar.setPadding(new Insets(0, 0, 10, 0));

        Button prodBtn = createTabBtn("📦 Products", true);
        Button catBtn = createTabBtn("📂 Categories", false);
        Button userBtn = createTabBtn("👥 Users", false);
        Button orderBtn = createTabBtn("📜 Orders", false);
        Button invBtn = createTabBtn("🏭 Inventory", false);
        Button reviewBtn = createTabBtn("⭐ Reviews", false);
        Button reportBtn = createTabBtn("📊 Reports", false);

        StackPane contentPane = new StackPane();
        VBox prodPanel = buildProductPanel();
        VBox catPanel = buildCategoryPanel();
        VBox userPanel = buildUserPanel();
        VBox orderPanel = buildOrderPanel();
        VBox reviewPanel = buildReviewPanel();
        VBox invPanel = buildInventoryPanel();
        VBox reportPanel = buildReportPanel();
        
        hideAll(catPanel, userPanel, orderPanel, reviewPanel, invPanel, reportPanel);

        contentPane.getChildren().addAll(prodPanel, catPanel, userPanel, orderPanel, reviewPanel, invPanel, reportPanel);
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        prodBtn.setOnAction(e -> switchTab(prodPanel, prodBtn, contentPane, toggleBar));
        catBtn.setOnAction(e -> switchTab(catPanel, catBtn, contentPane, toggleBar));
        userBtn.setOnAction(e -> { loadUsers(); switchTab(userPanel, userBtn, contentPane, toggleBar); });
        orderBtn.setOnAction(e -> { loadOrders(); switchTab(orderPanel, orderBtn, contentPane, toggleBar); });
        invBtn.setOnAction(e -> { loadInventory(); switchTab(invPanel, invBtn, contentPane, toggleBar); });
        reviewBtn.setOnAction(e -> { loadReviews(); switchTab(reviewPanel, reviewBtn, contentPane, toggleBar); });
        reportBtn.setOnAction(e -> switchTab(reportPanel, reportBtn, contentPane, toggleBar));

        toggleBar.getChildren().addAll(prodBtn, catBtn, userBtn, orderBtn, invBtn, reviewBtn, reportBtn);
        ScrollPane scrollPane = new ScrollPane(contentPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.getChildren().addAll(header, toggleBar, scrollPane);
        loadInitialData();
    }

    private Button createTabBtn(String text, boolean active) {
        Button b = new Button(text);
        if (active) b.getStyleClass().add("button-primary");
        return b;
    }

    private void switchTab(VBox panel, Button btn, StackPane container, HBox bar) {
        container.getChildren().forEach(c -> c.setVisible(false));
        panel.setVisible(true);
        bar.getChildren().forEach(b -> b.getStyleClass().remove("button-primary"));
        btn.getStyleClass().add("button-primary");
    }

    private void hideAll(VBox... panels) {
        for (VBox p : panels) p.setVisible(false);
    }

    // --- PANEL BUILDERS ---

    private VBox buildProductPanel() {
        VBox panel = new VBox(15);
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        Button add = new Button("➕ Add New Product"); add.getStyleClass().add("button-success");
        Button edit = new Button("✏ Edit Selected"); edit.getStyleClass().add("button-primary");
        Button del = new Button("🗑 Delete Selected"); del.getStyleClass().add("button-danger");
        Button seed = new Button("🌱 Seed Samples"); seed.getStyleClass().add("button-warning");
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        productSearch = new TextField(); productSearch.setPromptText("🔍 Search products...");
        productSearch.setPrefWidth(180);
        productSearch.textProperty().addListener((o, ov, nv) -> loadInitialData());

        productSort = new ComboBox<>(FXCollections.observableArrayList("ID (Oldest First)", "ID (Newest First)", "Name (A-Z)", "Name (Z-A)", "Price (Low to High)", "Price (High to Low)"));
        productSort.setValue("ID (Oldest First)");
        productSort.setOnAction(e -> loadInitialData());

        bar.getChildren().addAll(add, edit, del, seed, spacer, productSearch, productSort);

        TableView<Product> table = createProductTable();
        add.setOnAction(e -> handleProductDialog(null));
        edit.setOnAction(e -> { Product s = table.getSelectionModel().getSelectedItem(); if (s != null) handleProductDialog(s); });
        del.setOnAction(e -> { Product s = table.getSelectionModel().getSelectedItem(); if (s != null) handleDeleteProduct(s); });
        seed.setOnAction(e -> handleSeedData());

        panel.getChildren().addAll(bar, table);
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
        bar.getChildren().addAll(add, edit, del, spacer, catSearch);

        TableView<Category> table = createCategoryTable();
        add.setOnAction(e -> handleCategoryDialog(null));
        edit.setOnAction(e -> { Category s = table.getSelectionModel().getSelectedItem(); if (s != null) handleCategoryDialog(s); });
        del.setOnAction(e -> { Category s = table.getSelectionModel().getSelectedItem(); if (s != null) handleDeleteCategory(s); });

        panel.getChildren().addAll(bar, table);
        return panel;
    }

    private VBox buildUserPanel() {
        VBox panel = new VBox(15);
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        Button add = new Button("➕ Add New User"); add.getStyleClass().add("button-success");
        Button edit = new Button("✏ Edit Selected"); edit.getStyleClass().add("button-primary");
        Button del = new Button("🗑 Delete Selected"); del.getStyleClass().add("button-danger");
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        userSearch = new TextField(); userSearch.setPromptText("🔍 Search users...");
        userSort = new ComboBox<>(FXCollections.observableArrayList("ID ASC", "ID DESC", "Name A-Z", "Role"));
        userSort.setValue("ID ASC");
        bar.getChildren().addAll(add, edit, del, spacer, userSearch, userSort);

        TableView<User> table = createUserTable();
        add.setOnAction(e -> handleUserDialog(null));
        edit.setOnAction(e -> { User s = table.getSelectionModel().getSelectedItem(); if (s != null) handleUserDialog(s); });
        del.setOnAction(e -> { User s = table.getSelectionModel().getSelectedItem(); if (s != null) handleDeleteUser(s); });

        panel.getChildren().addAll(bar, table);
        return panel;
    }

    private VBox buildOrderPanel() {
        VBox panel = new VBox(15);
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("System Orders History"); lbl.getStyleClass().add("label-bright");
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        orderSearch = new TextField(); orderSearch.setPromptText("🔍 Search orders...");
        orderSort = new ComboBox<>(FXCollections.observableArrayList("Date (Newest)", "Date (Oldest)", "Amount (High)", "Status"));
        orderSort.setValue("Date (Newest)");
        bar.getChildren().addAll(lbl, spacer, orderSearch, orderSort);

        panel.getChildren().addAll(bar, createOrderTable());
        return panel;
    }

    private VBox buildReviewPanel() {
        VBox panel = new VBox(15);
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("Product Reviews Moderation"); lbl.getStyleClass().add("label-bright");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        reviewSearch = new TextField(); reviewSearch.setPromptText("🔍 Search reviews...");
        reviewSort = new ComboBox<>(FXCollections.observableArrayList("Rating (High)", "Rating (Low)", "Product Name"));
        reviewSort.setValue("Rating (High)");
        bar.getChildren().addAll(lbl, spacer, reviewSearch, reviewSort);

        panel.getChildren().addAll(bar, createReviewTable());
        return panel;
    }

    private VBox buildInventoryPanel() {
        VBox panel = new VBox(15);
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        Button edit = new Button("🔧 Edit Stock Level"); edit.getStyleClass().add("button-primary");
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        invSearch = new TextField(); invSearch.setPromptText("🔍 Search stock...");
        invSearch.textProperty().addListener((o, ov, nv) -> loadInitialData());

        invSort = new ComboBox<>(FXCollections.observableArrayList("ID (Oldest First)", "Stock (Low to High)", "Stock (High to Low)"));
        invSort.setValue("ID (Oldest First)");
        invSort.setOnAction(e -> loadInitialData());

        bar.getChildren().addAll(edit, spacer, invSearch, invSort);

        TableView<Product> table = createInventoryTable();
        edit.setOnAction(e -> { Product s = table.getSelectionModel().getSelectedItem(); if (s != null) handleInventoryDialog(s); });

        panel.getChildren().addAll(bar, table);
        return panel;
    }

    private VBox buildReportPanel() {
        VBox panel = new VBox(25);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(50));

        Label title = new Label("System Health & Diagnostic Reports");
        title.getStyleClass().add("content-title");
        title.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");

        VBox reportBox = new VBox(20);
        reportBox.setAlignment(Pos.CENTER);
        reportBox.setMaxWidth(500);
        reportBox.setPadding(new Insets(40));
        reportBox.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 15; -fx-border-color: #333; -fx-border-radius: 15;");

        Button perfBtn = new Button("🚀 Generate Performance Report");
        perfBtn.getStyleClass().add("button-primary");
        perfBtn.setPrefWidth(350);
        perfBtn.setPrefHeight(50);
        perfBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label perfDesc = new Label("Measures query speed across No-Index, Index, and Cache scenarios.");
        perfDesc.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 12px;");

        Button valBtn = new Button("✅ Generate Validation Report");
        valBtn.getStyleClass().add("button-success");
        valBtn.setPrefWidth(350);
        valBtn.setPrefHeight(50);
        valBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label valDesc = new Label("Runs automated sanity checks on CRUD, Constraints, and Checkout flows.");
        valDesc.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 12px;");

        ProgressIndicator progress = new ProgressIndicator();
        progress.setVisible(false);
        progress.setPrefSize(40, 40);

        perfBtn.setOnAction(e -> runReportTask("Performance Report", () -> reportService.generatePerformanceReport(), progress));
        valBtn.setOnAction(e -> runReportTask("Validation Report", () -> reportService.generateValidationReport(), progress));

        reportBox.getChildren().addAll(perfBtn, perfDesc, new Separator(), valBtn, valDesc, progress);
        panel.getChildren().addAll(title, reportBox);
        return panel;
    }

    private void runReportTask(String title, ReportTask task, ProgressIndicator progress) {
        progress.setVisible(true);
        new Thread(() -> {
            try {
                String path = task.execute();
                javafx.application.Platform.runLater(() -> {
                    progress.setVisible(false);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Report Generated");
                    alert.setHeaderText(title + " Complete");
                    alert.setContentText("Report saved successfully to:\n" + path);
                    alert.show();
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    progress.setVisible(false);
                    showAlert(Alert.AlertType.ERROR, "Generation Failed", ex.getMessage());
                });
            }
        }).start();
    }

    @FunctionalInterface
    interface ReportTask {
        String execute() throws Exception;
    }

    // --- TABLES ---

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
        if (catSearch != null) {
            catSearch.textProperty().addListener((o, ov, nv) -> {
                filtered.setPredicate(cat -> {
                    if (nv == null || nv.isEmpty()) return true;
                    return cat.getName().toLowerCase().contains(nv.toLowerCase());
                });
            });
        }
        TableView<Category> t = new TableView<>(filtered);
        addColumn(t, "ID", 60, d -> new SimpleObjectProperty<Object>(d.getCategoryId()));
        addColumn(t, "Name", 300, d -> new SimpleStringProperty(d.getName()));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

    private TableView<User> createUserTable() {
        FilteredList<User> filtered = new FilteredList<>(userList, p -> true);
        if (userSearch != null) {
            userSearch.textProperty().addListener((o, ov, nv) -> {
                filtered.setPredicate(u -> {
                    if (nv == null || nv.isEmpty()) return true;
                    String low = nv.toLowerCase();
                    return u.getName().toLowerCase().contains(low) || u.getEmail().toLowerCase().contains(low);
                });
            });
        }
        SortedList<User> sorted = new SortedList<>(filtered);
        if (userSort != null) {
            userSort.setOnAction(e -> {
                String val = userSort.getValue();
                if ("ID ASC".equals(val)) sorted.setComparator(java.util.Comparator.comparing(User::getUserId));
                else if ("ID DESC".equals(val)) sorted.setComparator(java.util.Comparator.comparing(User::getUserId).reversed());
                else if ("Name A-Z".equals(val)) sorted.setComparator(java.util.Comparator.comparing(User::getName));
                else if ("Role".equals(val)) sorted.setComparator(java.util.Comparator.comparing(User::getRole));
            });
            userSort.fireEvent(new javafx.event.ActionEvent());
        }

        TableView<User> t = new TableView<>(sorted);
        addColumn(t, "ID", 60, d -> new SimpleObjectProperty<Object>(d.getUserId()));
        addColumn(t, "Name", 200, d -> new SimpleStringProperty(d.getName()));
        addColumn(t, "Email", 250, d -> new SimpleStringProperty(d.getEmail()));
        addColumn(t, "Location", 150, d -> new SimpleStringProperty(d.getLocation()));
        addColumn(t, "Role", 100, d -> new SimpleStringProperty(d.getRole()));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

    private TableView<Order> createOrderTable() {
        FilteredList<Order> filtered = new FilteredList<>(orderList, p -> true);
        if (orderSearch != null) {
            orderSearch.textProperty().addListener((o, ov, nv) -> {
                filtered.setPredicate(ord -> {
                    if (nv == null || nv.isEmpty()) return true;
                    String low = nv.toLowerCase();
                    return ord.getUserName().toLowerCase().contains(low) || ord.getStatus().toLowerCase().contains(low);
                });
            });
        }
        SortedList<Order> sorted = new SortedList<>(filtered);
        if (orderSort != null) {
            orderSort.setOnAction(e -> {
                String val = orderSort.getValue();
                if ("Date (Newest)".equals(val)) sorted.setComparator(java.util.Comparator.comparing(Order::getOrderDate).reversed());
                else if ("Date (Oldest)".equals(val)) sorted.setComparator(java.util.Comparator.comparing(Order::getOrderDate));
                else if ("Amount (High)".equals(val)) sorted.setComparator(java.util.Comparator.comparing(Order::getTotalAmount).reversed());
                else if ("Status".equals(val)) sorted.setComparator(java.util.Comparator.comparing(Order::getStatus));
            });
            orderSort.fireEvent(new javafx.event.ActionEvent());
        }

        TableView<Order> t = new TableView<>(sorted);
        addColumn(t, "Order ID", 80, d -> new SimpleObjectProperty<Object>(d.getOrderId()));
        addColumn(t, "Customer", 200, d -> new SimpleStringProperty(d.getUserName()));
        addColumn(t, "Total", 100, d -> new SimpleStringProperty(String.format("$%.2f", d.getTotalAmount())));
        addColumn(t, "Date", 200, d -> new SimpleStringProperty(d.getOrderDate().toString().split("T")[0]));
        addColumn(t, "Status", 100, d -> new SimpleStringProperty(d.getStatus()));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

    private TableView<Review> createReviewTable() {
        FilteredList<Review> filtered = new FilteredList<>(reviewList, p -> true);
        if (reviewSearch != null) {
            reviewSearch.textProperty().addListener((o, ov, nv) -> {
                filtered.setPredicate(rev -> {
                    if (nv == null || nv.isEmpty()) return true;
                    String low = nv.toLowerCase();
                    return rev.getProductName().toLowerCase().contains(low) || rev.getUserName().toLowerCase().contains(low) || rev.getComment().toLowerCase().contains(low);
                });
            });
        }
        SortedList<Review> sorted = new SortedList<>(filtered);
        if (reviewSort != null) {
            reviewSort.setOnAction(e -> {
                String val = reviewSort.getValue();
                if ("Rating (High)".equals(val)) sorted.setComparator(java.util.Comparator.comparing(Review::getRating).reversed());
                else if ("Rating (Low)".equals(val)) sorted.setComparator(java.util.Comparator.comparing(Review::getRating));
                else if ("Product Name".equals(val)) sorted.setComparator(java.util.Comparator.comparing(Review::getProductName));
            });
            reviewSort.fireEvent(new javafx.event.ActionEvent());
        }

        TableView<Review> t = new TableView<>(sorted);
        addColumn(t, "Product", 200, d -> new SimpleStringProperty(d.getProductName()));
        addColumn(t, "User", 150, d -> new SimpleStringProperty(d.getUserName()));
        addColumn(t, "Rating", 80, d -> new SimpleStringProperty("⭐".repeat(d.getRating())));
        addColumn(t, "Comment", 400, d -> new SimpleStringProperty(d.getComment()));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

    private TableView<Product> createInventoryTable() {
        TableView<Product> t = new TableView<>(productList);
        addColumn(t, "ID", 60, d -> new SimpleObjectProperty<>(d.getProductId()));
        addColumn(t, "Product Name", 300, d -> new SimpleStringProperty(d.getName()));
        addColumn(t, "In Stock", 120, d -> new SimpleObjectProperty<>(d.getStockQuantity()));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

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
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", ex.getMessage().contains("empty") ? ex.getMessage() : "Invalid numeric values for Price or Stock.");
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveBtnType) {
                Product p = existing != null ? existing : new Product();
                p.setName(name.getText().trim()); p.setDescription(desc.getText().trim());
                p.setPrice(Double.parseDouble(price.getText()));
                p.setStockQuantity(Integer.parseInt(stock.getText()));
                String selectedCat = cat.getValue();
                categories.stream().filter(c -> c.getName().equals(selectedCat)).findFirst().ifPresent(c -> p.setCategoryId(c.getCategoryId()));
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            try {
                if (existing == null) productService.addProduct(p); else productService.updateProduct(p);
                loadInitialData();
            } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
        });
    }

    private void handleDeleteProduct(Product selected) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remove " + selected.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { productService.deleteProduct(selected.getProductId()); loadInitialData(); }
                catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
            }
        });
    }

    private void handleCategoryDialog(Category existing) {
        TextInputDialog dialog = new TextInputDialog(existing != null ? existing.getName() : "");
        dialog.setTitle(existing == null ? "Add Category" : "Edit Category");
        dialog.setHeaderText("Enter category name:");
        
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String name = dialog.getEditor().getText().trim();
            if (name.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Category name cannot be empty.");
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(name -> {
            try {
                if (existing == null) { Category c = new Category(); c.setName(name); categoryDAO.addCategory(c); }
                else { existing.setName(name); categoryDAO.updateCategory(existing); }
                loadInitialData();
            } catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
        });
    }

    private void handleDeleteCategory(Category selected) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remove category " + selected.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { categoryDAO.deleteCategory(selected.getCategoryId()); loadInitialData(); }
                catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
            }
        });
    }

    private void handleInventoryDialog(Product product) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(product.getStockQuantity()));
        dialog.setTitle("Update Stock");
        dialog.setHeaderText("Update stock for: " + product.getName());
        dialog.setContentText("Quantity on hand:");
        
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                int q = Integer.parseInt(dialog.getEditor().getText());
                if (q < 0) throw new Exception();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a non-negative whole number.");
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(qty -> {
            try {
                product.setStockQuantity(Integer.parseInt(qty));
                productService.updateProduct(product);
                loadInitialData();
            } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", "Invalid quantity"); }
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

        Button okButton = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String n = name.getText().trim();
            String e = email.getText().trim();
            if (!Pattern.matches("^[a-zA-Z\\s]+$", n)) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Name can only contain letters and spaces.");
                event.consume();
            } else if (!Pattern.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", e)) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a valid email address.");
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveBtnType) {
                String nameStr = name.getText().trim();
                String emailStr = email.getText().trim().toLowerCase();

                User u = existing != null ? existing : new User();
                u.setName(nameStr); u.setEmail(emailStr); u.setRole(role.getValue()); u.setLocation(location.getText().trim());

                if (existing == null) {
                    String firstName = nameStr.split("\\s+")[0];
                    String defaultPass = firstName + "@123";
                    String hashed = BCrypt.hashpw(defaultPass, BCrypt.gensalt());
                    u.setPassword(hashed);
                }
                return u;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(u -> {
            try {
                if (existing == null) userDAO.addUser(u); else userDAO.updateUser(u);
                loadUsers();
            } catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
        });
    }

    private void handleDeleteUser(User selected) {
        if ("ADMIN".equalsIgnoreCase(selected.getRole())) {
            showAlert(Alert.AlertType.WARNING, "Guard", "Administrator accounts cannot be removed from this console.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remove user " + selected.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { userDAO.deleteUser(selected.getUserId()); loadUsers(); }
                catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
            }
        });
    }

    private void handleSeedData() {
        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO Categories (name) VALUES ('Smartphones'), ('Laptops'), ('Audio'), ('Wearables'), ('Accessories') ON CONFLICT DO NOTHING");
            
            String seedProducts = "INSERT INTO Products (name, description, price, category_id) VALUES " +
                "('iPhone 15 Pro', 'Titanium build, A17 Pro chip', 999.00, (SELECT category_id FROM Categories WHERE name='Smartphones')), " +
                "('Samsung S24 Ultra', 'AI features, S-Pen included', 1199.00, (SELECT category_id FROM Categories WHERE name='Smartphones')), " +
                "('MacBook Air M3', 'Liquid Retina display, fanless design', 1099.00, (SELECT category_id FROM Categories WHERE name='Laptops')), " +
                "('Dell XPS 13', 'InfinityEdge touch display', 949.00, (SELECT category_id FROM Categories WHERE name='Laptops')), " +
                "('Sony WH-1000XM5', 'Industry-leading noise canceling', 349.00, (SELECT category_id FROM Categories WHERE name='Audio')), " +
                "('AirPods Pro 2', 'USB-C charging branch', 249.00, (SELECT category_id FROM Categories WHERE name='Audio')), " +
                "('Apple Watch Ultra 2', 'Rugged outdoor smartwatch', 799.00, (SELECT category_id FROM Categories WHERE name='Wearables')), " +
                "('Pixel Watch 2', 'Fitbit integration included', 349.00, (SELECT category_id FROM Categories WHERE name='Wearables')), " +
                "('Anker 737 PowerBank', '140W fast charging 24K', 149.00, (SELECT category_id FROM Categories WHERE name='Accessories')), " +
                "('Logitech MX Master 3S', 'Ergonomic wireless mouse', 99.00, (SELECT category_id FROM Categories WHERE name='Accessories')) " +
                "ON CONFLICT DO NOTHING";
            stmt.execute(seedProducts);
            
            stmt.execute("INSERT INTO Inventory (product_id, quantity_on_hand) " +
                "SELECT product_id, 25 FROM Products ON CONFLICT (product_id) DO NOTHING");
            
            loadInitialData(); showAlert(Alert.AlertType.INFORMATION, "Success", "Premium sample data populated!");
        } catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Seed Failed", e.getMessage()); }
    }

    // --- DATA LOADERS ---

    private void loadInitialData() {
        try {
            categories = categoryDAO.getAllCategories();
            categoryList.setAll(categories);
            
            String search = (productSearch != null) ? productSearch.getText() : "";
            String sort = (productSort != null) ? productSort.getValue() : "ID (Oldest First)";

            if (invSearch != null && !invSearch.getText().isEmpty()) {
                search = invSearch.getText();
            }
            if (invSort != null && invSort.isVisible() && !invSort.getValue().equals("ID (Oldest First)")) {
                sort = invSort.getValue();
            }
            
            productList.setAll(productService.searchProducts(search, null, 1, 500, sort));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadUsers() { try { userList.setAll(userDAO.getAllUsers().stream().filter(u -> !"ADMIN".equals(u.getRole())).toList()); } catch (SQLException e) {} }
    private void loadOrders() { try { orderList.setAll(orderDAO.getAllOrders()); } catch (SQLException e) {} }
    private void loadReviews() { try { reviewList.setAll(reviewDAO.getAllReviews()); } catch (SQLException e) {} }
    private void loadInventory() { loadInitialData(); }

    // --- HELPERS ---
    private <T> void addColumn(TableView<T> t, String name, double width, java.util.function.Function<T, ObservableValue<?>> factory) {
        TableColumn<T, Object> col = new TableColumn<>(name);
        col.setPrefWidth(width);
        col.setCellValueFactory(data -> {
            ObservableValue<?> val = factory.apply(data.getValue());
            return (ObservableValue<Object>) val;
        });
        t.getColumns().add(col);
    }
    private void showAlert(Alert.AlertType t, String ti, String m) { Alert a = new Alert(t, m); a.setTitle(ti); a.show(); }
}
