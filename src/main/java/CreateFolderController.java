import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class CreateFolderController {
    @FXML
    private TextField nameFolderTextField;
    @FXML
    private Label infoLabel;
    private static Stage window;
    
    public static void createModalWindow() throws IOException {
        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader fxmlLoader = new FXMLLoader(StartApp.class.getResource("createFolder.fxml"));

        window.setScene(new Scene(fxmlLoader.load()));
        window.setTitle("Create folder");
        window.showAndWait();
    }

    @FXML
    public void confirmCreatingFolder() throws IOException {
        if (nameFolderTextField.getText().isEmpty()){
            infoLabel.setVisible(true);
            infoLabel.setTextFill(Paint.valueOf("red"));
            infoLabel.setText("Name of folder can't be empty");
        }
        else{
            //создать папку с меткой (все папки будут создаваться с такой метадатой
            GoogleDriveClass.createFolder(nameFolderTextField.getText(), StartWindowController.currentFolder);
            window.close();
        }
    }
}
