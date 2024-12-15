import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

public class GoogleDriveClass {
    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    //путь до файла токена
    public static String TOKENS_DIRECTORY_PATH = "";

    //путь до файла, в котором хранится путь до файла токена
    public static String FILE_WITH_TOKENS_DIRECTORY_PATH = "";

    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE, DriveScopes.DRIVE_METADATA);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    public static Drive service = null;

    public static String userType = "";

    public static Credential credential = null;

    private static Credential getCredentials(NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveClass.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        //returns an authorized Credential object.
        return credential;
    }

    public static void authorization() throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static List<File> getFiles() throws IOException, GeneralSecurityException {

        FileList result = service.files().list()
                .setQ("'root' in parents and (" +
                        "mimeType = 'application/vnd.google-apps.folder' or " +
                        "mimeType = 'application/msword' or " +
                        "mimeType = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' or " +
                        "mimeType = 'application/vnd.google-apps.document' or " +
                        "mimeType = 'application/pdf' or " +
                        "mimeType = 'text/plain' or " +
                        "mimeType = 'text/csv')  and trashed = false and" +
                        "properties has { key='identifier' and value='ourcompanyid' }")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name, parents, owners, mimeType)")
                .execute();

        System.out.println(result);
        List<File> files = result.getFiles();

        userType = "root";

        //проверяем раздел "доступные мне"
        if (files.isEmpty()) {
            files = getFilesFromSharedDrive();
            userType = "user";
        }

        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {

                byte[] bytes = file.getName().getBytes(StandardCharsets.UTF_8);
                String fileName = new String(bytes, "cp1251");
            }
        }
        return files;
    }

    public static List<File> getFilesFromSharedDrive() throws IOException {
        //проверка, если на "моем" диске нет таких файлов, то ищем в "доступные мне"
        FileList result = service.files().list()
                .setQ("sharedWithMe and (mimeType = 'application/vnd.google-apps.folder' or " +
                        "mimeType = 'application/msword' or " +
                        "mimeType = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' or " +
                        "mimeType = 'application/vnd.google-apps.document' or " +
                        "mimeType = 'application/pdf' or " +
                        "mimeType = 'text/plain' or " +
                        "mimeType = 'text/csv') and " +
                        "properties has { key='identifier' and value='ourcompanyid' }")
                .setIncludeItemsFromAllDrives(true)
                .setSupportsAllDrives(true)
                .setFields("nextPageToken, files(id, name, parents, owners, mimeType)")
                .execute();

        System.out.println(result);

        return result.getFiles();
    }

    public static List<File> getFilesFromFolder(String folderId) throws IOException, GeneralSecurityException {
        FileList result = service.files().list()
                .setQ("'" + folderId + "' in parents and (" +
                        "mimeType = 'application/vnd.google-apps.folder' or " +
                        "mimeType = 'application/msword' or " +
                        "mimeType = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' or " +
                        "mimeType = 'application/vnd.google-apps.document' or " +
                        "mimeType = 'application/pdf' or " +
                        "mimeType = 'text/plain' or " +
                        "mimeType = 'text/csv')  and trashed = false and" +
                        "properties has { key='identifier' and value='ourcompanyid' }")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name, parents, owners, mimeType)")
                .execute();

        System.out.println(result);
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
        }
        return files;
    }

    public static void uploadFile(java.io.File file, String mimeType, String folderToLoad) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(file.getName());
        fileMetadata.setParents(Collections.singletonList(folderToLoad));

        HashMap<String, String> propertyOfFolder = new HashMap<>();
        propertyOfFolder.put("identifier", "ourcompanyid");

        //добавление пользовательских свойств файла для идентификации
        fileMetadata.setProperties(propertyOfFolder);

        // File's content.
        java.io.File filePath = new java.io.File(file.getPath());
        // Specify media type and file-path for file.
        FileContent mediaContent = new FileContent(mimeType, filePath);
        try {
            File fileNew = service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            System.out.println("File ID: " + fileNew.getId());
        } catch (GoogleJsonResponseException e) {
            System.err.println("Unable to upload file: " + e.getDetails());
            throw e;
        }
    }

    public static void downloadFile(String fileName, String fileId, String mimeType) throws IOException {
        OutputStream outputStream = new FileOutputStream("D:/Desktop/" + fileName);
        service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        outputStream.flush();
        outputStream.close();
    }

    public static void createMainFolder(String folderName) throws IOException {
        String rootFolderId = service.files().list()
                .setQ("'root' in parents")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, parents)")
                .execute().getFiles().get(0).getParents().get(0);

        createFolder(folderName, rootFolderId);
    }

    public static void createFolder(String name, String folderToCreate) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setParents(Collections.singletonList(folderToCreate));

        HashMap<String, String> propertyOfFolder = new HashMap<>();
        propertyOfFolder.put("identifier", "ourcompanyid");

        //добавление пользовательских свойств файла для идентификации
        fileMetadata.setProperties(propertyOfFolder);

        service.files().create(fileMetadata).setFields("id").execute();
    }

    public static void copyAndPasteFile(String fileId, String fileName, String folderToPaste) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(folderToPaste));

        HashMap<String, String> propertyOfFolder = new HashMap<>();
        propertyOfFolder.put("identifier", "ourcompanyid");

        //добавление пользовательских свойств файла для идентификации
        fileMetadata.setProperties(propertyOfFolder);

        service.files().copy(fileId, fileMetadata).execute();
    }

    public static void deleteFile(String fileId) throws IOException {
        service.files().delete(fileId).execute();
    }

    public static boolean getOwnerOfTheFile(String fileId) throws IOException {

        User owner = service.files().get(fileId).setFields("owners").execute().getOwners().get(0);

        return owner.getMe();
    }

    public static void renameFile(String fileId, String newName) throws IOException {
        File file = service.files().get(fileId).setFields("name").execute();

        file.setName(newName);

        service.files().update(fileId, file).execute();
    }
}
