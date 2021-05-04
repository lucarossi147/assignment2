package controller;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Controller {
    private final View view;
    private final RankMonitor monitor;
    private final Manager manager;
    private int nThreads = 0;
    private List<String> unwantedWords = new ArrayList<>();


    private LinkedHashMap<String, Integer> globalRank = new LinkedHashMap<>();
    private int totalWords = 0;

    public Controller(){
        this.manager = new Manager();
        this.monitor = new RankMonitorImpl();
        this.view = new View(this, monitor);
        monitor.setView(view);
    }

    public void processEvent(String event, String path){
        final String pathFinal = cleanPath(path);
        nThreads = Runtime.getRuntime().availableProcessors();

        switch(event){
            case "start":
                log("main thread");
                long start = System.currentTimeMillis();
                view.setStartButtonStatus(false);
                view.reset();
                try {
                    new Thread(() -> {
                        manager.clear();
                        monitor.reset();

                        createTasks(pathFinal, nThreads);

                        //read ignore.txt
                        this.unwantedWords = getFromIgnoreText(view.getIgnorePath());

                        /*Flowable.fromIterable(manager.getTasks()).forEach(task -> {
                            Flowable.just(task)
                                    .subscribe(t -> {
                                        for (int i = 0; i < nThreads; i++) {
                                            new MyRunnable(i, manager, monitor, unwantedWords, nThreads, t).run();
                                        }
                                    });

                        });*/

                        Flowable.fromIterable(manager.getTasks()).forEach(task -> {
                                    Flowable.just(task)
                                            .subscribeOn(Schedulers.io())
                                            .map(this::read) //Task -> Optional<Page
                                            .subscribeOn(Schedulers.computation())
                                            .map(this::analyze)
                                            .map(this::addAll)
                                            .subscribe();
                                });

                        view.setStartButtonStatus(true);
                        System.out.println("Time elapsed "+ (System.currentTimeMillis() - start));
                    }).start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;

            case "stop":
                manager.stop();
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

    private Optional<Page> read(Task task){
        log("reading the docunent");
        try{
            PDDocument document = PDDocument.load(new File(task.getPath()));
            return extractPage(document, 1, document.getNumberOfPages());
        } catch (IOException e){
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private Optional<Page> extractPage(PDDocument document, int from, int to) throws IOException {
        if (document.getCurrentAccessPermission().canExtractContent()){
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(from);
            stripper.setEndPage(to);
            Page p = new Page(stripper.getText(document).trim());
            return Optional.of(p);
        }else{
            System.out.println("Couldn't extract content of file");
            return Optional.empty();
        }
    }

    private HashMap<String, Integer> analyze(Optional<Page> page){
        log("analyzing the page");
        HashMap<String, Integer>pageRank = new HashMap<>();
        if (page.isPresent()){
            List<String> words =  page.get().getRelevantWords(this.unwantedWords);
            for (String word : words){
                update(pageRank, word);
            }
        }
        return pageRank;
    }

    private void update(HashMap<String, Integer> pageRank, String word){
        if(pageRank.containsKey(word)){
            pageRank.put(word, (pageRank.get(word))+1);
        } else {
            pageRank.put(word,1);
        }
    }

    private Map<String, Integer> addAll(Map<String, Integer> map){
        for (String s: map.keySet()) {
            int instancesOfThisWord = map.get(s);
            if(globalRank.containsKey(s)){
                globalRank.put(s,globalRank.get(s) + instancesOfThisWord);
            } else {
                globalRank.put(s, instancesOfThisWord);
            }
            totalWords += instancesOfThisWord;
        }
        return sortRank(globalRank);
    }

    private Map<String, Integer> sortRank(Map<String, Integer> map){
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

        sortedMap.put("TOTAL_WORDS", totalWords);
        int i =0;
        for (String s : sortedMap.keySet()){
            if(i<10){
                System.out.println("parola: "+ s.toUpperCase() + " comparsa: " + sortedMap.get(s)+ " volte");
                i++;
            } else {
                break;
            }
        }
        return sortedMap;
    }

    static private void log(String msg) {
        System.out.println("[ " + Thread.currentThread().getName() + "  ] " + msg);
    }

}
