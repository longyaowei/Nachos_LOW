package nachos.threads;

import nachos.machine.*;
import java.util.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    private PriorityQueue<AlarmQueueEntry> alarmQueue;

    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });

    alarmQueue = new PriorityQueue<AlarmQueueEntry>();
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
	//KThread.currentThread().yield();
    
    boolean intStatus = Machine.interrupt().disable();

    while (!alarmQueue.isEmpty()){
        AlarmQueueEntry entry = alarmQueue.peek();
        if (entry.wakeTime > Machine.timer().getTime()) break;
        entry.waitThread.ready();
        alarmQueue.remove();
    }

    Machine.interrupt().restore(intStatus);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	boolean intStatus = Machine.interrupt().disable();

    long wakeTime = Machine.timer().getTime() + x;
	//while (wakeTime > Machine.timer().getTime())
	//    KThread.yield();
    alarmQueue.add(new AlarmQueueEntry(KThread.currentThread(), wakeTime));
    KThread.sleep();

    Machine.interrupt().restore(intStatus);
    }

    public class AlarmQueueEntry implements Comparable<AlarmQueueEntry>{
        private KThread waitThread;
        private long wakeTime;

        public AlarmQueueEntry(KThread waitThread, long wakeTime){
            this.waitThread = waitThread;
            this.wakeTime = wakeTime;
        }

        public int compareTo(AlarmQueueEntry other){
            if (this.wakeTime > other.wakeTime) return 1;
            if (this.wakeTime < other.wakeTime) return -1;
            return 0;
        }
    }

    public static void sleepAWhile(long sleepTime){
        long timeStamp1 = Machine.timer().getTime();
        System.out.println(KThread.currentThread().toString() + ": going to sleep (time " + Long.toString(timeStamp1) + ")");
        ThreadedKernel.alarm.waitUntil(sleepTime);
        long timeStamp2 = Machine.timer().getTime();
        System.out.println(KThread.currentThread().toString() + ": wake up (time " + Long.toString(timeStamp2) + ")");
        System.out.println(KThread.currentThread().toString() + ": planned sleep time " + Long.toString(sleepTime) 
            + "; real sleep time " + Long.toString(timeStamp2-timeStamp1));
    }
    
    public static void selfTest(){
        System.out.println("\n\nAlarm Tests");

        KThread thread0 = new KThread(new Runnable(){
            public void run(){
                sleepAWhile(8000);
            }
        }).setName("AlarmTest Thread0");
        
        KThread thread1 = new KThread(new Runnable(){
            public void run(){
                sleepAWhile(4000);
            }
        }).setName("AlarmTest Thread1");

        KThread thread2 = new KThread(new Runnable(){
            public void run(){
                sleepAWhile(10000);
            }
        }).setName("AlarmTest Thread2");

        KThread thread3 = new KThread(new Runnable(){
            public void run(){
                sleepAWhile(2000);
            }
        }).setName("AlarmTest Thread3");

        KThread thread4 = new KThread(new Runnable(){
            public void run(){
                sleepAWhile(6000);
            }
        }).setName("AlarmTest Thread4");

        thread0.fork();
        thread1.fork();
        thread2.fork();
        thread3.fork();
        thread4.fork();
    
        thread0.join();
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
        
    }
}
