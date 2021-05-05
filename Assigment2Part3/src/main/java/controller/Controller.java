package controller;


import model.*;
import view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Controller {
    private final View view;
    private final Manager manager;
    private int nThreads = 0;
    private FlowableLogic rankManagement;

    public Controller(){
        this.manager = new Manager();
        this.view = new View(this);
        manager.setView(view);
        rankManagement = new FlowableLogic(manager);
    }

    public void processEvent(String event, String path){
        final String pathFinal = cleanPath(path);

        switch(event){
            case "start":
                long start = System.currentTimeMillis();
                view.setStartButtonStatus(false);
                view.reset();

                manager.clear();
                createTasks(pathFinal, nThreads);
                //read ignored.txt
                manager.setUnwantedWords(getFromIgnoreText(view.getIgnorePath()));
                rankManagement.start();
                break;

            case "stop":
                manager.stopComputation();
                view.setStartButtonStatus(true);
                break;
            default:
                throw new IllegalStateException("Error on action!");
        }
    }

    private String cleanPath(String path) {
        if(path.endsWith("/")){
            path = path.substring(0,path.length()-1);
        }
        return path;
    }

    private void createTasks(String path, int nThread){
        try {
            Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".pdf"))
                    .collect(Collectors.toSet())
                    .forEach(p ->  manager.add(new Task(String.valueOf(p), nThread)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getFromIgnoreText(String fileName) {
        File testFile = new File(fileName);
        Scanner inputFile = null;
        try {
            inputFile = new Scanner(testFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        List<String> words = new ArrayList<>();
        if (inputFile != null) {
            while (inputFile.hasNext()) {
                words.add(inputFile.nextLine());
            }
        } else {
            System.out.println("File Doesn't Exist");
        }
        return words;
    }
}
