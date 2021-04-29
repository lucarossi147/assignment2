package model;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MyRunnable implements Runnable{

    private static final int MAX_TO_WAIT_BEFORE_UPDATING = 500;
    private final Manager manager;
    private final RankMonitor rankMonitor;
    private final int myPosition;
    private final List<String> unwantedWords;
    private final int numberOfThreads;
    private PDFTextStripper stripper;
    private CompletableFuture<Boolean> future;
    private final Task task;

    public MyRunnable(CompletableFuture<Boolean> future, int myPosition, Manager manager, RankMonitor rankMonitor, List<String> unwantedWords, int numberOfThreads, Task task){
        this.future = future;
        this.manager = manager;
        this.myPosition = myPosition;
        this.rankMonitor = rankMonitor;
        this.unwantedWords = unwantedWords;
        this.numberOfThreads = numberOfThreads;
        this.task = task;
        try {
            this.stripper = new PDFTextStripper();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Optional<Page> currentPage = Optional.empty();
        if(!task.isDone() && task.isAvailable()){
            if (manager.isComputationStopped()){
                currentPage = read(task);
            }
            if (currentPage.isPresent() && manager.isComputationStopped()){
                analyze(currentPage.get());
            }
            task.incThreadWhoAlreadyWorked();
        }
        future.complete(true);
        //System.out.println("Thread "+getName()+" completed his job... exiting");
    }

    private Optional<Page> read(Task task){
        try {
            PDDocument document = PDDocument.load(new File(task.getPath()));
            Optional<Page> extractedPage = Optional.empty();
            if (document.getNumberOfPages() >= numberOfThreads){
                //System.out.println("Thread "+getName()+" begun to read file "+ task.getPath()+ "with "+document.getNumberOfPages()+" pages");
                Map<String, Integer> fromToMap = getRange(document.getNumberOfPages());
                extractedPage = extractPage(document,fromToMap.get("from"),fromToMap.get("to"));
            }else if (document.getNumberOfPages() < numberOfThreads && myPosition == 0){
                //if it works alone set the task unavailable for the others
                task.setUnavailable();
                task.workAlone();
                //System.out.println("Thread "+getName()+" begun to read file "+ task.getPath());
                extractedPage = extractPage(document, 1, document.getNumberOfPages());
            }
            document.close();
            return extractedPage;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error: file not found");
        }
        return Optional.empty();
    }

    private Optional<Page> extractPage(PDDocument document, int from, int to) throws IOException {
        if (document.getCurrentAccessPermission().canExtractContent()){
            stripper.setStartPage(from);
            stripper.setEndPage(to);
            Page p = new Page(stripper.getText(document).trim());
            //System.out.println("Thread "+ getName()+ " read his part of the file");
            return Optional.of(p);
        }else{
            System.out.println("Couldn't extract content of file");
            return Optional.empty();
        }
    }

    private void analyze(Page page){
        HashMap<String, Integer> pageRank = new HashMap<>();
        long end;
        List<String> words =  page.getRelevantWords(unwantedWords);
        long start = System.currentTimeMillis();
        for (String word : words){
            update(pageRank, word);
            end = System.currentTimeMillis();
            if (end-start > MAX_TO_WAIT_BEFORE_UPDATING){
                rankMonitor.update(pageRank);
                pageRank.clear();
                start = 0;
            }
        }
        rankMonitor.update(pageRank);
    }

    private void update(HashMap<String, Integer> pageRank, String word){
        if(pageRank.containsKey(word)){
            pageRank.put(word, (pageRank.get(word))+1);
        } else {
            pageRank.put(word,1);
        }
    }

    private HashMap<String, Integer> getRange(int numberOfPages){
        HashMap<String, Integer> fromToMap = new HashMap<>();
        List<Integer> qzAndRest = divideEqually(numberOfPages);
        int qz = qzAndRest.get(0);
        int remaining = qzAndRest.get(1);
        if (myPosition == 0){
            fromToMap.put("from", 1);
            fromToMap.put("to", qz + remaining);
        } else {
            int from = (remaining)+( qz * myPosition);
            int to = from + qz;
            from ++;
            fromToMap.put("from", from);
            fromToMap.put("to", to);
        }
        return  fromToMap;
    }
    private List<Integer> divideEqually(int divideEqually){
        List<Integer> pagesForThread = new ArrayList<>();
        pagesForThread.add(divideEqually / numberOfThreads);
        pagesForThread.add(divideEqually % numberOfThreads);
        return pagesForThread;
    }
}
