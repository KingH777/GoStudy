package org.example.fina;

import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.fina.FinanceRecord;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;



public class FinanceManager extends Application {

    // 数据库连接信息
    private static final String DB_URL = "jdbc:mysql://localhost:3306/financedb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "financedb";
    private static final String DB_USER = "root"; // 替换为你的MySQL用户名
    private static final String DB_PASS = "1234"; // 替换为你的MySQL密码


    // UI组件
    private TableView<FinanceRecord> tableView;
    private TextField incomeField;
    private TextField expenseField;

    private DatePicker datePicker;
    private TextField noteField;
    private Label balanceLabel;
    private static final String[] EXPENSE_CATEGORIES = {"三餐", "水电", "衣物", "出行", "其他"};
    private static final String[] INCOME_CATEGORIES = {"工资", "红包", "生活费", "其他"};

    // UI组件（在现有组件列表中添加）
    private ComboBox<String> expenseCategoryCombo;
    private ComboBox<String> incomeCategoryCombo;

    // 数据
    private final ObservableList<FinanceRecord> recordList = FXCollections.observableArrayList();


    @Override
    public void start(Stage primaryStage) {
        // 初始化数据库
        initDatabase();

        // 创建用户界面
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // 顶部菜单
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        // 中间表格区域
        tableView = createTableView();
        root.setCenter(tableView);

        // 底部数据录入区域
        GridPane inputPane = createInputPane();
        root.setBottom(inputPane);

        // 加载数据
        loadData();

        // 创建场景并显示
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("家庭理财系统");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 显示登录对话框
        showLoginDialog(primaryStage);
    }

