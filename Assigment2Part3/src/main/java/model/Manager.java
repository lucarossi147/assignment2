package model;

import view.View;

import java.util.LinkedList;
import java.util.List;


public class Manager {

    private View view;
    private final List<Task> tasks = new LinkedList<>();
    private List<String> unwantedWords;
    private int madeTasks;
    private boolean computation;

    public Manager() {
        unwantedWords = new LinkedList<>();
        computation = true;
    }

    public void add(Task e){
        tasks.add(e);
    }

    public List<Task> getTasks(){
        return this.tasks;
    }

    public void clear(){
        this.tasks.clear();
        this.madeTasks = 0;
        computation = true;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public void setUnwantedWords(List<String> unwantedWords) {
        this.unwantedWords = unwantedWords;
    }

    public List<String> getUnwantedWords() {
        return unwantedWords;
    }

    public void incMadeTasks(){
        this.madeTasks++;
        if (this.madeTasks == tasks.size()){
            view.setStartButtonStatus(true);
        }
    }

    public void stopComputation(){
        this.computation = false;
    }

    public boolean getComputation() {
        return computation;
    }
}
