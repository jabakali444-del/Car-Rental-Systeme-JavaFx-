
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Login extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private final VBox root = new VBox(12);

    private final Label lblEmail = new Label("Email:");
    private final Label lblPassword = new Label("Password:");

    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();

    private final Button btLogin = new Button("Login");

    @Override
    public void start(Stage primaryStage) {

        HBox hBoxEmail = new HBox(8, lblEmail, emailField);
        hBoxEmail.setAlignment(Pos.CENTER_LEFT);


        HBox hBoxPassword = new HBox(8, lblPassword, passwordField);
        hBoxPassword.setAlignment(Pos.CENTER_LEFT);


        emailField.setPromptText("Enter your email...");
        passwordField.setPromptText("Enter your password...");


        btLogin.setOnAction(e -> {
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showError("Please fill all the fields!");
                return;
            }


            try (Connection conn = DBConnection.getConnection()) {


                String sql = "SELECT role FROM login WHERE email = ? AND password = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, email);
                    ps.setString(2, password);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String role = rs.getString("role");

                            if ("admin".equalsIgnoreCase(role)) {

                                new QuestionManger().start(new Stage());
                            } else if ("student".equalsIgnoreCase(role)) {

                                new StudentExam().start(new Stage());
                            } else {
                                showError("Unknown role: " + role);
                                return;
                            }

                            ((Stage) btLogin.getScene().getWindow()).close();
                        } else {
                            showError("Invalid email or password!");
                        }
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Database connection error");
            }
        });


        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_LEFT);
        root.getChildren().addAll(
                hBoxEmail,
                hBoxPassword,
                btLogin
        );

        Scene scene = new Scene(root, 420, 180);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Login");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.showAndWait();
    }
}
