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
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.logging.Level.SEVERE;

public class HelloController implements Initializable {

    @FXML
    private BorderPane borderPane;

    private File fileName;
    
    private FileTime timeItTakes;
    
    public Label statusMessage;
    
    public ProgressBar loadingBar;
    
    public Button loadChangesButton;
    
    public TextArea textArea;


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

    public void initialize() {
        loadChangesButton.setVisible(false);
    }

    public void openFile(ActionEvent event) {
        FileChooser fileReader = new FileChooser();

        fileReader.getExtensionFilters().add( new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));
        fileReader.setInitialDirectory(new File(System.getProperty("user.home")));
        File loadedFile = fileReader.showOpenDialog(null);
        if (loadedFile != null) {
            readFile(loadedFile);
        }
    }

    private void readFile(File fileToLoad) {
        Task<String> task = attemptLoading(fileToLoad);
        loadingBar.progressProperty().bind(task.progressProperty());
        task.run();
    }

    private Task<String> attemptLoading(File fileToLoad) {
        
        Task<String> loadFileTask = new Task<>() {
            
            @Override
            protected String call() throws Exception {
                BufferedReader reader = new BufferedReader(new FileReader(fileToLoad));
                long count;
                try (Stream<String> stream = Files.lines(fileToLoad.toPath())) {
                    count = stream.count();
                }
                
                String l;
                StringBuilder sb = new StringBuilder();
                long lRead = 0;
                while ((l = reader.readLine()) != null) {
                    sb.append(l);
                    sb.append("\n");
                    updateProgress(++lRead, count);
                }
                return sb.toString();
            }
        };
        
        loadFileTask.setOnSucceeded(workerStateEvent -> {
            try {
                textArea.setText(loadFileTask.get());
                statusMessage.setText("File loaded: " + fileToLoad.getName());
                fileName = fileToLoad;
                timeItTakes = Files.readAttributes(fileToLoad.toPath(), BasicFileAttributes.class).lastModifiedTime();
            } catch (ExecutionException | InterruptedException | IOException e) {
                Logger.getLogger(getClass().getName()).log(SEVERE, null, e);
                textArea.setText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
            }
            scheduleFileChecking(fileName);
        });
        

        loadFileTask.setOnFailed(workerStateEvent -> {
            textArea.setText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
            statusMessage.setText("Failed to load file");
        });
        return loadFileTask;
    }

    private void scheduleFileChecking(File file) {
        ScheduledService<Boolean> fileChanger = loadingBar(file);
        fileChanger.setOnSucceeded(workerStateEvent -> {
            if (fileChanger.getLastValue() == null) return;
            if (fileChanger.getLastValue()) {
                //no need to keep checking
                fileChanger.cancel();
                notifyUserOfChanges();
            }
        });
        System.out.println("Starting Checking Service...");
        fileChanger.start();
    }

    private ScheduledService<Boolean> loadingBar(File file) {
        ScheduledService<Boolean> scheduledService = new ScheduledService<>() {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        FileTime timeFromNow = Files.readAttributes(file.toPath(), BasicFileAttributes.class).lastModifiedTime();
                        return timeFromNow.compareTo(timeItTakes) > 0;
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

    public void saveFile(ActionEvent event) {
        try {
            FileWriter myWriter = new FileWriter(fileName);
            myWriter.write(textArea.getText());
            myWriter.close();
            timeItTakes = FileTime.fromMillis(System.currentTimeMillis() + 3000);
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(SEVERE, null, e);
        }
    }

}