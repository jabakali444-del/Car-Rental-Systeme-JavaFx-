
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamBuilder extends Application {


    private TableView<Exam> examsTable;
    private ObservableList<Exam> exams = FXCollections.observableArrayList();
    private TextField txtExamName;
    private Spinner<Integer> spnDuration;
    private Button btnCreateExam;


    private TableView<ExamItem> examItemsTable;
    private ObservableList<ExamItem> examItems = FXCollections.observableArrayList();


    private TableView<Question> allQuestionsTable;
    private ObservableList<Question> allQuestions = FXCollections.observableArrayList();


    private Button btnMoveUp, btnMoveDown, btnRemove, btnAddSelected;


    private static final String Q_TABLE = "`instructore-mode`"; // table with hyphen must be quoted

    @Override
    public void start(Stage stage) {

        stage.setTitle("Exam Builder");


        ensureSchema();
        seedDefaultExams();


        examsTable = new TableView<>();
        TableColumn<Exam, Integer> exId = new TableColumn<>("ID");
        exId.setCellValueFactory(new PropertyValueFactory<>("id"));
        exId.setPrefWidth(60);
        TableColumn<Exam, String> exName = new TableColumn<>("Exam Name");
        exName.setCellValueFactory(new PropertyValueFactory<>("name"));
        exName.setPrefWidth(180);
        TableColumn<Exam, Integer> exDur = new TableColumn<>("Duration (min)");
        exDur.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        exDur.setPrefWidth(120);
        examsTable.getColumns().addAll(exId, exName, exDur);
        examsTable.setItems(exams);
        examsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> loadExamItems());


        txtExamName = new TextField();
        txtExamName.setPromptText("Exam name (e.g., Java Basic Quiz)");
        spnDuration = new Spinner<>(10, 300, 60, 5); // 10 to 300 min, default 60, step 5
        spnDuration.setEditable(true);

        btnCreateExam = new Button("Create Exam");
        btnCreateExam.setOnAction(e -> createExam());

        GridPane createPanel = new GridPane();
        createPanel.setHgap(8);
        createPanel.setVgap(8);
        createPanel.setPadding(new Insets(8));
        createPanel.add(new Label("Name Exam:"), 0, 0);
        createPanel.add(txtExamName, 1, 0);
        createPanel.add(new Label("Duration (min):"), 0, 1);
        createPanel.add(spnDuration, 1, 1);
        createPanel.add(btnCreateExam, 1, 2);

        VBox leftPane = new VBox(10, new Label("Exams"), examsTable, new Separator(), createPanel);
        leftPane.setPadding(new Insets(10));
        leftPane.setPrefWidth(380);
        VBox.setVgrow(examsTable, Priority.ALWAYS);


        examItemsTable = new TableView<>();
        TableColumn<ExamItem, Integer> itOrder = new TableColumn<>("Order");
        itOrder.setCellValueFactory(new PropertyValueFactory<>("order"));
        itOrder.setPrefWidth(70);
        TableColumn<ExamItem, String> itType = new TableColumn<>("Type");
        itType.setCellValueFactory(new PropertyValueFactory<>("type"));
        itType.setPrefWidth(100);
        TableColumn<ExamItem, String> itText = new TableColumn<>("Question Text");
        itText.setCellValueFactory(new PropertyValueFactory<>("text"));
        itText.setPrefWidth(420);
        examItemsTable.getColumns().addAll(itOrder, itType, itText);
        examItemsTable.setItems(examItems);

        VBox centerPane = new VBox(10, new Label("Questions in Selected Exam"), examItemsTable);
        centerPane.setPadding(new Insets(10));
        VBox.setVgrow(examItemsTable, Priority.ALWAYS);


        allQuestionsTable = new TableView<>();
        TableColumn<Question, Integer> qId = new TableColumn<>("ID");
        qId.setCellValueFactory(new PropertyValueFactory<>("id"));
        qId.setPrefWidth(60);
        TableColumn<Question, String> qType = new TableColumn<>("Type");
        qType.setCellValueFactory(new PropertyValueFactory<>("type"));
        qType.setPrefWidth(100);
        TableColumn<Question, String> qText = new TableColumn<>("Question Text");
        qText.setCellValueFactory(new PropertyValueFactory<>("text"));
        qText.setPrefWidth(420);
        allQuestionsTable.getColumns().addAll(qId, qType, qText);
        allQuestionsTable.setItems(allQuestions);

        VBox rightPane = new VBox(10, new Label("All Questions"), allQuestionsTable);
        rightPane.setPadding(new Insets(10));
        rightPane.setPrefWidth(600);
        VBox.setVgrow(allQuestionsTable, Priority.ALWAYS);


        btnMoveUp = new Button("Move Up");
        btnMoveDown = new Button("Move Down");
        btnRemove = new Button("Remove from Exam");
        btnAddSelected = new Button("Add Selected to Exam");

        btnMoveUp.setOnAction(e -> moveSelected(-1));
        btnMoveDown.setOnAction(e -> moveSelected(+1));
        btnRemove.setOnAction(e -> removeSelectedFromExam());
        btnAddSelected.setOnAction(e -> addSelectedToExam());

        HBox bottomBar = new HBox(10, btnMoveUp, btnMoveDown, btnRemove, btnAddSelected);
        bottomBar.setPadding(new Insets(10));
        bottomBar.setAlignment(Pos.CENTER_RIGHT);


        BorderPane root = new BorderPane();
        root.setLeft(leftPane);
        root.setCenter(centerPane);
        root.setRight(rightPane);
        root.setBottom(bottomBar);


        loadExams();
        loadAllQuestions();

        Scene scene = new Scene(root, 1200, 620);
        stage.setScene(scene);
        stage.show();
    }



    private void loadExams() {
        exams.clear();
        String sql = "SELECT id, name, duration_minutes FROM exams ORDER BY id ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                exams.add(new Exam(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("duration_minutes")
                ));
            }
        } catch (Exception ex) {
            showError("Load exams failed: " + ex.getMessage());
        }
        if (!exams.isEmpty() && examsTable.getSelectionModel().getSelectedItem() == null) {
            examsTable.getSelectionModel().selectFirst();
            loadExamItems();
        }
    }

    private void loadExamItems() {
        examItems.clear();
        Exam ex = examsTable.getSelectionModel().getSelectedItem();
        if (ex == null) return;

        String sql = "SELECT eq.id, eq.q_order, q.id AS qid, q.type, q.text " +
                "FROM exam_questions eq " +
                "JOIN " + Q_TABLE + " q ON q.id = eq.question_id " +
                "WHERE eq.exam_id = ? " +
                "ORDER BY eq.q_order ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ex.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    examItems.add(new ExamItem(
                            rs.getInt("id"),
                            ex.getId(),
                            rs.getInt("qid"),
                            rs.getInt("q_order"),
                            rs.getString("type"),
                            rs.getString("text")
                    ));
                }
            }
        } catch (Exception exx) {
            showError("Load exam items failed: " + exx.getMessage());
        }
    }

    private void loadAllQuestions() {
        allQuestions.clear();
        String sql = "SELECT id, type, text FROM " + Q_TABLE + " ORDER BY id ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                allQuestions.add(new Question(
                        rs.getInt("id"),
                        rs.getString("type"),
                        rs.getString("text")
                ));
            }
        } catch (Exception ex) {
            showError("Load questions failed: " + ex.getMessage());
        }
    }



    private void createExam() {
        String name = txtExamName.getText() == null ? "" : txtExamName.getText().trim();
        int duration = spnDuration.getValue();

        if (name.isEmpty()) {
            showWarn("Validation", "Please enter exam name.");
            return;
        }
        String sql = "INSERT INTO exams (name, duration_minutes) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, duration);
            ps.executeUpdate();
        } catch (Exception ex) {
            showError("Create exam failed: " + ex.getMessage());
        }
        txtExamName.clear();
        spnDuration.getValueFactory().setValue(60);
        loadExams();
    }

    private void addSelectedToExam() {
        Exam ex = examsTable.getSelectionModel().getSelectedItem();
        Question q = allQuestionsTable.getSelectionModel().getSelectedItem();
        if (ex == null) { showWarn("No exam", "Select an exam first."); return; }
        if (q == null) { showWarn("No question", "Select a question to add."); return; }

        int nextOrder = examItems.isEmpty() ? 1 : examItems.get(examItems.size() - 1).getOrder() + 1;

        String sql = "INSERT INTO exam_questions (exam_id, question_id, q_order) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ex.getId());
            ps.setInt(2, q.getId());
            ps.setInt(3, nextOrder);
            ps.executeUpdate();
        } catch (Exception exx) {
            showError("Add question failed: " + exx.getMessage());
        }
        loadExamItems();
    }

    private void removeSelectedFromExam() {
        ExamItem item = examItemsTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            showWarn("No selection", "Select an item in the exam to remove.");
            return;
        }
        String sql = "DELETE FROM exam_questions WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, item.getId());
            ps.executeUpdate();
        } catch (Exception ex) {
            showError("Remove failed: " + ex.getMessage());
        }

        renumberOrders(item.getExamId());
        loadExamItems();
    }

    private void moveSelected(int delta) {
        ExamItem item = examItemsTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            showWarn("No selection", "Select an item to move.");
            return;
        }

        int index = examItems.indexOf(item);
        int targetIndex = index + delta;
        if (targetIndex < 0 || targetIndex >= examItems.size()) return;

        ExamItem neighbor = examItems.get(targetIndex);


        String sql1 = "UPDATE exam_questions SET q_order=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql1)) {


            ps.setInt(1, neighbor.getOrder());
            ps.setInt(2, item.getId());
            ps.executeUpdate();

            // neighbor -> item's order
            ps.clearParameters();
            ps.setInt(1, item.getOrder());
            ps.setInt(2, neighbor.getId());
            ps.executeUpdate();

        } catch (Exception ex) {
            showError("Move failed: " + ex.getMessage());
        }
        loadExamItems();

        int newIndex = Math.max(0, Math.min(examItems.size() - 1, targetIndex));
        if (!examItems.isEmpty()) {
            examItemsTable.getSelectionModel().select(newIndex);
        }
    }

    private void renumberOrders(int examId) {

        List<Integer> ids = new ArrayList<>();
        String sel = "SELECT id FROM exam_questions WHERE exam_id=? ORDER BY q_order ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sel)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        } catch (Exception ex) {
            showError("Renumber select failed: " + ex.getMessage());
        }

        String upd = "UPDATE exam_questions SET q_order=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(upd)) {
            for (int i = 0; i < ids.size(); i++) {
                ps.setInt(1, i + 1);
                ps.setInt(2, ids.get(i));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception ex) {
            showError("Renumber update failed: " + ex.getMessage());
        }
    }


    private void ensureSchema() {
        String createExams =
                "CREATE TABLE IF NOT EXISTS exams (" +
                        "  id INT AUTO_INCREMENT PRIMARY KEY," +
                        "  name VARCHAR(255) NOT NULL," +
                        "  duration_minutes INT NOT NULL" +
                        ")";
        String createExamQuestions =
                "CREATE TABLE IF NOT EXISTS exam_questions (" +
                        "  id INT AUTO_INCREMENT PRIMARY KEY," +
                        "  exam_id INT NOT NULL," +
                        "  question_id INT NOT NULL," +
                        "  q_order INT NOT NULL," +
                        "  FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE" +
                        ")";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {
            st.execute(createExams);
            st.execute(createExamQuestions);
        } catch (Exception ex) {
            showError("Schema init failed: " + ex.getMessage());
        }
    }

    private void seedDefaultExams() {
        // Seed "Java Basic Quiz" and "OOP Midterm" if not present
        String countSql = "SELECT COUNT(*) FROM exams WHERE name IN (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(countSql)) {
            ps.setString(1, "Java Basic Quiz");
            ps.setString(2, "OOP Midterm");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) < 2) {
                    // Insert both (default 60 min)
                    insertExamIfMissing("Java Basic Quiz", 60);
                    insertExamIfMissing("OOP Midterm", 90);
                }
            }
        } catch (Exception ex) {
            showError("Seeding failed: " + ex.getMessage());
        }
    }

    private void insertExamIfMissing(String name, int duration) throws SQLException {
        String exists = "SELECT id FROM exams WHERE name = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(exists)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    String ins = "INSERT INTO exams (name, duration_minutes) VALUES (?, ?)";
                    try (PreparedStatement insPs = con.prepareStatement(ins)) {
                        insPs.setString(1, name);
                        insPs.setInt(2, duration);
                        insPs.executeUpdate();
                    }
                }
            }
        }
    }



    private void showWarn(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setTitle("Warning");
        a.setHeaderText(header);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle("Error");
        a.setHeaderText("Operation failed");
        a.setContentText(msg);
        a.showAndWait();
    }



    public static class Exam {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final StringProperty name = new SimpleStringProperty();
        private final IntegerProperty durationMinutes = new SimpleIntegerProperty();

        public Exam(int id, String name, int durationMinutes) {
            this.id.set(id);
            this.name.set(name);
            this.durationMinutes.set(durationMinutes);
        }
        public int getId() { return id.get(); }
        public String getName() { return name.get(); }
        public int getDurationMinutes() { return durationMinutes.get(); }

        public IntegerProperty idProperty() { return id; }
        public StringProperty nameProperty() { return name; }
        public IntegerProperty durationMinutesProperty() { return durationMinutes; }
    }

    public static class ExamItem {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final IntegerProperty examId = new SimpleIntegerProperty();
        private final IntegerProperty questionId = new SimpleIntegerProperty();
        private final IntegerProperty order = new SimpleIntegerProperty();
        private final StringProperty type = new SimpleStringProperty();
        private final StringProperty text = new SimpleStringProperty();

        public ExamItem(int id, int examId, int questionId, int order, String type, String text) {
            this.id.set(id);
            this.examId.set(examId);
            this.questionId.set(questionId);
            this.order.set(order);
            this.type.set(type);
            this.text.set(text);
        }

        public int getId() { return id.get(); }
        public int getExamId() { return examId.get(); }
        public int getQuestionId() { return questionId.get(); }
        public int getOrder() { return order.get(); }
        public String getType() { return type.get(); }
        public String getText() { return text.get(); }

        public IntegerProperty orderProperty() { return order; }
        public StringProperty typeProperty() { return type; }
        public StringProperty textProperty() { return text; }
    }

    public static class Question {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final StringProperty type = new SimpleStringProperty();
        private final StringProperty text = new SimpleStringProperty();

        public Question(int id, String type, String text) {
            this.id.set(id);
            this.type.set(type);
            this.text.set(text);
        }
        public int getId() { return id.get(); }
        public String getType() { return type.get(); }
        public String getText() { return text.get(); }

        public IntegerProperty idProperty() { return id; }
        public StringProperty typeProperty() { return type; }
        public StringProperty textProperty() { return text; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
