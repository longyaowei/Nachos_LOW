package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat {
	static BoatGrader bg;
	
	static Lock lock;
	static Condition cond_adult;
	static Condition cond_child, cond_child_pilot, cond_child_Molokai;

	static Communicator com;
	
	static int boat;
	static boolean at_least_one_child_Molokai;
	static boolean ride;

	static int n_adult_Oahu, n_child_Oahu, n_child_Molokai;
    
    public static void selfTest()  {
		BoatGrader b = new BoatGrader();
		
		// System.out.println("\n ***Testing Boats with only 2 children***");
		// begin(0, 2, b);

		// System.out.println("\n ***Testing Boats with 5 children, 3 adult***");
	  	// begin(5, 2, b);

	  	for (int i = 0; i < 7; ++i) {
			for (int j = 2; j < 7; ++j) {
				System.out.println("\n***" + i + " Adults " + j + " Children\n"); 
				begin(i, j, b);
			}
		}
		  
    }

    public static void begin( int adults, int children, BoatGrader b ) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here

		lock = new Lock();
		boat = 1;
		cond_adult = new Condition(lock);
		cond_child = new Condition(lock);
		at_least_one_child_Molokai = false;
		n_adult_Oahu = n_child_Oahu = n_child_Molokai = 0;
		ride = false;
		cond_child_pilot = new Condition(lock);
		cond_child_Molokai = new Condition(lock);
		com = new Communicator();
		
		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.

		Runnable r_adult = new Runnable() {
			public void run() {
				AdultItinerary();
			}
		};
		Runnable r_child = new Runnable() {
			public void run() {
				ChildItinerary();
			}
		};
		for (int i = 0; i < adults; ++i) {
			KThread t = new KThread(r_adult);
			t.setName(String.format("Adult Thread # %d", i));
			t.fork();
		}
		for (int i = 0; i < children; ++i) {
			KThread t = new KThread(r_child);
			t.setName(String.format("Child Thread # %d", i));
			t.fork();
		}

		ThreadedKernel.alarm.waitUntil(1000);

		int arrived = 0;
		while (arrived < adults + children) {
			int msg = com.listen();
			arrived += msg;
		}

		ThreadedKernel.alarm.waitUntil(1000);
    }

    static void AdultItinerary() {
		bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
		//DO NOT PUT ANYTHING ABOVE THIS LINE.

		/* This is where you should put your solutions. Make calls
		to the BoatGrader to show that it is synchronized. For
		example:
			bg.AdultRowToMolokai();
		indicates that an adult has rowed the boat across to Molokai
		*/

		lock.acquire();

		n_adult_Oahu++;
		while (boat != 1 || !at_least_one_child_Molokai) {	
			cond_adult.sleep();
		}
		bg.AdultRowToMolokai();
		com.speak(1);
		n_adult_Oahu--;
		boat = 2;
		cond_child_Molokai.wake();

		lock.release();
    }

    static void ChildItinerary() {
		bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
		//DO NOT PUT ANYTHING ABOVE THIS LINE. 

		lock.acquire();

		n_child_Oahu++;

		while (true) {
			while ((boat != 1 || !(n_adult_Oahu == 0 || at_least_one_child_Molokai == false) || n_child_Oahu < 2) && ride == false) {
				cond_child.sleep();
			}
			if (boat == 1 && (n_adult_Oahu == 0 || at_least_one_child_Molokai == false) && n_child_Oahu >= 2) {
				bg.ChildRowToMolokai();
				com.speak(1);	
				boat = 0;	
				n_child_Oahu--;	
				n_child_Molokai++;
				at_least_one_child_Molokai = true;
				ride = true;
				cond_child.wake();
				while (ride == true) {	
					cond_child_pilot.sleep();
				}
				boat = 2;
				cond_child_Molokai.wake();
			}
			else {
				bg.ChildRideToMolokai();
				com.speak(1);
				n_child_Oahu--;
				n_child_Molokai++;
				if (n_child_Oahu == 0 && n_adult_Oahu == 0) {// end request
					com.speak(0);
				}
				at_least_one_child_Molokai = true;
				ride = false;
				cond_child_pilot.wake();
			}
			while (boat != 2) {
				cond_child_Molokai.sleep();
			}
			bg.ChildRowToOahu();
			com.speak(-1);
			boat = 1;
			n_child_Molokai--;
			n_child_Oahu++;
			at_least_one_child_Molokai = n_child_Molokai >= 1;
			cond_child.wake();
			cond_adult.wake();
		}
    }

    static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
    }
    
}
