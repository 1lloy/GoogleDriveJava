import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class DeleteFileController {
    private static Stage window;
    private static String idOfDeletingFile = "";

    public static void createModalWindow(String id) throws IOException {
        idOfDeletingFile = id;
        System.out.println(idOfDeletingFile);
        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader fxmlLoader = new FXMLLoader(StartApp.class.getResource("deleteFile.fxml"));

        window.setScene(new Scene(fxmlLoader.load()));
        window.setTitle("Deleting file");
        window.showAndWait();
    }

    @FXML
    public void confirmDelete() throws IOException {
        GoogleDriveClass.deleteFile(idOfDeletingFile);
        window.close();
    }

    @FXML
    public void cancelDelete(){
        window.close();
    }
}
