package com.example.javafx;

import Visuals.main.Main;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.logging.Level.SEVERE;

public class HelloController implements Initializable {
    @FXML
    private ImageView perlin;

    @FXML
    private BorderPane borderPane;

    @FXML
    private TextField gravity;

    @FXML
    private TextField mass;

    @FXML
    private TextField friction;

    @FXML
    private TextField startPosX;

    @FXML
    private TextField goalPosX;

    @FXML
    private TextField startPosY;

    @FXML
    private TextField goalPosY;

    @FXML
    private TextField treePosX;

    @FXML
    private TextField treePosY;

    @FXML
    private TextField sandTopX;

    @FXML
    private TextField sandTopY;

    @FXML
    private TextField sandBtmX;

    @FXML
    private TextField sandBtmY;

    @FXML
    private TextField radius;

    @FXML
    private TextField maxSpeed;

    @FXML
    private TextField heightProfile;

    @FXML
    private TextField courseCreator;

    @FXML
    private TextField listMoves;

    @FXML
    private TextField heightMapp;

    @FXML
    private TextField widthString;

    @FXML
    private TextField heightString;



    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }




    @FXML
    private void applyMan() throws IOException {
        new Main().start();
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {}

    @FXML
    private void start() {
        UILoader("Finish");
    }

    @FXML
    private void manInput(){
        UILoader("ManualInputScreen");
    }

    @FXML
    private void fileRead(){
        UILoader("SimpleFileEditor");
    }

    @FXML
    private void exit(){
        Stage stage = (Stage) borderPane.getScene().getWindow();
        stage.close();
    }






    private void UILoader(String scene){
        Parent root = null;
        try{
            root = FXMLLoader.load(getClass().getResource(scene+".fxml"));
        } catch (IOException ex){
            Logger.getLogger(ModuleLayer.Controller.class.getName()).log(SEVERE, null, ex);
        }
        borderPane.setCenter(root);
    }


    private File loadedFileReference;
    private FileTime lastModifiedTime;
    public Label statusMessage;
    public ProgressBar progressBar;
    public Button loadChangesButton;
    public TextArea textArea;


    public void initialize() {
        loadChangesButton.setVisible(false);
    }

    public void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        //only allow text files to be selected using chooser
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt")
        );
        //set initial directory somewhere user will recognise
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        //let user select file
        File fileToLoad = fileChooser.showOpenDialog(null);
        //if file has been chosen, load it using asynchronous method (define later)
        if (fileToLoad != null) {
            loadFileToTextArea(fileToLoad);
        }
    }

    private void loadFileToTextArea(File fileToLoad) {
        Task<String> loadTask = fileLoaderTask(fileToLoad);
        progressBar.progressProperty().bind(loadTask.progressProperty());
        loadTask.run();
    }

    private Task<String> fileLoaderTask(File fileToLoad) {
        //Create a task to load the file asynchronously
        Task<String> loadFileTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                BufferedReader reader = new BufferedReader(new FileReader(fileToLoad));
                //Use Files.lines() to calculate total lines - used for progress
                long lineCount;
                try (Stream<String> stream = Files.lines(fileToLoad.toPath())) {
                    lineCount = stream.count();
                }
                //Load in all lines one by one into a StringBuilder separated by "\n" - compatible with TextArea
                String line;
                StringBuilder totalFile = new StringBuilder();
                long linesLoaded = 0;
                while ((line = reader.readLine()) != null) {
                    totalFile.append(line);
                    totalFile.append("\n");
                    updateProgress(++linesLoaded, lineCount);
                }
                return totalFile.toString();
            }
        };
        //If successful, update the text area, display a success message and store the loaded file reference
        loadFileTask.setOnSucceeded(workerStateEvent -> {
            try {
                textArea.setText(loadFileTask.get());
                statusMessage.setText("File loaded: " + fileToLoad.getName());
                loadedFileReference = fileToLoad;
                lastModifiedTime = Files.readAttributes(fileToLoad.toPath(), BasicFileAttributes.class).lastModifiedTime();
            } catch (InterruptedException | ExecutionException | IOException e) {
                Logger.getLogger(getClass().getName()).log(SEVERE, null, e);
                textArea.setText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
            }
            scheduleFileChecking(loadedFileReference);
        });
        //If unsuccessful, set text area with error message and status message to failed
        loadFileTask.setOnFailed(workerStateEvent -> {
            textArea.setText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
            statusMessage.setText("Failed to load file");
        });
        return loadFileTask;
    }

    private void scheduleFileChecking(File file) {
        ScheduledService<Boolean> fileChangeCheckingService = createFileChangesCheckingService(file);
        fileChangeCheckingService.setOnSucceeded(workerStateEvent -> {
            if (fileChangeCheckingService.getLastValue() == null) return;
            if (fileChangeCheckingService.getLastValue()) {
                //no need to keep checking
                fileChangeCheckingService.cancel();
                notifyUserOfChanges();
            }
        });
        System.out.println("Starting Checking Service...");
        fileChangeCheckingService.start();
    }

    private ScheduledService<Boolean> createFileChangesCheckingService(File file) {
        ScheduledService<Boolean> scheduledService = new ScheduledService<>() {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        FileTime lastModifiedAsOfNow = Files.readAttributes(file.toPath(), BasicFileAttributes.class).lastModifiedTime();
                        return lastModifiedAsOfNow.compareTo(lastModifiedTime) > 0;
                    }
                };
            }
        };
        scheduledService.setPeriod(Duration.seconds(1));
        return scheduledService;
    }

    private void notifyUserOfChanges() {
        loadChangesButton.setVisible(true);
    }

    public void loadChanges(ActionEvent event) {
        loadFileToTextArea(loadedFileReference);
        loadChangesButton.setVisible(false);
    }

    public void saveFile(ActionEvent event) {
        try {
            FileWriter myWriter = new FileWriter(loadedFileReference);
            myWriter.write(textArea.getText());
            myWriter.close();
            lastModifiedTime = FileTime.fromMillis(System.currentTimeMillis() + 3000);
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(SEVERE, null, e);
        }
    }






}