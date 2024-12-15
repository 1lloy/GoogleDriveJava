import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class RenameFileController {
    private static Stage window;
    @FXML
    private Label infoLabel;
    @FXML
    private TextField nameOfFileTextField;
    private static String idOfRenamingFile = "";
    public static void createModalWindow(String id) throws IOException {
        idOfRenamingFile = id;
        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader fxmlLoader = new FXMLLoader(StartApp.class.getResource("renameFile.fxml"));

        window.setScene(new Scene(fxmlLoader.load()));
        window.setTitle("Rename File");
        window.showAndWait();
    }
    @FXML
    public void confirmRenamingFile() throws IOException {
        if (!nameOfFileTextField.getText().isEmpty()){
            GoogleDriveClass.renameFile(idOfRenamingFile, nameOfFileTextField.getText());
            StartWindowController.isRenamed = true;
            window.close();
        }
        else {
            infoLabel.setVisible(true);
            infoLabel.setTextFill(Paint.valueOf("red"));
            infoLabel.setText("New name can not be empty.");
        }
    }
    @FXML
    public void cancelRenamingFile(){
        window.close();
    }
}
