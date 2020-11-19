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
public class LotteryScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public LotteryScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());
                
        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());
                
        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());
                
        Lib.assertTrue(priority >= priorityMinimum &&
            priority <= priorityMaximum);
        
        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();
                
        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority+1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();
                
        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority-1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 1;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = Integer.MAX_VALUE;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    public void selfTest() {
    	System.out.println("\n\nLotteryScheduler Test");

    	Machine.interrupt().disable();
    	//create three threads with minimum, maximum & default priority
    	//test whether a priority donation is achievable
    	
    	Runnable simpleRun = new Runnable(){
		public void run(){
            }
    	};

    	int low1 = 0;
    	int low2 = 0;

   		for (int i = 0; i < 1000; i++) {
   			KThread low = new KThread(simpleRun).setName("low");
	    	KThread medium = new KThread(simpleRun).setName("medium");
	    	KThread high = new KThread(simpleRun).setName("high");
	    	setPriority(low, priorityMinimum);
	    	setPriority(medium, priorityMinimum + 1);
	    	setPriority(high, priorityMinimum + 2);

	    	//Test if it is a priority queue, no transferPriority
	    	PriorityQueue queue1 = new PriorityQueue(false); 
	    	//another queue to test priority donation
	    	PriorityQueue queue3 = new PriorityQueue(false);
	    	queue1.waitForAccess(low);
	    	queue1.waitForAccess(medium);
	    	queue1.waitForAccess(high);

	    	queue3.waitForAccess(low);
	    	queue3.waitForAccess(medium);
	    	queue3.waitForAccess(high);
	    	//it should be high->medium->low
	    	if (queue1.nextThread().getName() == "low")
	    		low1 += 1;
	    	System.out.println("Simple Queue is dequing");
	    	System.out.println("\n\n" + queue1.nextThread().getName());
	    	System.out.println("\n\n" + queue1.nextThread().getName());

	    	//Test when there is a priority donation
	    	System.out.println("PriorityDonation is testing");
	    	PriorityQueue queue2 = new PriorityQueue(true);
	    	queue2.acquire(low);
	    	//now the priority of low should be low
	    	System.out.println("The effectivePriority of Low is now " + getEffectivePriority(low));
	    	queue2.waitForAccess(medium);
	    	//now the priority of low should be medium
	    	System.out.println("The effectivePriority of Low is now " + getEffectivePriority(low));
	    	System.out.println("The effectivePriority of Median is now " + getEffectivePriority(medium));
	    	//further insert high
	    	queue2.waitForAccess(high);
	    	//now the priority of low should be high
	    	System.out.println("The effectivePriority of Low is now " + getEffectivePriority(low));
	    	//no change for medium since it has no access
	    	System.out.println("The effectivePriority of Medium is now " + getEffectivePriority(medium));

	    	//Queue3 is dequeing, test the order: should be low-high-medium
	    	System.out.println("Queue 3 is dequing");
	    	if (queue3.nextThread().getName() == "low")
	    		low2 += 1;
	    	System.out.println("\n\n" + queue3.nextThread().getName());
	    	System.out.println("\n\n" + queue3.nextThread().getName());
   		}
   		System.out.println("\n\n" + low1 + ' ' + low2);
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }

        /**
        * a new thread waiting for the resource (e.g., a lock)
        *
        * @param	thread the thread who is waiting for the resource
        */
        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            ThreadState state = getThreadState(thread);
            state.waitForAccess(this);
            waitQueue.add(state);
        }

        /**
        * a new thread has acquired access to the resource
        *
        * @param	thread the thread who has acquired the access
        */
        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            //if some thread is holding the resource, release it
            if (curThread != null) 
                curThread.release(this);
            ThreadState state = getThreadState(thread);
            curThread = state;
            state.acquire(this);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            //Find the one with highest priority
            ThreadState nextThread = pickNextThread();
            if (nextThread == null)
                return null;
            //pop it from the queue
            waitQueue.remove(nextThread);
            //and acquire the resource
            acquire(nextThread.getThread());
            return nextThread.getThread();
        }

        /**
        * Return the next thread that <tt>nextThread()</tt> would return,
        * without modifying the state of this queue.
        *
        * @return	the next thread that <tt>nextThread()</tt> would
        *		return.
        */
        protected ThreadState pickNextThread() {
            ThreadState nextThread = null; //the nextThread
            
            // System.out.println("\n\n" + "Start getting total priority");
            int totalPriority = 0;
            for (int i = 0; i < waitQueue.size(); i++) { 
                ThreadState curThread = waitQueue.get(i);
                int realPriority = curThread.getEffectivePriority();
                totalPriority += realPriority;
            }
            // System.out.println("\n\n" + "Total priority " + totalPriority);

            if (totalPriority == 0) {
                return null;
            }

            int target = 1 + Lib.random(totalPriority), cumsum = 0;
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

        /**
        * Return the highest effective priority in the waiting queue
        *
        * @return 
        */

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

        public void invalidCache() {
            validCache = false;
            if (transferPriority == true && curThread != null)
                curThread.invalidCache(); 
        }
        
        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }

        /**
        * <tt>true</tt> if this queue should transfer priority from waiting
        * threads to the owning thread.
        */
        public boolean transferPriority;

        /**
        * highest effective priorities for threads waiting for the resource
        */
        protected int effectivePriority = 0; 
        /**
        *  whether the effectivePriority is correct
        */
        protected boolean validCache = false;

        /** A queue of waiting threads */
        protected LinkedList<ThreadState> waitQueue = new LinkedList<ThreadState>();
        /** curThread: who is having this resource*/
        protected ThreadState curThread = null; 
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
        /**
        * Allocate a new <tt>ThreadState</tt> object and associate it with the
        * specified thread.
        *
        * @param	thread	the thread this state belongs to.
        */
        public ThreadState(KThread thread) {
            this.thread = thread;
            setPriority(priorityDefault);
        }
        /**
        * return the thread this state belongs to
        *
        * @return	thread	the thread this state belongs to.
        */
        public KThread getThread() {
            return thread;
        }

        /**
        * Return the priority of the associated thread.
        *
        * @return	the priority of the associated thread.
        */
        public int getPriority() {
            return priority;
        }

        /**
        * Return the effective priority of the associated thread.
        *
        * @return	the effective priority of the associated thread.
        */
        public int getEffectivePriority() {
            /** priority donation
                a high priority waiting thread donates its priority to the thread holding the resource
            */
            if (validCache == false) {
                validCache = true;
                effectivePriority = priority;
                for(int i = 0;i < holdQueue.size();i ++) {
                    PriorityQueue curQueue = holdQueue.get(i); 
                    //since every one in the waiting queue would donate its priority, we only need to compare it with the queue
                    effectivePriority += curQueue.getEffectivePriority();
                }
            }
            System.out.println("thread " + thread + " " + effectivePriority + ' ' + priority);
	    	return effectivePriority;
        }

        /**
        * Set the priority of the associated thread to the specified value.
        *
        * @param	priority	the new priority.
        */
        public void setPriority(int priority) {
                if (this.priority == priority)
                return;
            
                this.priority = priority;
                this.effectivePriority = priority;
                //priority is changed
                invalidCache();
        }

        /**
        * Current EffectivePriority is incorrect
        * Therefore, that of all waiting queues are incorrect
        */
        public void invalidCache() {
            validCache = false;

                for (int i = 0;i < needQueue.size();i ++) 
                    (needQueue.get(i)).invalidCache();
        }

        /**
        * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
        * the associated thread) is invoked on the specified priority queue.
        * The associated thread is therefore waiting for access to the
        * resource guarded by <tt>waitQueue</tt>. This method is only called
        * if the associated thread cannot immediately obtain access.
        *
        * @param	waitQueue	the queue that the associated thread is
        *				now waiting on.
        *
        * @see	nachos.threads.ThreadQueue#waitForAccess
        */
        public void waitForAccess(PriorityQueue waitQueue) {
                // put into a waiting queue
            holdQueue.remove(waitQueue);
            needQueue.add(waitQueue);
            waitQueue.invalidCache();
        }

        /**
        * Called when the associated thread has acquired access to whatever is
        * guarded by <tt>waitQueue</tt>. This can occur either as a result of
        * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
        * <tt>thread</tt> is the associated thread), or as a result of
        * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
        *
        * @see	nachos.threads.ThreadQueue#acquire
        * @see	nachos.threads.ThreadQueue#nextThread
        */
        public void acquire(PriorityQueue waitQueue) {
            //own a resource now
            holdQueue.add(waitQueue);
            needQueue.remove(waitQueue);
            invalidCache();		
        }	

        /**
        * Called when the associated thread has released access to whatever is
        * guarded by <tt>waitQueue</tt>. 
        */
        public void release(PriorityQueue waitQueue) {
            holdQueue.remove(waitQueue);
            invalidCache();
        }
        /** whether the current effectivePriority is correct*/
        protected boolean validCache = false; 
        /** cached effectivePriority*/
        protected int effectivePriority = priorityMinimum;
        /** The thread with which this object is associated. */	   
        protected KThread thread;
        /** The priority of the associated thread. */
        protected int priority;
        /** needQueue: queues of resources where this thread needs*/
        protected LinkedList<PriorityQueue> needQueue = new LinkedList<PriorityQueue>();
        /** holdQueue: queues of resources where this thread holds*/
        protected LinkedList<PriorityQueue> holdQueue = new LinkedList<PriorityQueue>();
    }
}
