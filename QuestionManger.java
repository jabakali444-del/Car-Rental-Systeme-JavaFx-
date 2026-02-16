
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuestionManger extends Application {


    private TextField searchField;
    private ComboBox<String> typeCombo;
    private TableView<Question> table;
    private final ObservableList<Question> rows = FXCollections.observableArrayList();


    private static final String DB_URL  = "jdbc:mysql://localhost:3306/final-programming3?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";
    private static final String TABLE   = "`instructore-mode`";

    @Override
    public void start(Stage stage) {
        stage.setTitle("Question Manager (Instructor Mode)");


        Button examBuilderBtn = new Button(" Exam Builder");


        examBuilderBtn.setOnAction(e -> {
            try {
                new ExamBuilder().start(new Stage());  // make sure ExamBuilder is accessible
            } catch (Exception ex) {
                ex.printStackTrace();

                Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
                a.setTitle("Open Exam Builder failed");
                a.showAndWait();
            }
        });


        searchField = new TextField();
        searchField.setPromptText("Search question text...");
        Label searchLabel = new Label("Search:");

        typeCombo = new ComboBox<>();
        typeCombo.setPromptText("Type");
        Label typeLabel = new Label("Type:");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refreshData());

        HBox top = new HBox(10, searchLabel, searchField, typeLabel, typeCombo, refreshBtn,examBuilderBtn);
        top.setPadding(new Insets(10));
        HBox.setHgrow(searchField, Priority.ALWAYS);


        table = new TableView<>();
        table.setItems(rows);

        TableColumn<Question, Integer> colId = new TableColumn<>("ID");
        colId.setPrefWidth(80);
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Question, String> colType = new TableColumn<>("Type");
        colType.setPrefWidth(120);
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Question, String> colText = new TableColumn<>("Question Text");
        colText.setPrefWidth(600);
        colText.setCellValueFactory(new PropertyValueFactory<>("text"));

        table.getColumns().addAll(colId, colType, colText);


        Button addBtn    = new Button("Add Question");
        Button editBtn   = new Button("Edit Question");
        Button delBtn    = new Button("Delete Question");
        Button exportBtn = new Button("Export Exam (txt)");
        Button backupBtn = new Button("Backup Questions");

        addBtn.setOnAction(e -> onAdd());
        editBtn.setOnAction(e -> onEdit());
        delBtn.setOnAction(e -> onDelete());
        exportBtn.setOnAction(e -> onExport());
        backupBtn.setOnAction(e -> onBackup());

        HBox bottom = new HBox(10, addBtn, editBtn, delBtn, exportBtn, backupBtn);
        bottom.setPadding(new Insets(10));


        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(table);
        root.setBottom(bottom);


        searchField.textProperty().addListener((o, a, b) -> refreshData());
        typeCombo.valueProperty().addListener((o, a, b) -> refreshData());

        stage.setScene(new Scene(root, 960, 560));
        stage.show();


        loadTypesFromDb();
        refreshData();
    }


    private Connection getConn() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }


    private void loadTypesFromDb() {
        List<String> types = new ArrayList<>();
        types.add("All");

        String sql = "SELECT DISTINCT type FROM " + TABLE + " WHERE type IS NOT NULL ORDER BY type";
        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String t = rs.getString(1);
                if (t != null && !t.isBlank())
                    types.add(t);
            }
        } catch (Exception ex) {
            error("Load Types Error", ex.getMessage());
        }

        typeCombo.getItems().setAll(types);
        if (!types.isEmpty()) typeCombo.getSelectionModel().selectFirst();
    }


    private void refreshData() {
        rows.clear();

        String search = (searchField.getText() == null) ? "" : searchField.getText().trim();
        String type = typeCombo.getValue();

        StringBuilder sql = new StringBuilder("SELECT id, type, text FROM " + TABLE + " WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isEmpty()) {
            sql.append("AND text LIKE ? ");
            params.add("%" + search + "%");
        }
        if (type != null && !"All".equalsIgnoreCase(type)) {
            sql.append("AND type = ? ");
            params.add(type);
        }
        sql.append("ORDER BY id ASC");

        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {


            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new Question(
                        rs.getInt("id"),
                        rs.getString("type"),
                        rs.getString("text")
                ));
            }
        } catch (Exception ex) {
            error("Load Data Error", ex.getMessage());
        }
    }


    private Question fetchFull(int id) {
        String sql = "SELECT id, type, text, optionA, optionB, optionC, optionD, CorrectAnswer FROM " + TABLE + " WHERE id = ?";
        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Question(
                        rs.getInt("id"),
                        rs.getString("type"),
                        rs.getString("text"),
                        rs.getString("optionA"),
                        rs.getString("optionB"),
                        rs.getString("optionC"),
                        rs.getString("optionD"),
                        rs.getString("CorrectAnswer")
                );
            }
        } catch (Exception ex) {
            error("Fetch Error", ex.getMessage());
        }
        return null;
    }



    private void onAdd() {
        Dialog<Question> dlg = buildDialog(null);
        dlg.setTitle("Add Question");
        dlg.setHeaderText("Enter question details");

        dlg.showAndWait().ifPresent(q -> {
            String sql = "INSERT INTO " + TABLE + " (type, text, optionA, optionB, optionC, optionD, CorrectAnswer) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection con = getConn();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, q.getType());
                ps.setString(2, q.getText());
                ps.setString(3, nullIfBlank(q.getOptionA()));
                ps.setString(4, nullIfBlank(q.getOptionB()));
                ps.setString(5, nullIfBlank(q.getOptionC()));
                ps.setString(6, nullIfBlank(q.getOptionD()));
                ps.setString(7, nullIfBlank(q.getCorrectAnswer()));
                ps.executeUpdate();
                refreshData();
                info("Added", "Question inserted.");
            } catch (Exception ex) {
                error("Insert Error", ex.getMessage());
            }
        });
    }

    private void onEdit() {
        Question minimal = table.getSelectionModel().getSelectedItem();
        if (minimal == null) {
            warn("No selection", "Please select a question to edit.");
            return;
        }
        Question full = fetchFull(minimal.getId());
        if (full == null) {
            warn("Not found", "Could not reload the question from DB.");
            return;
        }

        Dialog<Question> dlg = buildDialog(full);
        dlg.setTitle("Edit Question");
        dlg.setHeaderText("Modify question details");

        dlg.showAndWait().ifPresent(q -> {
            String sql = "UPDATE " + TABLE + " SET type=?, text=?, optionA=?, optionB=?, optionC=?, optionD=?, CorrectAnswer=? WHERE id=?";
            try (Connection con = getConn();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, q.getType());
                ps.setString(2, q.getText());
                ps.setString(3, nullIfBlank(q.getOptionA()));
                ps.setString(4, nullIfBlank(q.getOptionB()));
                ps.setString(5, nullIfBlank(q.getOptionC()));
                ps.setString(6, nullIfBlank(q.getOptionD()));
                ps.setString(7, nullIfBlank(q.getCorrectAnswer()));
                ps.setInt(8, q.getId());
                ps.executeUpdate();
                refreshData();
                info("Updated", "Question updated.");
            } catch (Exception ex) {
                error("Update Error", ex.getMessage());
            }
        });
    }

    private void onDelete() {
        Question selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            warn("No selection", "Please select a question to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete question ID " + selected.getId() + "?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String sql = "DELETE FROM " + TABLE + " WHERE id=?";
                try (Connection con = getConn();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, selected.getId());
                    ps.executeUpdate();
                    refreshData();
                    info("Deleted", "Question deleted.");
                } catch (Exception ex) {
                    error("Delete Error", ex.getMessage());
                }
            }
        });
    }


    private void onExport() {
        File file = new File("ExamExport.txt");
        try (PrintWriter pw = new PrintWriter(file)) {
            pw.println("=== Exam Export ===");
            for (Question qMin : table.getItems()) {
                Question q = fetchFull(qMin.getId());
                if (q == null) continue;

                pw.println("Q" + q.getId() + ": " + safe(q.getText()));
                pw.println("Type: " + safe(q.getType()));
                if (notBlank(q.getOptionA())) pw.println("A) " + q.getOptionA());
                if (notBlank(q.getOptionB())) pw.println("B) " + q.getOptionB());
                if (notBlank(q.getOptionC())) pw.println("C) " + q.getOptionC());
                if (notBlank(q.getOptionD())) pw.println("D) " + q.getOptionD());
                if (notBlank(q.getCorrectAnswer())) pw.println("Correct: " + q.getCorrectAnswer());
                pw.println("----------------------------------------");
            }
            info("Exported", "Saved to " + file.getAbsolutePath());
        } catch (Exception ex) {
            error("Export Error", ex.getMessage());
        }
    }


    private void onBackup() {
        File file = new File("QuestionsBackup.csv");
        String sql = "SELECT id, type, text, optionA, optionB, optionC, optionD, CorrectAnswer FROM " + TABLE + " ORDER BY id";
        try (Connection con = getConn();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();
             PrintWriter pw = new PrintWriter(file)) {

            pw.println("id,type,text,optionA,optionB,optionC,optionD,CorrectAnswer");
            while (rs.next()) {
                pw.printf("%d,%s,%s,%s,%s,%s,%s,%s%n",
                        rs.getInt("id"),
                        csv(rs.getString("type")),
                        csv(rs.getString("text")),
                        csv(rs.getString("optionA")),
                        csv(rs.getString("optionB")),
                        csv(rs.getString("optionC")),
                        csv(rs.getString("optionD")),
                        csv(rs.getString("CorrectAnswer")));
            }
            info("Backup Complete", "Saved to " + file.getAbsolutePath());
        } catch (Exception ex) {
            error("Backup Error", ex.getMessage());
        }
    }


    private Dialog<Question> buildDialog(Question existing) {
        Dialog<Question> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField idField = new TextField();
        idField.setEditable(false);

        ComboBox<String> typeField = new ComboBox<>();

        if (typeCombo.getItems().isEmpty()) {
            typeField.getItems().addAll("MCQ", "Free", "True/False", "Text");
        } else {
            typeField.getItems().addAll(typeCombo.getItems().filtered(s -> !"All".equalsIgnoreCase(s)));
        }
        if (!typeField.getItems().isEmpty()) typeField.getSelectionModel().selectFirst();

        TextArea textField = new TextArea(); textField.setWrapText(true);
        TextField aField = new TextField();
        TextField bField = new TextField();
        TextField cField = new TextField();
        TextField dField = new TextField();
        TextField correctField = new TextField();

        if (existing != null) {
            idField.setText(String.valueOf(existing.getId()));
            selectCombo(typeField, existing.getType());
            textField.setText(safe(existing.getText()));
            aField.setText(safe(existing.getOptionA()));
            bField.setText(safe(existing.getOptionB()));
            cField.setText(safe(existing.getOptionC()));
            dField.setText(safe(existing.getOptionD()));
            correctField.setText(safe(existing.getCorrectAnswer()));
        }

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        int r = 0;
        grid.add(new Label("ID:"), 0, r); grid.add(idField, 1, r++);
        grid.add(new Label("Type:"), 0, r); grid.add(typeField, 1, r++);
        grid.add(new Label("Text:"), 0, r); grid.add(textField, 1, r++);
        grid.add(new Label("Option A:"), 0, r); grid.add(aField, 1, r++);
        grid.add(new Label("Option B:"), 0, r); grid.add(bField, 1, r++);
        grid.add(new Label("Option C:"), 0, r); grid.add(cField, 1, r++);
        grid.add(new Label("Option D:"), 0, r); grid.add(dField, 1, r++);
        grid.add(new Label("Correct:"), 0, r); grid.add(correctField, 1, r);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Integer id = (idField.getText() == null || idField.getText().isBlank())
                        ? null : Integer.parseInt(idField.getText());
                return new Question(
                        id == null ? 0 : id,
                        typeField.getValue(),
                        textField.getText(),
                        aField.getText(),
                        bField.getText(),
                        cField.getText(),
                        dField.getText(),
                        correctField.getText()
                );
            }
            return null;
        });

        return dialog;
    }

    private void selectCombo(ComboBox<String> combo, String value) {
        if (value == null) return;
        for (String v : combo.getItems()) {
            if (v != null && v.equalsIgnoreCase(value)) {
                combo.getSelectionModel().select(v);
                return;
            }
        }
        combo.getItems().add(value);
        combo.getSelectionModel().select(value);
    }



    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }
    private void warn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }
    private void error(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }
    private String safe(String s) { return s == null ? "" : s; }
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private String nullIfBlank(String s) { return (s == null || s.isBlank()) ? null : s; }
    private String csv(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        return "\"" + v + "\"";
    }



    public static class Question {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final StringProperty type = new SimpleStringProperty();
        private final StringProperty text = new SimpleStringProperty();
        private final StringProperty optionA = new SimpleStringProperty();
        private final StringProperty optionB = new SimpleStringProperty();
        private final StringProperty optionC = new SimpleStringProperty();
        private final StringProperty optionD = new SimpleStringProperty();
        private final StringProperty correctAnswer = new SimpleStringProperty();

        public Question(int id, String type, String text) {
            this(id, type, text, null, null, null, null, null);
        }

        public Question(int id, String type, String text,
                        String optionA, String optionB, String optionC, String optionD,
                        String correctAnswer) {
            this.id.set(id);
            this.type.set(type);
            this.text.set(text);
            this.optionA.set(optionA);
            this.optionB.set(optionB);
            this.optionC.set(optionC);
            this.optionD.set(optionD);
            this.correctAnswer.set(correctAnswer);
        }

        public int getId() { return id.get(); }
        public String getType() { return type.get(); }
        public String getText() { return text.get(); }
        public String getOptionA() { return optionA.get(); }
        public String getOptionB() { return optionB.get(); }
        public String getOptionC() { return optionC.get(); }
        public String getOptionD() { return optionD.get(); }
        public String getCorrectAnswer() { return correctAnswer.get(); }


        public void setType(String t) { this.type.set(t); }
        public void setText(String t) { this.text.set(t); }

        public IntegerProperty idProperty() { return id; }
        public StringProperty typeProperty() { return type; }
        public StringProperty textProperty() { return text; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
