package nachos.threads;

import nachos.machine.*;

public class Fridge {
	private int apples;
	private Lock lock;
	private Condition2 condition2;
	private static Fridge fridge = null;
	public Fridge(int apples){
		this.apples = apples;
		lock = new Lock();
		condition2 = new Condition2(lock);
	}
	public boolean hasApple(){
		return (apples > 0);
	}
	public void acquire(){
		lock.acquire();
		while (!hasApple()){
			System.out.println(KThread.currentThread().toString() + ": no apples and sleep");
			condition2.sleep();
			System.out.println(KThread.currentThread().toString() + ": wake up");
		}
		apples --;
		System.out.println(KThread.currentThread().toString() + ": get an apple (" + Integer.toString(apples) + " left) and leave");
		lock.release();
	}
	public void store(){
		lock.acquire();
		apples ++;
		System.out.println(KThread.currentThread().toString() + ": put an apple (" + Integer.toString(apples) + " left) and leave");
		condition2.wake();
		lock.release();
	}

	public static void test(){
		System.out.println("\n\nCondition Variable Test");
    	
    	fridge = new Fridge(0);

    	KThread acquireThread0 = new KThread(new Runnable(){
    		public void run(){
    			fridge.acquire();
    		}
    	}).setName("Condition2Test AcquireThread0");

		KThread acquireThread1 = new KThread(new Runnable(){
    		public void run(){
    			fridge.acquire();
    		}
    	}).setName("Condition2Test AcquireThread1");

		KThread storeThread0 = new KThread(new Runnable(){
    		public void run(){
    			fridge.store();
    		}
    	}).setName("Condition2Test StoreThread0");

		KThread storeThread1 = new KThread(new Runnable(){
    		public void run(){
    			fridge.store();
    		}
    	}).setName("Condition2Test StoreThread1");
		acquireThread0.fork();
		acquireThread1.fork();
		storeThread0.fork();
		storeThread1.fork();
		acquireThread0.join();
		acquireThread1.join();
		storeThread0.join();
		storeThread1.join();
	}
}
