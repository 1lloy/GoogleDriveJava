import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class StartApp extends Application {
    public static Stage mainStage;
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        boolean tokenPathFileExist = false;
        //нужно получить список дисков, желательно искать C, затем Program Files, при первом запуске создать там файл pathToToken
        File[] roots = File.listRoots();
        boolean existCRoot = false;
        String rootPath = "";
        ArrayList<String> rootPathsWithoutC = new ArrayList<>();

        for (File root : roots){
            if (root.getPath().equals("C:\\")){
                existCRoot = true;
                rootPath = root.getPath();
                System.out.println(rootPath);
                break;
            }
            else{
                rootPathsWithoutC.add(root.getName());
            }
        }

        if (!existCRoot) {
            rootPath = rootPathsWithoutC.get(0);
        }

        //создаем папку на нужном корневом диске
        File newFolder = new File(rootPath, "loadDocsProject");
        if (!newFolder.exists()){
            newFolder.mkdir();

            //создаем в папке нужный файл, хранящий путь
            String pathWithFolder = rootPath + "loadDocsProject\\pathToToken";
            File pathToTokenFile = new File(pathWithFolder);

            pathToTokenFile.createNewFile();

            //обновляем поле, хранящее путь до файла с путём до токена
            GoogleDriveClass.FILE_WITH_TOKENS_DIRECTORY_PATH = pathWithFolder;
        }
        else{
            System.out.println("Folder already exists");

            //создаем в папке нужный файл, хранящий путь
            String pathWithFolder = rootPath + "loadDocsProject\\pathToToken";
            File pathToTokenFile = new File(pathWithFolder);

            if (pathToTokenFile.createNewFile()){
                //файл создан
            }
            else{
                System.out.println("File already exists");
                //файл с путём до токена может существовать, но самого токена может не быть
                //нужно проверять, существует ли файл по пути ~/StoredCredential

                //взяли строку из файла pathToToken
                File file = new File(pathWithFolder);
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                String line;
                while((line = br.readLine()) != null){
                    GoogleDriveClass.TOKENS_DIRECTORY_PATH = line;
                }
                br.close();
                fr.close();
                //если строка не пустая, то проверяем наличие этого файла
                String pathFromFile = GoogleDriveClass.TOKENS_DIRECTORY_PATH;
                if (!pathFromFile.isEmpty()){
                    file = new File(pathFromFile + "\\StoredCredential");
                    if (file.exists()){
                        //файл с токеном существует, даём разрешение на обычный запуск
                        tokenPathFileExist = true;
                        System.out.println("File exists");
                    }
                }
            }

            //обновляем поле, хранящее путь до файла с путём до токена
            GoogleDriveClass.FILE_WITH_TOKENS_DIRECTORY_PATH = pathWithFolder;
        }

        //если нет файла с путём, значит идем на создание
        if (!tokenPathFileExist){
            //открываем окно выбора типа пользователя
            mainStage = primaryStage;
            FXMLLoader fxmlLoader = new FXMLLoader(StartApp.class.getResource("userType.fxml"));
            Scene mainScene = new Scene(fxmlLoader.load());
            mainStage.setTitle("Choose type");
            mainStage.setScene(mainScene);
            mainStage.show();

            System.out.println("Еще не входил");
        }
        else{
            System.out.println("Входил уже");

            //файл с путем до токена существует, значит нужно взять этот путь из файла
            File file = new File(GoogleDriveClass.FILE_WITH_TOKENS_DIRECTORY_PATH);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while((line = br.readLine()) != null){
                GoogleDriveClass.TOKENS_DIRECTORY_PATH = line;
            }
            br.close();
            fr.close();

            //далее открываем главное окно
            mainStage = primaryStage;
            FXMLLoader fxmlLoader = new FXMLLoader(StartApp.class.getResource("startWindow.fxml"));
            Scene mainScene = new Scene(fxmlLoader.load());
            mainStage.setTitle("Documents");
            mainStage.setScene(mainScene);
            mainStage.show();
        }
    }
}
