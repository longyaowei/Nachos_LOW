package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

import java.util.*;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	    return new PriorityQueue(transferPriority);
    }

    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 1;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = Integer.MAX_VALUE;    

    protected class PriorityQueue extends PriorityScheduler.PriorityQueue {
        PriorityQueue(boolean transferPriority) {
            super(transferPriority);
        }


        protected ThreadState pickNextThread() {
            ThreadState nextThread = null; //the nextThread
            
            int target = 1 + Lib.random(getEffectivePriority()), cumsum = 0;
            for (int i = 0; i < waitQueue.size(); i++) { 
                ThreadState curThread = waitQueue.get(i);
                int realPriority = curThread.getEffectivePriority();
                cumsum += realPriority;
                if (cumsum >= target) {
                    nextThread = curThread;
                    break;
                }
            }

            return nextThread;
        }

        public int getEffectivePriority() {
            if (transferPriority == true && !validCache) {
                validCache = true;
                effectivePriority = 0;
                for (int i = 0;i < waitQueue.size(); i++) {
                    ThreadState curThread = waitQueue.get(i);
                    int realPriority = curThread.getEffectivePriority();
                    effectivePriority += realPriority;
                }
            }
            return effectivePriority;
        }

        int effectivePriority = 0;

        LinkedList<ThreadState> waitQueue = new LinkedList<ThreadState>();
        /** curThread: who is having this resource*/
        ThreadState curThread = null;
    }

    protected class ThreadState extends PriorityScheduler.ThreadState {
        public ThreadState(KThread thread) {
           super(thread);
        }

        public int getEffectivePriority() {
            if (validCache == false) {
                validCache = true;
                effectivePriority = priority;
                for(int i = 0;i < holdQueue.size();i ++) {
                    PriorityQueue curQueue = holdQueue.get(i); 
                    //since every one in the waiting queue would donate its priority, we only need to compare it with the queue
                    effectivePriority += curQueue.getEffectivePriority();
                }
            }
	    	return effectivePriority;
        }

        LinkedList<PriorityQueue> needQueue = new LinkedList<PriorityQueue>();
        /** holdQueue: queues of resources where this thread holds*/
        LinkedList<PriorityQueue> holdQueue = new LinkedList<PriorityQueue>();
    }
}
