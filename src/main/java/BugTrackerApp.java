import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

public class BugTrackerApp extends Application {
    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd");

    private final AuthService auth = new AuthService();
    private final ObservableList<Bug> bugs = FXCollections.observableArrayList();

    private Stage stage;
    private User me;
    private TableView<Bug> table;
    private TextArea details;
    private ImageView preview;
    private Label previewLabel;
    private FilteredList<Bug> filtered;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Bug Tracker");
        showLogin();
        stage.show();
    }

    private void showLogin() {
        TextField user = new TextField();
        user.setPromptText("Username");
        PasswordField pass = new PasswordField();
        pass.setPromptText("Password");
        Button signIn = new Button("Sign In");

        signIn.setOnAction(e -> {
            User u = auth.login(user.getText().trim(), pass.getText());
            if (u == null) {
                alert(Alert.AlertType.ERROR, "Invalid credentials.");
                return;
            }
            me = u;
            showDashboard();
        });

        VBox root = new VBox(12, new Label("Bug Tracker"), user, pass, signIn);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#151a22;-fx-text-fill:white;");
        Scene scene = new Scene(root, 400, 260);
        stage.setScene(scene);
    }

    private void showDashboard() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(col("ID", "id", Integer.class));
        table.getColumns().add(col("Name", "bugName", String.class));
        table.getColumns().add(col("Status", "status", String.class));
        table.getColumns().add(col("Priority", "priority", String.class));
        table.getColumns().add(col("Assigned", "assignedTo", String.class));
        table.getColumns().add(col("Reporter", "reportedBy", String.class));

        bugs.setAll(BugService.getAllBugs());
        filtered = new FilteredList<>(bugs, b -> true);
        SortedList<Bug> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        TextField search = new TextField();
        search.setPromptText("Search...");
        search.textProperty().addListener((o, a, q) -> {
            String s = q == null ? "" : q.toLowerCase().trim();
            filtered.setPredicate(b -> s.isEmpty() ||
                    String.valueOf(b.getId()).contains(s) ||
                    hit(b.getBugName(), s) || hit(b.getStatus(), s) || hit(b.getAssignedTo(), s) || hit(b.getReportedBy(), s));
        });

        details = new TextArea("Select a bug.");
        details.setEditable(false);
        details.setPrefRowCount(6);
        preview = new ImageView();
        preview.setFitWidth(340);
        preview.setFitHeight(220);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        previewLabel = new Label("No screenshot");
        VBox previewBox = new VBox(6, new Label("Screenshot Preview"), previewLabel, preview);
        previewBox.setPadding(new Insets(8));

        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showBug(b));

        HBox actions = new HBox(8,
                btn("Refresh", this::refresh),
                btn("Report", this::reportBug),
                btn("Assign", this::assignBug),
                btn("Status", this::updateStatus),
                btn("Screenshot", this::attachScreenshot),
                btn("Insights", this::showInsights),
                btn("Users", this::manageUsers),
                btn("Logout", this::showLogin)
        );
        actions.setAlignment(Pos.CENTER_LEFT);

        Label head = new Label("Welcome " + me.getUsername() + " (" + me.getRole() + ")");
        HBox bottom = new HBox(10, details, previewBox);
        HBox.setHgrow(details, Priority.ALWAYS);

        VBox root = new VBox(10, head, search, table, actions, bottom);
        root.setPadding(new Insets(14));

        Scene scene = new Scene(root, 1200, 760);
        stage.setScene(scene);
    }

    private <T> TableColumn<Bug, T> col(String title, String prop, Class<T> type) {
        TableColumn<Bug, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        return c;
    }

    private Button btn(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        return b;
    }

    private void refresh() {
        bugs.setAll(BugService.getAllBugs());
        showBug(table.getSelectionModel().getSelectedItem());
    }

    private Bug selectedBug() {
        Bug b = table.getSelectionModel().getSelectedItem();
        if (b == null) {
            alert(Alert.AlertType.WARNING, "Select a bug first.");
        }
        return b;
    }

    private void reportBug() {
        if (!(me instanceof Tester) && !(me instanceof Admin)) {
            alert(Alert.AlertType.WARNING, "Only Tester/Admin can report.");
            return;
        }

        Dialog<Bug> d = new Dialog<>();
        d.initOwner(stage);
        d.initModality(Modality.APPLICATION_MODAL);
        d.setTitle("Report Bug");
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField name = new TextField();
        TextField project = new TextField();
        TextArea desc = new TextArea();
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("Open", "In Progress", "Closed"));
        status.setValue("Open");
        DatePicker date = new DatePicker(LocalDate.now());

        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8);
        g.addRow(0, new Label("Name"), name);
        g.addRow(1, new Label("Project"), project);
        g.addRow(2, new Label("Description"), desc);
        g.addRow(3, new Label("Status"), status);
        g.addRow(4, new Label("Date"), date);
        d.getDialogPane().setContent(g);

        d.setResultConverter(bt -> {
            if (bt != save || name.getText().trim().isEmpty() || project.getText().trim().isEmpty()) {
                return null;
            }
            int id = BugService.getAllBugs().stream().mapToInt(Bug::getId).max().orElse(0) + 1;
            Date bugDate = Date.from(date.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
            return new Bug(id, name.getText().trim(), desc.getText().trim(), "UI", "Medium", "Major",
                    project.getText().trim(), bugDate, status.getValue(), "", me.getUsername(), "");
        });

        d.showAndWait().ifPresent(b -> {
            BugService.addBug(b);
            refresh();
        });
    }

    private void assignBug() {
        if (!(me instanceof Tester) && !(me instanceof Admin)) {
            alert(Alert.AlertType.WARNING, "Only Tester/Admin can assign.");
            return;
        }
        Bug b = selectedBug();
        if (b == null) return;

        List<String> devs = new ArrayList<>();
        for (User u : UserService.getAllUsers()) if (u instanceof Developer) devs.add(u.getUsername());
        if (devs.isEmpty()) {
            alert(Alert.AlertType.WARNING, "No developers found.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(devs.get(0), devs);
        dialog.setTitle("Assign Bug");
        dialog.setHeaderText("Assign bug #" + b.getId());
        dialog.showAndWait().ifPresent(dev -> {
            b.setAssignedTo(dev);
            BugService.saveBugsToFile();
            refresh();
        });
    }

    private void updateStatus() {
        if (!(me instanceof Developer)) {
            alert(Alert.AlertType.WARNING, "Only Developer can update status.");
            return;
        }
        Bug b = selectedBug();
        if (b == null) return;
        if (!me.getUsername().equalsIgnoreCase(b.getAssignedTo())) {
            alert(Alert.AlertType.ERROR, "Bug is not assigned to you.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>("In Progress", "In Progress", "Closed");
        dialog.setTitle("Update Status");
        dialog.setHeaderText("Bug #" + b.getId());
        dialog.showAndWait().ifPresent(status -> {
            b.setStatus(status);
            BugService.saveBugsToFile();
            refresh();
        });
    }

    private void attachScreenshot() {
        Bug b = selectedBug();
        if (b == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Screenshot");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File f = fc.showOpenDialog(stage);
        if (f == null) return;

        b.setScreenshotPath(f.getAbsolutePath());
        BugService.saveBugsToFile();
        refresh();
        showBug(b);
    }

    private void showBug(Bug b) {
        details.setText(format(b));
        preview.setImage(null);
        previewLabel.setText("No screenshot");
        if (b == null || b.getScreenshotPath() == null || b.getScreenshotPath().trim().isEmpty()) return;

        File f = new File(b.getScreenshotPath().trim());
        if (!f.exists() || !f.isFile()) {
            previewLabel.setText("Screenshot file not found");
            return;
        }

        try {
            preview.setImage(new Image(f.toURI().toString(), true));
            previewLabel.setText(f.getName());
        } catch (Exception ex) {
            previewLabel.setText("Unable to load image");
        }
    }

    private void showInsights() {
        if (!(me instanceof PM) && !(me instanceof Admin)) {
            alert(Alert.AlertType.WARNING, "Only PM/Admin can view insights.");
            return;
        }

        ChoiceDialog<String> menu = new ChoiceDialog<>("Bug Counts", "Bug Counts", "Developer Performance", "Tester Performance");
        menu.setTitle("Insights & Reports");
        menu.setHeaderText("Choose report type");
        Optional<String> choice = menu.showAndWait();
        
        if (choice.isEmpty()) return;

        switch (choice.get()) {
            case "Bug Counts" -> viewBugCounts();
            case "Developer Performance" -> viewDeveloperPerformance();
            case "Tester Performance" -> viewTesterPerformance();
        }
    }

    private void viewBugCounts() {
        int open = 0, progress = 0, closed = 0;
        for (Bug b : BugService.getAllBugs()) {
            if ("Open".equalsIgnoreCase(b.getStatus())) open++;
            else if ("In Progress".equalsIgnoreCase(b.getStatus())) progress++;
            else if ("Closed".equalsIgnoreCase(b.getStatus()) || "Complete".equalsIgnoreCase(b.getStatus())) closed++;
        }

        alert(Alert.AlertType.INFORMATION,
                "Open: " + open + "\nIn Progress: " + progress + "\nClosed: " + closed);
    }

    private void viewDeveloperPerformance() {
        List<Bug> bugs = BugService.getAllBugs();
        List<User> users = UserService.getAllUsers();

        Map<String, Integer> assigned = new HashMap<>();
        Map<String, Integer> resolved = new HashMap<>();

        for (Bug b : bugs) {
            if (b.getAssignedTo() != null && !b.getAssignedTo().isEmpty()) {
                assigned.put(b.getAssignedTo(), assigned.getOrDefault(b.getAssignedTo(), 0) + 1);
                if ("Closed".equalsIgnoreCase(b.getStatus()) || "Complete".equalsIgnoreCase(b.getStatus())) {
                    resolved.put(b.getAssignedTo(), resolved.getOrDefault(b.getAssignedTo(), 0) + 1);
                }
            }
        }

        StringBuilder report = new StringBuilder("=== DEVELOPER PERFORMANCE REPORT ===\n\n");
        boolean found = false;
        for (User u : users) {
            if ("Developer".equalsIgnoreCase(u.getRole())) {
                found = true;
                int total = assigned.getOrDefault(u.getUsername(), 0);
                int done = resolved.getOrDefault(u.getUsername(), 0);
                double rate = total == 0 ? 0 : (done * 100.0 / total);
                report.append(String.format("%s:\n  Assigned: %d\n  Resolved: %d\n  Completion: %.1f%%\n\n",
                        u.getUsername(), total, done, rate));
            }
        }
        
        if (!found) report.append("No developers found.");

        TextArea reportArea = new TextArea(report.toString());
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setPrefRowCount(15);
        reportArea.setPrefColumnCount(50);

        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("Developer Performance");
        dialog.getDialogPane().setContent(reportArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    private void viewTesterPerformance() {
        List<Bug> bugs = BugService.getAllBugs();
        List<User> users = UserService.getAllUsers();

        Map<String, Integer> reported = new HashMap<>();
        Map<String, Integer> verified = new HashMap<>();

        for (Bug b : bugs) {
            if (b.getReportedBy() != null && !b.getReportedBy().isEmpty()) {
                reported.put(b.getReportedBy(), reported.getOrDefault(b.getReportedBy(), 0) + 1);
                if ("Closed".equalsIgnoreCase(b.getStatus()) || "Complete".equalsIgnoreCase(b.getStatus())) {
                    verified.put(b.getReportedBy(), verified.getOrDefault(b.getReportedBy(), 0) + 1);
                }
            }
        }

        StringBuilder report = new StringBuilder("=== TESTER PERFORMANCE REPORT ===\n\n");
        boolean found = false;
        for (User u : users) {
            if ("Tester".equalsIgnoreCase(u.getRole())) {
                found = true;
                int total = reported.getOrDefault(u.getUsername(), 0);
                int closed = verified.getOrDefault(u.getUsername(), 0);
                double rate = total == 0 ? 0 : (closed * 100.0 / total);
                report.append(String.format("%s:\n  Reported: %d\n  Closed: %d\n  Closure Rate: %.1f%%\n\n",
                        u.getUsername(), total, closed, rate));
            }
        }
        
        if (!found) report.append("No testers found.");

        TextArea reportArea = new TextArea(report.toString());
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setPrefRowCount(15);
        reportArea.setPrefColumnCount(50);

        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("Tester Performance");
        dialog.getDialogPane().setContent(reportArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    private void manageUsers() {
        if (!(me instanceof Admin)) {
            alert(Alert.AlertType.WARNING, "Only Admin can manage users.");
            return;
        }

        ChoiceDialog<String> menu = new ChoiceDialog<>("Add", "Add", "Update", "Delete");
        menu.setTitle("Users");
        menu.setHeaderText("Choose action");
        Optional<String> choice = menu.showAndWait();
        if (choice.isEmpty()) return;

        TextInputDialog userDialog = new TextInputDialog();
        userDialog.setHeaderText("Username");
        Optional<String> username = userDialog.showAndWait();
        if (username.isEmpty() || username.get().trim().isEmpty()) return;

        String u = username.get().trim();
        switch (choice.get()) {
            case "Delete" -> UserService.deleteUser(u);
            case "Add", "Update" -> {
                TextInputDialog pass = new TextInputDialog();
                pass.setHeaderText(choice.get().equals("Add") ? "Password" : "New Password");
                Optional<String> p = pass.showAndWait();
                if (p.isEmpty() || p.get().trim().isEmpty()) return;

                ChoiceDialog<String> role = new ChoiceDialog<>("tester", "tester", "developer", "pm", "admin");
                role.setHeaderText(choice.get().equals("Add") ? "Role" : "New Role");
                Optional<String> r = role.showAndWait();
                if (r.isEmpty()) return;

                if (choice.get().equals("Add")) UserService.addUser(u, p.get().trim(), r.get());
                else UserService.updateUser(u, p.get().trim(), r.get());
            }
        }
        UserService.saveUsersToFile();
    }

    private String format(Bug b) {
        if (b == null) return "Select a bug.";
        return "ID: " + b.getId() + "\n"
                + "Name: " + b.getBugName() + "\n"
                + "Description: " + b.getDescription() + "\n"
                + "Type: " + b.getBugType() + "\n"
                + "Priority: " + b.getPriority() + "\n"
                + "Level: " + b.getBugLevel() + "\n"
                + "Project: " + b.getProjectName() + "\n"
                + "Date: " + (b.getBugDate() == null ? "" : DATE.format(b.getBugDate())) + "\n"
                + "Status: " + b.getStatus() + "\n"
                + "Assigned To: " + b.getAssignedTo() + "\n"
                + "Reported By: " + b.getReportedBy() + "\n"
                + "Screenshot: " + b.getScreenshotPath();
    }

    private boolean hit(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void alert(Alert.AlertType type, String message) {
        Alert a = new Alert(type);
        a.initOwner(stage);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
