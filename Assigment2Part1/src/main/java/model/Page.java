package model;

import java.util.Arrays;
import java.util.List;

public class Page {

    private String page;

    public Page(String page){
        this.page = page;

    }

    /**
     * remove unwanted words from page and return all the other words as a List of String
     * @param unwantedWords words to delete from string
     * @return string without unwantedWords
     */
    public List<String> getRelevantWords(List<String> unwantedWords){
        this.page = page.toLowerCase();
        this.page = page.replaceAll("[^\\p{L}\\p{Nd}]+", " ");
        for(String s : unwantedWords){
            page = page.replace(" "+s.toLowerCase()+" ", " ");
        }
        return Arrays.asList(page.split(" +"));
    }
}
