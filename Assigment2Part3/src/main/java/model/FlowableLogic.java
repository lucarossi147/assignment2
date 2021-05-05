package model;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import view.View;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FlowableLogic {

    private Manager manager;
    private List<String> unwantedWords;
    private View view;
    private Map<String, Integer> globalRank;
    private int totalWords;

    public FlowableLogic(Manager manager){
        this.manager = manager;
        this.view = manager.getView();
        this.globalRank = new LinkedHashMap<>();
        this.unwantedWords = manager.getUnwantedWords();
    }

    public void start(){
        this.globalRank.clear();
        this.totalWords = 0;
        this.unwantedWords = manager.getUnwantedWords();
        Flowable.fromIterable(manager.getTasks()).forEach(task -> {
            Flowable.just(task)
                    .subscribeOn(Schedulers.io())
                    .map(this::read) //Task -> Optional<Page
                    .subscribeOn(Schedulers.computation())
                    .map(this::analyze)
                    .map(this::addAll)
                    .subscribe();
        });
    }

    private Optional<Page> read(Task task){
        //log("reading the docunent");
        if(manager.getComputation()){
            try{
                PDDocument document = PDDocument.load(new File(task.getPath()));
                var page = extractPage(document, 1, document.getNumberOfPages());
                document.close();
                return page;
            } catch (IOException e){
                e.printStackTrace();
            }
            return Optional.empty();
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
        HashMap<String, Integer>pageRank = new HashMap<>();
        if(manager.getComputation()){
            //log("analyzing the page");
            if (page.isPresent()){
                List<String> words =  page.get().getRelevantWords(this.unwantedWords);
                for (String word : words){
                    update(pageRank, word);
                }
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

    private synchronized Map<String, Integer> addAll(Map<String, Integer> pageRank){
        for (String s: pageRank.keySet()) {
            int instancesOfThisWord = pageRank.get(s);
            if(globalRank.containsKey(s)){
                globalRank.put(s,globalRank.get(s) + instancesOfThisWord);
            } else {
                globalRank.put(s, instancesOfThisWord);
            }
            totalWords += instancesOfThisWord;
        }
        LinkedHashMap<String,Integer> finalRank = sortRank(globalRank);
        view.updateGUI(finalRank);
        manager.incMadeTasks();
        return sortRank(globalRank);
    }

    private LinkedHashMap<String, Integer> sortRank(Map<String, Integer> map){
        LinkedHashMap<String, Integer> sortedMap = new LinkedHashMap<>();
        map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
        sortedMap.put("TOTAL_WORDS", totalWords);
        return sortedMap;
    }

    static private void log(String msg) {
        System.out.println("[ " + Thread.currentThread().getName() + "  ] " + msg);
    }
}
