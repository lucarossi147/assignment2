package model;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AnalyzeRunnable implements Runnable{

    private static final int MAX_TO_WAIT_BEFORE_UPDATING = 500;
    private final Page page;
    private final Manager manager;
    private final RankMonitor rankMonitor;
    private final List<String> unwantedWords;

    public AnalyzeRunnable(Page page, RankMonitor rankMonitor, List<String> unwantedWords, Manager manager) {
        this.page = page;
        this.manager = manager;
        this.rankMonitor = rankMonitor;
        this.unwantedWords = unwantedWords;
    }

    @Override
    public void run() {
        if (manager.isComputationStopped()){
            analyze(page);
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
}
