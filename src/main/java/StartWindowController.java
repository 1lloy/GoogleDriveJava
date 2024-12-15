import com.google.api.services.drive.model.File;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StartWindowController implements Initializable {
    @FXML
    private AnchorPane mainAnchorPane;
    @FXML
    private ListView<String> listView;
    @FXML
    private Label choosenFileName, infoLabel;
    @FXML
    private Button backToFolder, createFolder;
    final FileChooser fileChooser = new FileChooser();
    private Desktop desktop = Desktop.getDesktop();
    private java.io.File file = null;
    private ObservableList<String> namesOfFiles = FXCollections.observableArrayList();
    private ObservableList<String> namesOfFilesInFolder = FXCollections.observableArrayList();
    private List<File> filesFromGoogleDrive = new ArrayList<>();
    private List<String> fileNamesFromGoogleDrive = new ArrayList<>();
    private ArrayList<String> foldersInPath = new ArrayList<>();
    public static String currentFolder = "";
    private String mimeType = "";
    public String copiedNameOfFile = "";
    public String copiedIdOfFile = "";
    public static String idOfDeleteFile = "";
    public static boolean isRenamed = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        infoLabel.setVisible(false);
        infoLabel.setText("");
        backToFolder.setDisable(true);
        fileChooser.setTitle("Choose text file");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.docx", "*.doc", "*.pdf", "*.csv"));

        //настройка Drag-and-Drop
        mainAnchorPane.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                if (event.getGestureSource() != mainAnchorPane && event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.LINK);
                }
                event.consume();
            }
        });
        //настройка Drag-and-Drop
        mainAnchorPane.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {

                    file = db.getFiles().get(0);
                    String path = file.getPath();

                    try {
                        mimeType = Files.probeContentType(Path.of(path));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    choosenFileName.setText(file.getName() + "\n" + file.getPath() + "\n" + mimeType);
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            }
        });

        try {
            GoogleDriveClass.authorization();
            //создание главной папки администратором
            if (GoogleDriveClass.userType.equals("root")){
                if (UserTypeController.needToCreateFolder){
                    UserTypeController.needToCreateFolder = false;
                    GoogleDriveClass.createMainFolder(UserTypeController.nameOfMainFolder);
                }
            }

            filesFromGoogleDrive = GoogleDriveClass.getFiles();
            updateFiles();

            //сохранение id начальной (корневой) папки, только для рута
            if (!filesFromGoogleDrive.isEmpty() && GoogleDriveClass.userType.equals("root")){
                currentFolder = filesFromGoogleDrive.get(0).getParents().get(0);
            }
            else{
                createFolder.setDisable(true);
            }

        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        //контекстное меню для вставки скопированного файла
        ContextMenu contextMenuPaste = new ContextMenu();
        MenuItem pasteItem = new MenuItem("Paste");
        contextMenuPaste.getItems().add(pasteItem);
        pasteItem.setDisable(true);
        listView.setContextMenu(contextMenuPaste);

        //для вставки
        pasteItem.setOnAction(event -> {
            if (!currentFolder.isEmpty()){
                try {
                    GoogleDriveClass.copyAndPasteFile(copiedIdOfFile, copiedNameOfFile, currentFolder);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                infoLabel.setVisible(true);
                infoLabel.setTextFill(Paint.valueOf("green"));
                infoLabel.setText("File copied and pasted successfully");
                //получение нового списка файлов в текущей папке
                //загружать все файлы папки аналогичным образом
                namesOfFiles.clear();
                fileNamesFromGoogleDrive.clear();

                try {
                    filesFromGoogleDrive = GoogleDriveClass.getFilesFromFolder(currentFolder);
                } catch (IOException | GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
                updateFiles();
            }
            else{
                infoLabel.setVisible(true);
                infoLabel.setTextFill(Paint.valueOf("red"));
                infoLabel.setText("You can not paste file in this folder");
            }
        });

        //контекстное меню при выборе файла
        listView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem downloadItem = new MenuItem("Download");
            MenuItem copyItem = new MenuItem("Copy");
            MenuItem deleteItem = new MenuItem("Delete");
            MenuItem renameItem = new MenuItem("Rename");

            //скачивание
            downloadItem.setOnAction(event -> {
                String itemName = cell.getItem();
                int posOfFileInList = fileNamesFromGoogleDrive.indexOf(itemName);

                if (!filesFromGoogleDrive.get(posOfFileInList).getMimeType().equals("application/vnd.google-apps.folder")){
                    infoLabel.setVisible(false);
                    contextMenu.hide();

                    try {
                        downloadFile(itemName, posOfFileInList);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            //копирование
            copyItem.setOnAction(event -> {
                //запомнить файл для копирования, далее активировать меню вставки
                String itemName = cell.getItem();
                int posOfFileInList = fileNamesFromGoogleDrive.indexOf(itemName);

                if (!filesFromGoogleDrive.get(posOfFileInList).getMimeType().equals("application/vnd.google-apps.folder")){
                    copiedNameOfFile = "COPY OF " + filesFromGoogleDrive.get(posOfFileInList).getName();
                    copiedIdOfFile = filesFromGoogleDrive.get(posOfFileInList).getId();
                    System.out.println(copiedIdOfFile + " " + copiedNameOfFile);

                    pasteItem.setDisable(false);
                }
                else{
                    infoLabel.setVisible(true);
                    infoLabel.setTextFill(Paint.valueOf("red"));
                    infoLabel.setText("You can not copy a folder");
                }
            });
            //удаление
            deleteItem.setOnAction(event -> {
                if (!currentFolder.isEmpty()) {
                    String itemName = cell.getItem();
                    int posOfFileInList = fileNamesFromGoogleDrive.indexOf(itemName);
                    idOfDeleteFile = filesFromGoogleDrive.get(posOfFileInList).getId();

                    try {
                        if (GoogleDriveClass.getOwnerOfTheFile(idOfDeleteFile)) {
                            try {
                                DeleteFileController.createModalWindow(idOfDeleteFile);

                                infoLabel.setVisible(true);
                                infoLabel.setTextFill(Paint.valueOf("green"));
                                infoLabel.setText("File deleted");
                                //получение нового списка файлов в текущей папке
                                //загружать все файлы папки аналогичным образом
                                namesOfFiles.clear();
                                fileNamesFromGoogleDrive.clear();

                                try {
                                    filesFromGoogleDrive = GoogleDriveClass.getFilesFromFolder(currentFolder);
                                } catch (IOException | GeneralSecurityException e) {
                                    throw new RuntimeException(e);
                                }
                                updateFiles();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        else {
                            infoLabel.setVisible(true);
                            infoLabel.setTextFill(Paint.valueOf("red"));
                            infoLabel.setText("You don't have permissions to delete this file");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    infoLabel.setVisible(true);
                    infoLabel.setTextFill(Paint.valueOf("red"));
                    infoLabel.setText("You can not delete files in this folder");
                }
            });

            //переименование
            renameItem.setOnAction(event -> {
                if (!currentFolder.isEmpty()) {
                    String itemName = cell.getItem();
                    int posOfFileInList = fileNamesFromGoogleDrive.indexOf(itemName);
                    String idOfRenameFile = filesFromGoogleDrive.get(posOfFileInList).getId();

                    try {
                        if (GoogleDriveClass.getOwnerOfTheFile(idOfRenameFile)) {
                            //переименовываем файл
                            RenameFileController.createModalWindow(idOfRenameFile);
                            if (isRenamed){
                                infoLabel.setVisible(true);
                                infoLabel.setTextFill(Paint.valueOf("green"));
                                infoLabel.setText("File renamed");

                                //получение нового списка файлов в текущей папке
                                //загружать все файлы папки аналогичным образом
                                namesOfFiles.clear();
                                fileNamesFromGoogleDrive.clear();

                                try {
                                    filesFromGoogleDrive = GoogleDriveClass.getFilesFromFolder(currentFolder);
                                } catch (IOException | GeneralSecurityException e) {
                                    throw new RuntimeException(e);
                                }
                                updateFiles();
                            }
                            isRenamed = false;
                        }
                        else {
                            infoLabel.setVisible(true);
                            infoLabel.setTextFill(Paint.valueOf("red"));
                            infoLabel.setText("You don't have permissions to rename this file");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    infoLabel.setVisible(true);
                    infoLabel.setTextFill(Paint.valueOf("red"));
                    infoLabel.setText("You don't have permissions to rename this file");
                }
            });
            //переход по папкам (дабл клик)
            cell.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent click) {

                    if (listView.getSelectionModel().getSelectedItem() != null && click.getClickCount() == 2 && click.getButton().equals(MouseButton.PRIMARY)) {
                        int posInList = fileNamesFromGoogleDrive.indexOf(listView.getSelectionModel().getSelectedItem());
                        if (filesFromGoogleDrive.get(posInList).getMimeType().equals("application/vnd.google-apps.folder")){
                            if (backToFolder.isDisable()){
                                backToFolder.setDisable(false);
                            }
                            if (createFolder.isDisable()){
                                createFolder.setDisable(false);
                            }

                            //загружать все файлы папки аналогичным образом
                            namesOfFiles.clear();
                            fileNamesFromGoogleDrive.clear();
                            //занесли id папки из которой переходим в "путь"
                            foldersInPath.add(currentFolder);
                            //сохранение id новой папки (в которую переходим)
                            currentFolder = filesFromGoogleDrive.get(posInList).getId();

                            try {
                                filesFromGoogleDrive = GoogleDriveClass.getFilesFromFolder(currentFolder);
                            } catch (IOException | GeneralSecurityException e) {
                                throw new RuntimeException(e);
                            }
                            updateFiles();
                        }
                    }
                }
            });
            contextMenu.getItems().addAll(copyItem, renameItem, deleteItem, downloadItem);

            cell.textProperty().bind(cell.itemProperty());
            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });
            return cell ;
        });
    }

    @FXML
    public void backToFolder() throws IOException {
        namesOfFiles.clear();
        fileNamesFromGoogleDrive.clear();

        //id папки выше, куда нужно вернуться
        currentFolder = foldersInPath.get(foldersInPath.size() - 1);

        if (GoogleDriveClass.userType.equals("user") && foldersInPath.size() == 1){
            filesFromGoogleDrive = GoogleDriveClass.getFilesFromSharedDrive();
            foldersInPath.remove(foldersInPath.size() - 1);

            createFolder.setDisable(true);
        }
        else {
            try {
                filesFromGoogleDrive = GoogleDriveClass.getFilesFromFolder(currentFolder);
                foldersInPath.remove(foldersInPath.size() - 1);
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }
        updateFiles();

        if (foldersInPath.isEmpty()){
            backToFolder.setDisable(true);
        }
    }

    @FXML
    public void createFolder() throws IOException {
        CreateFolderController.createModalWindow();

        infoLabel.setVisible(true);
        infoLabel.setTextFill(Paint.valueOf("green"));
        infoLabel.setText("Folder created");

        //получение нового списка файлов в текущей папке
        //загружать все файлы папки аналогичным образом
        namesOfFiles.clear();
        fileNamesFromGoogleDrive.clear();

        try {
            filesFromGoogleDrive = GoogleDriveClass.getFilesFromFolder(currentFolder);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        updateFiles();
    }

    @FXML
    public void openFileChooserDialog() throws IOException {
        infoLabel.setVisible(false);
        infoLabel.setText("");

        file = fileChooser.showOpenDialog(StartApp.mainStage);

        if (file != null) {
            String path = file.getPath();
            mimeType = Files.probeContentType(Path.of(path));
            choosenFileName.setText(file.getName() + "\n" + file.getPath() + "\n" + mimeType);
        }
    }

    @FXML
    public void openFile(){
        if (file != null){
            try {
                desktop.open(file);
            } catch (IOException ex) {
                Logger.getLogger(StartWindowController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else {
            infoLabel.setVisible(true);
            infoLabel.setTextFill(Paint.valueOf("red"));
            infoLabel.setText("No selected file.");
        }
    }

    @FXML
    public void uploadFile() throws IOException {
        if (file != null){
            choosenFileName.setText("no chosen file yet");

            GoogleDriveClass.uploadFile(file, mimeType, currentFolder);

            file = null;
            infoLabel.setVisible(true);
            infoLabel.setTextFill(Paint.valueOf("green"));
            infoLabel.setText("Upload complete.");

            //получение нового списка файлов в текущей папке
            //загружать все файлы папки аналогичным образом
            namesOfFiles.clear();
            fileNamesFromGoogleDrive.clear();

            try {
                filesFromGoogleDrive = GoogleDriveClass.getFilesFromFolder(currentFolder);
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
            updateFiles();
        }
        else{
            infoLabel.setVisible(true);
            infoLabel.setTextFill(Paint.valueOf("red"));
            infoLabel.setText("No selected file.");
        }
    }

    public void downloadFile(String fileName, int posOfFile) throws IOException {

        infoLabel.setVisible(true);

        Platform.runLater(new Runnable(){
            public void run() {
                infoLabel.setTextFill(Paint.valueOf("blue"));
                infoLabel.setText("Downloading, please wait...");
            }
        });

        String fileMimeType = filesFromGoogleDrive.get(posOfFile).getMimeType();
        System.out.println(fileMimeType);
        String fileId = filesFromGoogleDrive.get(posOfFile).getId();

        GoogleDriveClass.downloadFile(fileName, fileId, fileMimeType);

        Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
                infoLabel.setTextFill(Paint.valueOf("green"));
                infoLabel.setText("Download complete.");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void updateFiles(){
        for (File file: filesFromGoogleDrive){

            String nameToShow = "";
            if (file.getMimeType().equals("application/vnd.google-apps.folder")){
                nameToShow = "(FOLDER) " + file.getName();
            }
            else{
                nameToShow = file.getName();
            }
            namesOfFiles.add(nameToShow);
            fileNamesFromGoogleDrive.add(nameToShow);
        }
        Collections.sort(namesOfFiles);
        listView.setItems(namesOfFiles);
    }
}
