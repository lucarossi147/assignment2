package model;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Task {

    private final String path;
    private final int maxNumberOfThread;
    private int threadWhoAlreadyWorked;
    private boolean done;
    private final Lock mutex;
    private boolean available;

    public Task(String path, int numberOfThread) {
        this.path = path;
        this.done = false;
        this.threadWhoAlreadyWorked = 0;
        this.mutex = new ReentrantLock();
        this.maxNumberOfThread = numberOfThread;
        this.available = true;
    }

    public void incThreadWhoAlreadyWorked(){
        try {
            mutex.lock();
            this.threadWhoAlreadyWorked++;
            if (threadWhoAlreadyWorked == maxNumberOfThread){
                done = true;
            }
        }finally {
            mutex.unlock();
        }
    }

    public boolean isAvailable(){
        try {
            mutex.lock();
            return this.available;
        }finally {
            mutex.unlock();
        }
    }

    public void setUnavailable(){
        try {
            mutex.lock();
            this.available = false;
        }finally {
            mutex.unlock();
        }
    }

    //set number of threads at max-1 to make the task completed when called
    public void workAlone(){
        try {
            mutex.lock();
            this.threadWhoAlreadyWorked = maxNumberOfThread - 1;
        }finally {
            mutex.unlock();
        }
    }

    public boolean isDone(){
        try {
            mutex.lock();
            return this.done;
        }finally {
            mutex.unlock();
        }
    }

    public String getPath(){
        try{
            mutex.lock();
            return  this.path;
        }finally {
            mutex.unlock();
        }
    }
}
