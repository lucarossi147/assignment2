package model;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Manager {

    private final List<Task> tasks = new LinkedList<>();
    private final Lock mutex;
    private boolean stop = false;

    public Manager() {
        mutex = new ReentrantLock();
    }

    public void add(Task e){
        try{
            mutex.lock();
            tasks.add(e);
        } finally {
            mutex.unlock();
        }
    }

    public List<Task> getTasks(){
        try{
            mutex.lock();
            return this.tasks;
        } finally {
            mutex.unlock();
        }
    }

    public void stop(){
        try{
            mutex.lock();
            this.stop = true;
        } finally {
            mutex.unlock();
        }
    }

    public boolean isComputationStopped(){
        try{
            mutex.lock();
            return !this.stop;
        } finally {
            mutex.unlock();
        }
    }

    public void clear(){
        try{
            mutex.lock();
            this.tasks.clear();
            this.stop = false;
        } finally {
            mutex.unlock();
        }

    }
}
