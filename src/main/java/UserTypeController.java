import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class UserTypeController extends Application implements Initializable {
    @FXML
    private Button rootButton, userButton, startWorkButton, directoryChooserButton;
    @FXML
    private Label topLabel, folderNameLabel, infoLabel, tokenFolderInfoLabel, tokensFolderPathLabel;
    @FXML
    private TextField newFolderTextField;
    public static String nameOfMainFolder = "";
    public static boolean needToCreateFolder;
    public static DirectoryChooser directoryChooser;
    public static Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        needToCreateFolder = false;
        stage = primaryStage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameOfMainFolder = "";
        tokensFolderPathLabel.setText("no selected folder");

        //отключаем все необходимое для ввода имени папки
        folderNameLabel.setVisible(false);
        folderNameLabel.setDisable(true);
        newFolderTextField.setVisible(false);
        newFolderTextField.setDisable(true);
        startWorkButton.setVisible(false);
        startWorkButton.setDisable(true);
        infoLabel.setVisible(false);
        infoLabel.setDisable(true);

        //включаем кнопки выбора типа
        rootButton.setDisable(false);
        rootButton.setVisible(true);
        userButton.setDisable(false);
        userButton.setVisible(true);

        //включаем выбор папки для хранения токена StoredCredential
        tokenFolderInfoLabel.setVisible(true);
        tokenFolderInfoLabel.setDisable(false);
        tokensFolderPathLabel.setVisible(true);
        tokensFolderPathLabel.setDisable(false);
        directoryChooserButton.setVisible(true);
        directoryChooserButton.setDisable(false);
        directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("/"));
    }

    @FXML
    public void setUserToRoot() throws IOException {
        if (!tokensFolderPathLabel.getText().equals("no selected folder")){
            GoogleDriveClass.userType = "root";

            //отключаем кнопки выбора
            rootButton.setDisable(true);
            rootButton.setVisible(false);
            userButton.setDisable(true);
            userButton.setVisible(false);

            //включаем все для создания папки
            folderNameLabel.setVisible(true);
            folderNameLabel.setDisable(false);
            newFolderTextField.setVisible(true);
            newFolderTextField.setDisable(false);
            startWorkButton.setVisible(true);
            startWorkButton.setDisable(false);

            topLabel.setText("Creating master folder\nYou need to create a source in the \"My Disk\" section,\nto start working in it");

            //заносим информацию о пути к файлу авторизации в файл pathToToken
            try(FileWriter fileWriter = new FileWriter(GoogleDriveClass.FILE_WITH_TOKENS_DIRECTORY_PATH, false)){
                fileWriter.write(tokensFolderPathLabel.getText());
            }
            catch(IOException ex){
                System.out.println(ex.getMessage());
            }

            //выключаем выбор папки для хранения токена StoredCredential
            tokenFolderInfoLabel.setVisible(false);
            tokenFolderInfoLabel.setDisable(true);
            tokensFolderPathLabel.setVisible(false);
            tokensFolderPathLabel.setDisable(true);
            directoryChooserButton.setVisible(false);
            directoryChooserButton.setDisable(true);
        }
        else{
            infoLabel.setVisible(true);
            infoLabel.setDisable(false);
            infoLabel.setTextFill(Paint.valueOf("red"));
            infoLabel.setText("You should choose directory");
        }
    }

    @FXML
    public void setUserToUser() throws IOException {
        if (!tokensFolderPathLabel.getText().equals("no selected folder")){
            GoogleDriveClass.userType = "user";

            //заносим информацию о пути к файлу авторизации в файл pathToToken
            try(FileWriter fileWriter = new FileWriter(GoogleDriveClass.FILE_WITH_TOKENS_DIRECTORY_PATH, false)){
                fileWriter.write(tokensFolderPathLabel.getText());
            }
            catch(IOException ex){
                System.out.println(ex.getMessage());
            }
            goToAuth();
        }
        else{
            infoLabel.setVisible(true);
            infoLabel.setDisable(false);
            infoLabel.setTextFill(Paint.valueOf("red"));
            infoLabel.setText("You should choose directory");
        }
    }

    private void goToAuth() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(UserTypeController.class.getResource("startWindow.fxml"));
        Scene mainScene = new Scene(fxmlLoader.load());
        StartApp.mainStage.setTitle("Documents");
        StartApp.mainStage.setScene(mainScene);
        StartApp.mainStage.show();
    }

    @FXML
    public void startWork() throws IOException {
        if (newFolderTextField.getText().isEmpty()){
            infoLabel.setVisible(true);
            infoLabel.setDisable(false);
            infoLabel.setTextFill(Paint.valueOf("red"));
            infoLabel.setText("Folder name can not be empty");
        }
        else{
            //запоминаем имя папки для последующего создания
            nameOfMainFolder = newFolderTextField.getText();
            needToCreateFolder = true;
            goToAuth();
        }
    }

    @FXML
    public void chooseTokenDirectory(){
        File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null){
            //запоминаем путь до токена StoredCredential
            GoogleDriveClass.TOKENS_DIRECTORY_PATH = selectedDirectory.getAbsolutePath();
            tokensFolderPathLabel.setText(GoogleDriveClass.TOKENS_DIRECTORY_PATH);
        }
    }
}
