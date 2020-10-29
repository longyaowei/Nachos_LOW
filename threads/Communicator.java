package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */

    private Lock lock;
    private Condition speaker;
    private Condition listener;
    private Condition goodBye;
    private int words;
    private int speakers;
    private int listeners;
    private int word;

    public Communicator() {
        lock = new Lock();
        speaker = new Condition(lock);
        listener = new Condition(lock);
        goodBye = new Condition(lock);
        words = 0;
        speakers = 0;
        listeners = 0;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();
        
        while (words > 0){
            speakers ++;
            speaker.sleep();
        }

        this.word = word;
        words ++;

        if (listeners > 0){
            listener.wake();
            listeners --;
        }
        goodBye.sleep();

        lock.release();

    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        lock.acquire();

        while (words == 0){
            listeners ++;
            listener.sleep();
        }

        int myWord = word;
        words --;

        if (speakers > 0){
            speaker.wake();
            speakers --;
        }

        goodBye.wake();

        lock.release();
        return myWord;
	}

    public void selfTest(){
        System.out.println("\n\nCommunicator Test");

        KThread speaker0 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().toString() + ": try to speak 0");
                speak(0);
                System.out.println(KThread.currentThread().toString() + ": finish speaking 0");
            }
        }).setName("Speaker0");

        KThread speaker1 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().toString() + ": try to speak 1");
                speak(1);
                System.out.println(KThread.currentThread().toString() + ": finish speaking 1");
            }
        }).setName("Speaker1");

        KThread speaker2 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().toString() + ": try to speak 2");
                speak(2);
                System.out.println(KThread.currentThread().toString() + ": finish speaking 2");
            }
        }).setName("Speaker2");

        KThread speaker3 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().toString() + ": try to speak 3");
                speak(3);
                System.out.println(KThread.currentThread().toString() + ": finish speaking 3");
            }
        }).setName("Speaker3");

        KThread listener0 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().toString() + ": try to listen");
                int myWord = listen();
                System.out.println(KThread.currentThread().toString() + ": get message " + Integer.toString(myWord));
            }
        }).setName("Listener0");

        KThread listener1 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().toString() + ": try to listen");
                int myWord = listen();
                System.out.println(KThread.currentThread().toString() + ": get message " + Integer.toString(myWord));
            }
        }).setName("Listener1");

        KThread listener2 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().toString() + ": try to listen");
                int myWord = listen();
                System.out.println(KThread.currentThread().toString() + ": get message " + Integer.toString(myWord));
            }
        }).setName("Listener2");

        KThread listener3 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().toString() + ": try to listen");
                int myWord = listen();
                System.out.println(KThread.currentThread().toString() + ": get message " + Integer.toString(myWord));
            }
        }).setName("Listener3");

    
        speaker0.fork();
        listener2.fork();
        listener1.fork();
        listener3.fork();
        speaker1.fork();
        listener0.fork();
        speaker2.fork();
        speaker3.fork();


        speaker0.join();
        speaker1.join();
        speaker2.join();
        speaker3.join();
        listener0.join();
        listener1.join();
        listener2.join();
        listener3.join();
    }
}
