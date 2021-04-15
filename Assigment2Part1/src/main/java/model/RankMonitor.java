package model;

import view.View;

import java.util.HashMap;
import java.util.Map;

public interface RankMonitor {

    void update(HashMap<String, Integer> pageRank);

    Map<String, Integer> viewMostFrequentN(int n);

    void reset();

    void setView(View view);
}
