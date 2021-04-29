package model;

import view.View;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RankMonitorImpl implements RankMonitor {

    private final HashMap<String, Integer> rank;
    private final Lock mutex;
    private final boolean stop;
    private int totalWords;
    private View view;

    public RankMonitorImpl(){
        mutex = new ReentrantLock();
        rank = new HashMap<>();
        stop = false;
        totalWords = 0;
    }

    public void setView(View view){
        this.view = view;
     }

    public void update(HashMap<String, Integer> pageRank) {
        try {
            mutex.lock();
			if(!stop){
                for (String s: pageRank.keySet()) {
                    int instancesOfThisWord = pageRank.get(s);
                    if(rank.containsKey(s)){
                        rank.put(s,rank.get(s) + instancesOfThisWord);
                    } else {
                        rank.put(s, instancesOfThisWord);
                    }
                    totalWords += instancesOfThisWord;
                }
            }
            notifyView();
		} finally {
            mutex.unlock();
		}
    }

    public Map<String, Integer> viewMostFrequentN(int n) {
        try{
            mutex.lock();
            Map<String, Integer> sortedMap = new LinkedHashMap<>();
            rank.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(n)
                    .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

            sortedMap.put("TOTAL_WORDS", totalWords);
            return sortedMap;
        } finally {
            mutex.unlock();
        }
    }

    @Override
    public void reset() {
        try {
            mutex.lock();
            rank.clear();
            this.totalWords = 0;
        }finally {
            mutex.unlock();
        }
    }

    private void notifyView(){
        if (this.view!=null){
            view.rankUpdated();
        }
    }
}