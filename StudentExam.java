
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class StudentExam extends Application {


    private final String studentIdentity;


    private Label lblStudent;
    private ComboBox<String> examCombo;
    private Button btnStart;

    private Label lblQuestionText;

    private RadioButton rbA, rbB, rbC, rbD;
    private ToggleGroup answersGroup;

    private TextArea explanationArea;

    private Button btnPrev, btnNext, btnFinish;


    private final List<Question> questions = new ArrayList<>(); // up to 3
    private int currentIndex = -1;


    private final Map<Integer, Character> selectedLetters = new HashMap<>();
    private final Map<Integer, String> explanations = new HashMap<>();


    public StudentExam() {
        this.studentIdentity = "";
    }
    public StudentExam(String studentIdentity) {
        this.studentIdentity = studentIdentity;
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Student Exam");


        lblStudent = new Label("Student: " + (studentIdentity == null ? "" : studentIdentity));
        examCombo = new ComboBox<>();
        examCombo.getItems().addAll("All", "MCQ", "Free");
        examCombo.getSelectionModel().selectFirst();

        btnStart = new Button("Start Exam");
        btnStart.setOnAction(e -> startExam());

        HBox topBar = new HBox(10,
                lblStudent,
                new Label("Exam:"),
                examCombo,
                btnStart
        );
        topBar.setPadding(new Insets(12));
        topBar.setAlignment(Pos.CENTER_LEFT);


        lblQuestionText = new Label();
        lblQuestionText.setWrapText(true);

        answersGroup = new ToggleGroup();
        rbA = new RadioButton();
        rbB = new RadioButton();
        rbC = new RadioButton();
        rbD = new RadioButton();
        rbA.setToggleGroup(answersGroup);
        rbB.setToggleGroup(answersGroup);
        rbC.setToggleGroup(answersGroup);
        rbD.setToggleGroup(answersGroup);

        VBox optionsBox = new VBox(6, rbA, rbB, rbC, rbD);

        Label lblExplain = new Label("Explain your answer:");
        explanationArea = new TextArea();
        explanationArea.setPromptText("Write your explanation here...");
        explanationArea.setWrapText(true);
        explanationArea.setPrefRowCount(4);

        VBox centerBox = new VBox(12,
                lblQuestionText,
                optionsBox,
                lblExplain,
                explanationArea
        );
        centerBox.setPadding(new Insets(12));


        btnPrev = new Button("Previous");
        btnNext = new Button("Next");
        btnFinish = new Button("Finish Exam");

        btnPrev.setOnAction(e -> goPrevious());
        btnNext.setOnAction(e -> goNext());
        btnFinish.setOnAction(e -> finishExam());

        HBox bottomBar = new HBox(10, btnPrev, btnNext, btnFinish);
        bottomBar.setPadding(new Insets(12));
        bottomBar.setAlignment(Pos.CENTER_RIGHT);


        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(centerBox);
        root.setBottom(bottomBar);


        setQuestionAreaEnabled(false);

        Scene scene = new Scene(root, 800, 520);
        stage.setScene(scene);
        stage.show();
    }



    private void startExam() {
        // Persist current (just in case)
        persistCurrentInput();

        String filter = examCombo.getValue();
        loadQuestionsFromDb(filter);
        if (questions.isEmpty()) {
            showWarn("No Questions", "No questions found for the selected exam/type.");
            setNavigationEnabled(false);
            setQuestionAreaEnabled(false);
            clearQuestionArea();
            return;
        }
        currentIndex = 0;
        renderCurrentQuestion();
        setNavigationEnabled(true);
        setQuestionAreaEnabled(true);
    }

    private void goPrevious() {
        persistCurrentInput();
        if (currentIndex > 0) {
            currentIndex--;
            renderCurrentQuestion();
        }
    }

    private void goNext() {
        persistCurrentInput();
        if (currentIndex < questions.size() - 1) {
            currentIndex++;
            renderCurrentQuestion();
        }
    }

    private void finishExam() {
        persistCurrentInput();

        int totalMcq = 0;
        int correctMcq = 0;
        StringBuilder summary = new StringBuilder();

        for (Question q : questions) {
            summary.append("Q").append(q.id).append(": ").append(safe(q.text)).append("\n");

            Character chosen = selectedLetters.get(q.id);
            String explain = explanations.getOrDefault(q.id, "");

            if ("MCQ".equalsIgnoreCase(q.type)) {
                totalMcq++;

                String correct = safe(q.correctAnswer);
                boolean isCorrect = false;
                if (chosen != null) {

                    char letter = Character.toUpperCase(chosen);
                    String chosenText = null;
                    switch (letter) {
                        case 'A': chosenText = q.optionA; break;
                        case 'B': chosenText = q.optionB; break;
                        case 'C': chosenText = q.optionC; break;
                        case 'D': chosenText = q.optionD; break;
                    }
                    if (correct.equalsIgnoreCase(String.valueOf(letter))) {
                        isCorrect = true;
                    } else if (chosenText != null && correct.equalsIgnoreCase(chosenText)) {
                        isCorrect = true;
                    }
                }
                if (isCorrect) correctMcq++;

                summary.append("  Your choice: ").append(chosen == null ? "(none)" : chosen)
                        .append(" | Correct: ").append(correct.isBlank() ? "(n/a)" : correct)
                        .append(" | Result: ").append(isCorrect ? "✓" : "✗").append("\n");
            } else {
                summary.append("  (Free question — ungraded)\n");
            }

            summary.append("  Explanation: ").append(explain.isBlank() ? "(none)" : explain).append("\n");

        }

        summary.append("Score (MCQ): ").append(correctMcq).append(" / ").append(totalMcq);

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Exam Finished");
        a.setHeaderText("Exam Summary");
        a.setContentText(summary.toString());
        a.setResizable(true);
        a.getDialogPane().setPrefSize(640, 480);
        a.showAndWait();


    }



    private void renderCurrentQuestion() {
        if (currentIndex < 0 || currentIndex >= questions.size()) {
            clearQuestionArea();
            return;
        }

        Question q = questions.get(currentIndex);
        lblQuestionText.setText("Q" + q.id + " (" + safe(q.type) + "): " + safe(q.text));

        answersGroup.selectToggle(null);


        boolean isMcq = "MCQ".equalsIgnoreCase(q.type);

        rbA.setText(optionLabel("A", q.optionA));
        rbB.setText(optionLabel("B", q.optionB));
        rbC.setText(optionLabel("C", q.optionC));
        rbD.setText(optionLabel("D", q.optionD));

        rbA.setVisible(isMcq && notBlank(q.optionA));
        rbB.setVisible(isMcq && notBlank(q.optionB));
        rbC.setVisible(isMcq && notBlank(q.optionC));
        rbD.setVisible(isMcq && notBlank(q.optionD));

        rbA.setManaged(rbA.isVisible());
        rbB.setManaged(rbB.isVisible());
        rbC.setManaged(rbC.isVisible());
        rbD.setManaged(rbD.isVisible());


        Character prevChoice = selectedLetters.get(q.id);
        if (prevChoice != null) {
            switch (Character.toUpperCase(prevChoice)) {
                case 'A': answersGroup.selectToggle(rbA); break;
                case 'B': answersGroup.selectToggle(rbB); break;
                case 'C': answersGroup.selectToggle(rbC); break;
                case 'D': answersGroup.selectToggle(rbD); break;
            }
        }


        explanationArea.setText(explanations.getOrDefault(q.id, ""));


        btnPrev.setDisable(currentIndex <= 0);
        btnNext.setDisable(currentIndex >= questions.size() - 1);
    }

    private void persistCurrentInput() {
        if (currentIndex < 0 || currentIndex >= questions.size()) return;

        Question q = questions.get(currentIndex);


        Toggle selected = answersGroup.getSelectedToggle();
        if (selected == rbA) selectedLetters.put(q.id, 'A');
        else if (selected == rbB) selectedLetters.put(q.id, 'B');
        else if (selected == rbC) selectedLetters.put(q.id, 'C');
        else if (selected == rbD) selectedLetters.put(q.id, 'D');
        else selectedLetters.remove(q.id); // none


        String explain = explanationArea.getText();
        explanations.put(q.id, explain == null ? "" : explain.trim());
    }

    private void clearQuestionArea() {
        lblQuestionText.setText("");
        answersGroup.selectToggle(null);
        rbA.setText("A)");
        rbB.setText("B)");
        rbC.setText("C)");
        rbD.setText("D)");
        rbA.setVisible(false); rbA.setManaged(false);
        rbB.setVisible(false); rbB.setManaged(false);
        rbC.setVisible(false); rbC.setManaged(false);
        rbD.setVisible(false); rbD.setManaged(false);
        explanationArea.clear();
    }

    private void setNavigationEnabled(boolean enabled) {
        btnPrev.setDisable(!enabled);
        btnNext.setDisable(!enabled);
        btnFinish.setDisable(!enabled);
    }

    private void setQuestionAreaEnabled(boolean enabled) {
        lblQuestionText.setDisable(!enabled);
        rbA.setDisable(!enabled);
        rbB.setDisable(!enabled);
        rbC.setDisable(!enabled);
        rbD.setDisable(!enabled);
        explanationArea.setDisable(!enabled);
    }

    private String optionLabel(String letter, String text) {
        return letter + ") " + (text == null ? "" : text);
    }



    private void loadQuestionsFromDb(String filter) {
        questions.clear();
        selectedLetters.clear();
        explanations.clear();
        currentIndex = -1;


        StringBuilder sql = new StringBuilder(
                "SELECT id, type, text, optionA, optionB, optionC, optionD, CorrectAnswer " +
                        "FROM `instructore-mode` WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if ("MCQ".equalsIgnoreCase(filter)) {
            sql.append("AND type = ? ");
            params.add("MCQ");
        } else if ("Free".equalsIgnoreCase(filter)) {
            sql.append("AND type = ? ");
            params.add("Free");
        }

        sql.append("ORDER BY id ASC LIMIT 3");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(new Question(
                            rs.getInt("id"),
                            rs.getString("type"),
                            rs.getString("text"),
                            rs.getString("optionA"),
                            rs.getString("optionB"),
                            rs.getString("optionC"),
                            rs.getString("optionD"),
                            rs.getString("CorrectAnswer")
                    ));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Database connection error");
        }
    };


    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle("Error");
        a.showAndWait();
    }
    private void showWarn(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setTitle("Warning");
        a.setHeaderText(header);
        a.showAndWait();
    }
    private String safe(String s) { return s == null ? "" : s; }
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }


    private static class Question {
        final int id;
        final String type;
        final String text;
        final String optionA, optionB, optionC, optionD;
        final String correctAnswer;

        Question(int id, String type, String text,
                 String optionA, String optionB, String optionC, String optionD,
                 String correctAnswer) {
            this.id = id;
            this.type = type;
            this.text = text;
            this.optionA = optionA;
            this.optionB = optionB;
            this.optionC = optionC;
            this.optionD = optionD;
            this.correctAnswer = correctAnswer;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