    /**
     * 获取不指定数据库的连接（用于创建数据库）
     */
    private Connection getRootConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/mysql?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                DB_USER, DB_PASS);
    }


    /**
     * 初始化数据库
     */
    private void initDatabase() {
        try {
            // 加载MySQL数据库驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 首先尝试创建数据库（如果不存在）
            try (Connection rootConn = getRootConnection();
                 Statement stmt = rootConn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
                System.out.println("数据库已创建或已存在: " + DB_NAME);
            } catch (SQLException e) {
                showError("无法创建数据库", "确保MySQL服务正在运行且提供的用户有创建数据库的权限。\n错误: " + e.getMessage());
                e.printStackTrace();
                return; // 如果无法创建数据库，则退出初始化
            }

            // 连接到新创建的数据库并创建表
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                // 创建财务记录表
                stmt.execute("CREATE TABLE IF NOT EXISTS finance_records (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "income DOUBLE, " +
                        "expense DOUBLE, " +
                        "category VARCHAR(50), " +
                        "record_date VARCHAR(50), " +
                        "notes VARCHAR(200))");
                System.out.println("财务记录表已创建或已存在");

                // 创建账户表
                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "username VARCHAR(50) UNIQUE, " +
                        "password VARCHAR(50))");
                System.out.println("用户表已创建或已存在");

                // 检查是否有默认用户，如果没有，创建默认用户
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO users (username, password) VALUES('admin', '123')");
                    System.out.println("已创建默认用户: admin, 密码: 123");
                }

                System.out.println("数据库初始化完成");
            } catch (SQLException e) {
                showError("表创建失败", "无法创建必要的数据库表。\n错误: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            showError("找不到数据库驱动", "请确保MySQL JDBC驱动已添加到项目中。\n错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("数据库初始化失败", "初始化过程中发生未知错误。\n错误: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // 文件菜单
        Menu fileMenu = new Menu("文件");
        MenuItem exportItem = new MenuItem("导出数据");
        MenuItem exitItem = new MenuItem("退出");
        exitItem.setOnAction(e -> System.exit(0));
        fileMenu.getItems().addAll(exportItem, new SeparatorMenuItem(), exitItem);

        // 账户菜单
        Menu accountMenu = new Menu("账户");
        MenuItem changePassItem = new MenuItem("修改密码");
        changePassItem.setOnAction(e -> showChangePasswordDialog());
        accountMenu.getItems().add(changePassItem);

        // 数据菜单
        Menu dataMenu = new Menu("数据");
        MenuItem clearItem = new MenuItem("清空数据");
        clearItem.setOnAction(e -> showClearDataConfirmation());
        dataMenu.getItems().add(clearItem);

        // 帮助菜单
        Menu helpMenu = new Menu("帮助");
        MenuItem aboutItem = new MenuItem("关于");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, accountMenu, dataMenu, helpMenu);
        return menuBar;
    }

    private TableView<FinanceRecord> createTableView() {
        TableView<FinanceRecord> table = new TableView<>();

        // 创建列
        TableColumn<FinanceRecord, Integer> idColumn = new TableColumn<>("序号");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);

        TableColumn<FinanceRecord, Double> incomeColumn = new TableColumn<>("收入");
        incomeColumn.setCellValueFactory(new PropertyValueFactory<>("income"));
        incomeColumn.setPrefWidth(100);

        TableColumn<FinanceRecord, Double> expenseColumn = new TableColumn<>("支出");
        expenseColumn.setCellValueFactory(new PropertyValueFactory<>("expense"));
        expenseColumn.setPrefWidth(100);

        TableColumn<FinanceRecord, String> categoryColumn = new TableColumn<>("类别");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryColumn.setPrefWidth(100);

        TableColumn<FinanceRecord, String> dateColumn = new TableColumn<>("日期");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateColumn.setPrefWidth(150);

        TableColumn<FinanceRecord, String> noteColumn = new TableColumn<>("备注");
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
        noteColumn.setPrefWidth(200);

        table.getColumns().addAll(idColumn, incomeColumn, expenseColumn, categoryColumn,dateColumn, noteColumn);
        table.setItems(recordList);

        // 添加右键菜单
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("修改");
        MenuItem deleteItem = new MenuItem("删除");

        editItem.setOnAction(e -> {
            FinanceRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditDialog(selected);
            }
        });

        deleteItem.setOnAction(e -> {
            FinanceRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteRecord(selected);
            }
        });

        contextMenu.getItems().addAll(editItem, deleteItem);
        table.setContextMenu(contextMenu);

        return table;
    }

    private GridPane createInputPane() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(10);
        grid.setHgap(10);

        // 收入
        Label incomeLabel = new Label("收入:");
        incomeField = new TextField("0");
        incomeField.setPrefWidth(100);
        grid.add(incomeLabel, 0, 0);
        grid.add(incomeField, 1, 0);
        // 收入类别
        Label incomeCategoryLabel = new Label("收入类别:");
        incomeCategoryCombo = new ComboBox<>();
        incomeCategoryCombo.getItems().addAll(INCOME_CATEGORIES);
        incomeCategoryCombo.setValue("其他"); // 默认选择
        incomeCategoryCombo.setDisable(true); // 初始状态禁用
        grid.add(incomeCategoryLabel, 2, 0);
        grid.add(incomeCategoryCombo, 3, 0);

        // 启用收入类别选择当收入不为0
        incomeField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double value = Double.parseDouble(newVal.trim());
                incomeCategoryCombo.setDisable(value <= 0);
                if (value > 0) {
                    expenseField.setText("0");
                    expenseCategoryCombo.setDisable(true);
                }
            } catch (NumberFormatException e) {
                // 忽略非数字输入
            }
        });

        // 支出
        Label expenseLabel = new Label("支出:");
        expenseField = new TextField("0");
        expenseField.setPrefWidth(100);
        grid.add(expenseLabel, 2, 0);
        grid.add(expenseField, 3, 0);

        // 支出类别
        Label expenseCategoryLabel = new Label("支出类别:");
        expenseCategoryCombo = new ComboBox<>();
        expenseCategoryCombo.getItems().addAll(EXPENSE_CATEGORIES);
        expenseCategoryCombo.setValue("其他"); // 默认选择
        expenseCategoryCombo.setDisable(true); // 初始状态禁用
        grid.add(expenseCategoryLabel, 2, 1);
        grid.add(expenseCategoryCombo, 3, 1);

        // 启用支出类别选择当支出不为0
        expenseField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double value = Double.parseDouble(newVal.trim());
                expenseCategoryCombo.setDisable(value <= 0);
                if (value > 0) {
                    incomeField.setText("0");
                    incomeCategoryCombo.setDisable(true);
                }
            } catch (NumberFormatException e) {
                // 忽略非数字输入
            }
        });

        // 日期
        Label dateLabel = new Label("日期:");
        datePicker = new DatePicker(LocalDate.now());
        grid.add(dateLabel, 0, 1);
        grid.add(datePicker, 1, 1);

        // 备注
        Label noteLabel = new Label("备注:");
        noteField = new TextField();
        grid.add(noteLabel, 2, 1);
        grid.add(noteField, 3, 1);

        // 添加按钮
        Button addButton = new Button("添加记录");
        addButton.setOnAction(e -> addRecord());

        // 统计按钮
        Button statsButton = new Button("统计");
        statsButton.setOnAction(e -> showStatistics());

        HBox buttons = new HBox(10, addButton, statsButton);
        grid.add(buttons, 0, 3, 2, 1);

        // 余额显示
        balanceLabel = new Label();
        updateBalanceLabel();
        grid.add(balanceLabel, 2, 3, 2, 1);

        return grid;
    }

    private void loadData() {
        recordList.clear();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM finance_records ORDER BY id")) {

            while (rs.next()) {
                recordList.add(new FinanceRecord(
                        rs.getInt("id"),
                        rs.getDouble("income"),
                        rs.getDouble("expense"),
                        rs.getString("category"),
                        rs.getString("record_date"),
                        rs.getString("notes")
                ));
            }

            updateBalanceLabel();
        } catch (SQLException e) {
            showError("加载数据失败", e.getMessage());
            e.printStackTrace();
        }
    }

    private void addRecord() {
        try {
            double income = Double.parseDouble(incomeField.getText().trim());
            double expense = Double.parseDouble(expenseField.getText().trim());
            String date = datePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String note = noteField.getText().trim();
            String category;

            if (income > 0 && expense == 0) {
                category = incomeCategoryCombo.getValue();
            } else if (expense > 0 && income == 0) {
                category = expenseCategoryCombo.getValue();
            } else {
                showError("输入错误", "收入和支出不能同时为0或同时有值");
                return;
            }

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO finance_records (income, expense, category, record_date, notes) VALUES (?, ?, ?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setDouble(1, income);
                pstmt.setDouble(2, expense);
                pstmt.setString(3, category);
                pstmt.setString(4, date);
                pstmt.setString(5, note);
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        // 假设FinanceRecord类已经更新，包含category字段
                        recordList.add(new FinanceRecord(id, income, expense, category, date, note));
                    }
                }
            }

            // 清空输入框
            incomeField.setText("0");
            expenseField.setText("0");
            noteField.clear();
            incomeCategoryCombo.setValue("其他");
            expenseCategoryCombo.setValue("其他");
            incomeCategoryCombo.setDisable(true);
            expenseCategoryCombo.setDisable(true);

            updateBalanceLabel();
        } catch (NumberFormatException e) {
            showError("输入错误", "收入和支出必须是数字");
        } catch (SQLException e) {
            showError("添加记录失败", e.getMessage());
            e.printStackTrace();
        }
    }


    private void deleteRecord(FinanceRecord record) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除记录");
        alert.setContentText("确定要删除选中的记录吗？");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM finance_records WHERE id = ?")) {

                pstmt.setInt(1, record.getId());
                pstmt.executeUpdate();

                recordList.remove(record);
                updateBalanceLabel();
            } catch (SQLException e) {
                showError("删除记录失败", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showEditDialog(FinanceRecord record) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("修改记录");
        dialog.setHeaderText("修改第 " + record.getId() + " 行数据");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField incomeField = new TextField(String.valueOf(record.getIncome()));
        TextField expenseField = new TextField(String.valueOf(record.getExpense()));
        ComboBox<String> categoryCombo = new ComboBox<>();
        if (record.getIncome() > 0 && record.getExpense() == 0) {
            categoryCombo.getItems().addAll(INCOME_CATEGORIES);
        } else {
            categoryCombo.getItems().addAll(EXPENSE_CATEGORIES);
        }
        categoryCombo.setValue(record.getCategory());

        DatePicker datePicker = new DatePicker(LocalDate.parse(record.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        TextField noteField = new TextField(record.getNote());

        grid.add(new Label("收入:"), 0, 0);
        grid.add(incomeField, 1, 0);
        grid.add(new Label("支出:"), 0, 1);
        grid.add(expenseField, 1, 1);
        grid.add(new Label("类别:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("日期:"), 0, 3);
        grid.add(datePicker, 1, 3);
        grid.add(new Label("备注:"), 0, 4);
        grid.add(noteField, 1, 4);

        // 添加收入和支出的监听器以更新类别选择
        incomeField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double value = Double.parseDouble(newVal.trim());
                if (value > 0) {
                    expenseField.setText("0");
                    categoryCombo.getItems().clear();
                    categoryCombo.getItems().addAll(INCOME_CATEGORIES);
                    categoryCombo.setValue(INCOME_CATEGORIES[0]);
                }
            } catch (NumberFormatException e) {
                // 忽略非数字输入
            }
        });

        expenseField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double value = Double.parseDouble(newVal.trim());
                if (value > 0) {
                    incomeField.setText("0");
                    categoryCombo.getItems().clear();
                    categoryCombo.getItems().addAll(EXPENSE_CATEGORIES);
                    categoryCombo.setValue(EXPENSE_CATEGORIES[0]);
                }
            } catch (NumberFormatException e) {
                // 忽略非数字输入
            }
        });

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            try {
                double income = Double.parseDouble(incomeField.getText().trim());
                double expense = Double.parseDouble(expenseField.getText().trim());
                String date = datePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String note = noteField.getText().trim();
                String category = null;

                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                             "UPDATE finance_records SET income = ?, expense = ?, category=?,record_date = ?, notes = ? WHERE id = ?")) {

                    pstmt.setDouble(1, income);
                    pstmt.setDouble(2, expense);
                    pstmt.setString(3, category);
                    pstmt.setString(4, date);
                    pstmt.setString(5, note);
                    pstmt.setInt(6, record.getId());
                    pstmt.executeUpdate();

                    // 更新列表中的记录
                    record.setIncome(income);
                    record.setExpense(expense);
                    record.setCategory(category);
                    record.setDate(date);
                    record.setNote(note);

                    // 刷新表格
                    tableView.refresh();
                    updateBalanceLabel();
                }
            } catch (NumberFormatException e) {
                showError("输入错误", "收入和支出必须是数字");
            } catch (SQLException e) {
                showError("更新记录失败", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showChangePasswordDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("修改密码");
        dialog.setHeaderText("请输入新密码");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        PasswordField oldPassField = new PasswordField();
        PasswordField newPassField = new PasswordField();
        PasswordField confirmPassField = new PasswordField();

        grid.add(new Label("原密码:"), 0, 0);
        grid.add(oldPassField, 1, 0);
        grid.add(new Label("新密码:"), 0, 1);
        grid.add(newPassField, 1, 1);
        grid.add(new Label("确认密码:"), 0, 2);
        grid.add(confirmPassField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String oldPass = oldPassField.getText();
            String newPass = newPassField.getText();
            String confirmPass = confirmPassField.getText();

            if (!newPass.equals(confirmPass)) {
                showError("密码错误", "两次输入的新密码不一致");
                return;
            }

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT password FROM users WHERE id = 1")) {

                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    if (!dbPassword.equals(oldPass)) {
                        showError("密码错误", "原密码不正确");
                        return;
                    }

                    try (PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET password = ? WHERE id = 1")) {
                        pstmt.setString(1, newPass);
                        pstmt.executeUpdate();
                        showInfo("成功", "密码修改成功");
                    }
                }
            } catch (SQLException e) {
                showError("修改密码失败", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showClearDataConfirmation() {
        PasswordDialog dialog = new PasswordDialog();
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            String password = result.get();

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT password FROM users WHERE id = 1")) {

                if (rs.next() && password.equals(rs.getString("password"))) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("确认清空");
                    confirmAlert.setHeaderText("清空所有数据");
                    confirmAlert.setContentText("确定要清空所有数据吗？此操作不可撤销。");

                    Optional<ButtonType> confirmResult = confirmAlert.showAndWait();
                    if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                        stmt.executeUpdate("DELETE FROM finance_records");
                        // MySQL中重置自增ID的方式
                        stmt.executeUpdate("ALTER TABLE finance_records AUTO_INCREMENT = 1");
                        recordList.clear();
                        updateBalanceLabel();
                        showInfo("成功", "数据已成功清空");
                    }
                } else {
                    showError("密码错误", "密码不正确");
                }
            } catch (SQLException e) {
                showError("清空数据失败", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showLoginDialog(Stage primaryStage) {
        PasswordDialog dialog = new PasswordDialog();
        dialog.setTitle("登录");
        dialog.setHeaderText("请输入密码");

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            String password = result.get();

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT password FROM users WHERE id = 1")) {

                if (rs.next() && password.equals(rs.getString("password"))) {
                    // 密码正确，继续执行
                    return;
                } else {
                    showError("密码错误", "密码不正确");
                    System.exit(0);
                }
            } catch (SQLException e) {
                showError("登录失败", e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            // 用户取消登录
            System.exit(0);
        }
    }

    private void showStatistics() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT SUM(income) as total_income, SUM(expense) as total_expense FROM finance_records")) {

            if (rs.next()) {
                double totalIncome = rs.getDouble("total_income");
                double totalExpense = rs.getDouble("total_expense");
                double balance = totalIncome - totalExpense;

                // 创建统计对话框
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("财务统计");
                dialog.setHeaderText("账户统计信息");
                dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

                TabPane tabPane = new TabPane();

                // 总览选项卡
                Tab overviewTab = new Tab("总览");
                VBox overviewContent = new VBox(10);
                overviewContent.setPadding(new Insets(10));
                overviewContent.getChildren().addAll(
                        new Label(String.format("总收入: %.2f 元", totalIncome)),
                        new Label(String.format("总支出: %.2f 元", totalExpense)),
                        new Label(String.format("当前余额: %.2f 元", balance))
                );
                overviewTab.setContent(overviewContent);

                // 收入类别统计选项卡
                Tab incomeTab = new Tab("收入统计");
                incomeTab.setContent(createCategoryChart(conn, true, totalIncome));

                // 支出类别统计选项卡
                Tab expenseTab = new Tab("支出统计");
                expenseTab.setContent(createCategoryChart(conn, false, totalExpense));

                tabPane.getTabs().addAll(overviewTab, incomeTab, expenseTab);

                dialog.getDialogPane().setContent(tabPane);
                dialog.getDialogPane().setPrefSize(600, 400);

                dialog.showAndWait();
            }
        } catch (SQLException e) {
            showError("统计失败", e.getMessage());
            e.printStackTrace();
        }
    }

    // 创建类别占比图表
    private VBox createCategoryChart(Connection conn, boolean isIncome, double total) throws SQLException {
        VBox chartBox = new VBox(10);
        chartBox.setPadding(new Insets(10));

        // 准备数据
        Map<String, Double> categoryData = new HashMap<>();
        String[] categories = isIncome ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;

        // 初始化所有类别为0
        for (String category : categories) {
            categoryData.put(category, 0.0);
        }

        // 查询各类别数据
        String query = isIncome
                ? "SELECT category, SUM(income) as amount FROM finance_records WHERE income > 0 GROUP BY category"
                : "SELECT category, SUM(expense) as amount FROM finance_records WHERE expense > 0 GROUP BY category";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String category = rs.getString("category");
                double amount = rs.getDouble("amount");
                categoryData.put(category, amount);
            }
        }

        // 创建饼图
        PieChart pieChart = new PieChart();
        pieChart.setTitle(isIncome ? "收入类别占比" : "支出类别占比");

        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            if (entry.getValue() > 0) {
                double percentage = (entry.getValue() / total) * 100;
                PieChart.Data slice = new PieChart.Data(
                        String.format("%s (%.1f%%)", entry.getKey(), percentage),
                        entry.getValue()
                );
                pieChart.getData().add(slice);
            }
        }

        // 添加图例标签
        for (PieChart.Data data : pieChart.getData()) {
            data.getNode().setOnMouseEntered(e -> {
                data.getNode().setStyle("-fx-pie-color: derive(" + data.getNode().getStyle() + ", 20%);");
            });
            data.getNode().setOnMouseExited(e -> {
                data.getNode().setStyle("");
            });
        }

        // 创建详细数据表格
        TableView<CategoryItem> detailTable = new TableView<>();

        TableColumn<CategoryItem, String> categoryCol = new TableColumn<>("类别");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<CategoryItem, Double> amountCol = new TableColumn<>("金额");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(col -> new TableCell<CategoryItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f 元", item));
                }
            }
        });

        TableColumn<CategoryItem, Double> percentCol = new TableColumn<>("占比");
        percentCol.setCellValueFactory(new PropertyValueFactory<>("percent"));
        percentCol.setCellFactory(col -> new TableCell<CategoryItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f%%", item));
                }
            }
        });

        detailTable.getColumns().addAll(categoryCol, amountCol, percentCol);

        ObservableList<CategoryItem> items = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            if (total > 0) {
                double percent = (entry.getValue() / total) * 100;
                items.add(new CategoryItem(entry.getKey(), entry.getValue(), percent));
            }
        }
        detailTable.setItems(items);

        chartBox.getChildren().addAll(pieChart, detailTable);
        return chartBox;
    }

    // 类别数据项类
    private static class CategoryItem {
        private final SimpleStringProperty category;
        private final SimpleDoubleProperty amount;
        private final SimpleDoubleProperty percent;

        public CategoryItem(String category, double amount, double percent) {
            this.category = new SimpleStringProperty(category);
            this.amount = new SimpleDoubleProperty(amount);
            this.percent = new SimpleDoubleProperty(percent);
        }

        public String getCategory() { return category.get(); }
        public double getAmount() { return amount.get(); }
        public double getPercent() { return percent.get(); }

        public SimpleStringProperty categoryProperty() { return category; }
        public SimpleDoubleProperty amountProperty() { return amount; }
        public SimpleDoubleProperty percentProperty() { return percent; }
    }
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于");
        alert.setHeaderText("家庭理财系统");
        alert.setContentText("版本: 1.0.0\n作者: KingH\n\n现代化的家庭财务管理工具");
        alert.showAndWait();
    }

    private void updateBalanceLabel() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT SUM(income) as total_income, SUM(expense) as total_expense FROM finance_records")) {

            if (rs.next()) {
                double totalIncome = rs.getDouble("total_income");
                double totalExpense = rs.getDouble("total_expense");
                double balance = totalIncome - totalExpense;
                balanceLabel.setText(String.format("当前余额: %.2f 元", balance));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("信息");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * 密码输入对话框
     */
    private class PasswordDialog extends Dialog<String> {
        private PasswordField passwordField;

        public PasswordDialog() {
            setTitle("密码");
            setHeaderText("请输入密码");

            ButtonType loginButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
            getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            passwordField = new PasswordField();
            passwordField.setPromptText("密码");

            grid.add(new Label("密码:"), 0, 0);
            grid.add(passwordField, 1, 0);

            getDialogPane().setContent(grid);

            setResultConverter(dialogButton -> {
                if (dialogButton == loginButtonType) {
                    return passwordField.getText();
                }
                return null;
            });
        }
    }

}