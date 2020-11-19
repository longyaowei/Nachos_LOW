package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;

import java.util.*;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
     
    public UserProcess() {
    	this.fileTable = new OpenFile[16]; //up to 16 open files
        //stdin and stdout
        this.fileTable[0] = UserKernel.console.openForReading();
        this.fileTable[1] = UserKernel.console.openForWriting();
        pid = ++ pidNum;
        cntLock.acquire();
        ++ numOfProcess;
        cntLock.release();
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args))
            return false;
        
        thread = new UThread(this);
        thread.setName(name).fork();

        unloadSections();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	    Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength+1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length=0; length<bytesRead; length++) {
            if (bytes[length] == 0)
            return new String(bytes, 0, length);
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	    return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

        byte[] memory = Machine.processor().getMemory();
        
        Lib.debug(dbgProcess, "vaddr " + vaddr + " len " + length);
        Lib.debug(dbgProcess, "numPages " + numPages + " pageSize " + pageSize);

        if (vaddr < 0 || vaddr >= numPages * pageSize)
            return 0;

        int maxAmount = Math.min(length, numPages * pageSize - vaddr);
        int amount = 0;

        for (int i = 0; i < maxAmount;) {
            int vpn = (vaddr + i) / pageSize;
            int ppn = pageTable[vpn].ppn;

            Lib.debug(dbgProcess, "vpn " + vpn + " ppn " + ppn);

            int pageOffset0 = (vaddr + i) - vpn * pageSize;
            int pageOffset1 = Math.min(vaddr + maxAmount, (vpn + 1) * pageSize) - vpn * pageSize;

            Lib.debug(dbgProcess, "offsets " + pageOffset0 + ' ' + pageOffset1);

            if (pageTable[vpn].valid) {
                System.arraycopy(memory, ppn * pageSize + pageOffset0, data, offset + i, pageOffset1 - pageOffset0);
                amount += pageOffset1 - pageOffset0;
            }
            else {
                break;
            }
            

            i = (vpn + 1) * pageSize;
        }

        return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	    return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

        byte[] memory = Machine.processor().getMemory();
        
        if (vaddr < 0 || vaddr >= numPages * pageSize)
            return 0;

        int maxAmount = Math.min(length, numPages * pageSize - vaddr);
        int amount = 0;

        for (int i = 0; i < maxAmount;) {
            int vpn = (vaddr + i) / pageSize;
            int ppn = pageTable[vpn].ppn;

            int pageOffset0 = (vaddr + i) - vpn * pageSize;
            int pageOffset1 = Math.min(vaddr + maxAmount, (vpn + 1) * pageSize) - vpn * pageSize;

            if (!pageTable[vpn].readOnly && pageTable[vpn].valid) {
                System.arraycopy(data, offset + i, memory, ppn * pageSize + pageOffset0, pageOffset1 - pageOffset0);
                amount += pageOffset1 - pageOffset0;
            }
            else {
                break;
            }

            i = (vpn + 1) * pageSize;
        }


        return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
        
        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        }
        catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i=0; i<args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();	

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages*pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages-1)*pageSize;
        int stringOffset = entryOffset + args.length*4;

        this.argc = args.length;
        this.argv = entryOffset;
        
        for (int i=0; i<argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
                argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {

        UserKernel.freePhysPagesLock.acquire();

        if (numPages > UserKernel.freePhysPages.size()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            UserKernel.freePhysPagesLock.release();
            return false;
        }

        pageTable = new TranslationEntry[numPages];
        for (int i = 0; i < numPages; ++i) {
            int ppn = UserKernel.freePhysPages.remove(0);
            pageTable[i] = new TranslationEntry(i, ppn, true, false, false, false);
        }

        UserKernel.freePhysPagesLock.release();        

        // load sections
        for (int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            
            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                + " section (" + section.getLength() + " pages)");

            for (int i=0; i<section.getLength(); i++) {
                int vpn = section.getFirstVPN()+i;

                section.loadPage(i, pageTable[vpn].ppn); 
                pageTable[vpn].readOnly = section.isReadOnly();
            }
	    }
	
	    return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        
        UserKernel.freePhysPagesLock.acquire();

        for (int i = 0; i < numPages; i++) {
            UserKernel.freePhysPages.add(pageTable[i].ppn);
        }

        UserKernel.freePhysPagesLock.release();

        coff.close();
    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
        Lib.debug(dbgProcess, "going to halt");
        
	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }

	private int handleCreat(int vaddr){
        String name = readVirtualMemoryString(vaddr, 256);
        Lib.debug(dbgProcess, "file name " + name);
        if (name == null || name.length() == 0) return -1; //invalid name
        int fileDescriptor = -1;
        for (int i=0; i<this.fileTable.length; i++)
        if (this.fileTable[i] == null){
            fileDescriptor = i;
            break;
        }
        if (fileDescriptor == -1) return -1; //no position
        OpenFile createdFile = ThreadedKernel.fileSystem.open(name, true);
        if (createdFile == null) return -1; //failed
        this.fileTable[fileDescriptor] = createdFile;
        return fileDescriptor;
    }

    private int handleOpen(int vaddr){
        String name = readVirtualMemoryString(vaddr, 256);
        if (name == null || name.length() == 0) return -1; //invalid name
        int fileDescriptor = -1;
        for (int i=0; i<this.fileTable.length; i++)
        if (this.fileTable[i] == null){
            fileDescriptor = i;
            break;
        }
        if (fileDescriptor == -1) return -1; //no position
        OpenFile openFile = ThreadedKernel.fileSystem.open(name, false);
        if (openFile == null) return -1; //failed
        this.fileTable[fileDescriptor] = openFile;
        return fileDescriptor;
    }

    private int handleRead(int fileDescriptor, int buffer, int count){
        if (count < 0 || fileDescriptor < 0 || fileDescriptor >= this.fileTable.length) return -1;
        OpenFile readFile = this.fileTable[fileDescriptor];
        if (readFile == null) return -1;
        byte[] content = new byte[count];
        int readLength = readFile.read(content, 0, count);
        if (readLength == -1) return -1;
        int writeLength = writeVirtualMemory(buffer, content, 0, readLength);
        if (readLength != writeLength) return -1;
        return readLength;
    }

    private int handleWrite(int fileDescriptor, int buffer, int count){
        if (count < 0 || fileDescriptor < 0 || fileDescriptor >= this.fileTable.length) return -1;
        OpenFile writeFile = this.fileTable[fileDescriptor];
        if (writeFile == null) return -1;
        byte[] content = new byte[count];
        int readLength = readVirtualMemory(buffer, content, 0, count);
        if (readLength < count) return -1;
        int writeLength = writeFile.write(content, 0, count);
        if (writeLength < count) return -1;
        return count;
    }

    private int handleClose(int fileDescriptor){
        if (fileDescriptor < 0 || fileDescriptor >= this.fileTable.length) return -1;
        OpenFile closeFile = fileTable[fileDescriptor];
        if (closeFile == null) return -1;
        fileTable[fileDescriptor] = null;
        closeFile.close();
        return 0;
    }

    private int handleUnlink(int vaddr){
        String name = readVirtualMemoryString(vaddr, 256);
        if (name == null || name.length() == 0) return -1; //invalid name
        if (ThreadedKernel.fileSystem.remove(name)) return 0;
        return -1;
    }
    
    private int handleExec(int file, int argc, int argv) {
    	//name of file
        Lib.debug(dbgProcess, "syscall Exec()");
    	if (file < 0 || file > numPages * pageSize)
    		return -1;
    		
        Lib.debug(dbgProcess, "using file  at address" + file);
    	String name = readVirtualMemoryString(file, 256);
  
        Lib.debug(dbgProcess, "using file " + name);
    	if (name == null || !name.endsWith(".coff") || argv < 0 || argv > numPages * pageSize || argc < 0)
    		return -1;
    		
    	String arg[] = new String[argc];
    	for (int i = 0;i < argc;i ++) {
    		//load data
    		byte value[] = new byte[4];
    		int num = readVirtualMemory(argv + i * 4, value);
    		if (num != 4) return -1;
    		arg[i] = readVirtualMemoryString(Lib.bytesToInt(value, 0), 256);
    		//invalid arg
    		if (arg[i] == null) return -1;
    		Lib.debug(dbgProcess, i + "-th arg is" + arg[i]);
    	}
    	UserProcess childProcess = UserProcess.newUserProcess();
    	childList.add(childProcess);
    	childProcess.parentProcess = this;
    	Lib.debug(dbgProcess, "parent process is " + pid + "child process is " + childProcess.pid);
    	if (!childProcess.execute(name, arg)) {//fail to open the file 
    		Lib.debug(dbgProcess, "exec() incorrect file");
    		childProcess.exit();
    		return -1;
    	}
    	return childProcess.pid;
    }
    
    private int handleJoin(int pid, int status) {
		//first the pid must belong to a child
		int pos = -1;
    	for (int i = 0;i < childList.size();i ++)
    		if (childList.get(i).pid == pid) {
    			pos = i;
    			break;
    		}
    	if (pos == -1)
    		return -1;
    	UserProcess childProcess = childList.get(pos);
    	childProcess.thread.join();
    	if (!childProcess.goodExit)
    		return 0;
    	byte stateChild[] = new byte[4];
    	stateChild = Lib.bytesFromInt(childProcess.status);
    	//write back into memory, is it successful?
    	int num = writeVirtualMemory(status, stateChild);
    	if (num != 4) //error
    		return 0;
    	return 1;
    }
    
    /*
    close a process, and the status is returned to the parent process.
    */
    
    private void exit() {
        //first close all opened files
    	for(int i = 0;i < 16;i ++) {
    		OpenFile closeFile = fileTable[i];
    		if (fileTable[i] != null) {
    			fileTable[i] = null;
    			closeFile.close();
    		}
    	}
    	//clear the childlist
    	childList.clear();
    	childList = null;
        Lib.debug(dbgProcess, "exit with status " + status);
    	//if only the "main" thread is running, close the whole machine
    	if (numOfProcess == 1) {
        	Lib.debug(dbgProcess, "exit and the machine terminates");
    		Machine.halt();
    	}
    	//remove this thread
    	//we need a lock to do that
    	cntLock.acquire();
    	numOfProcess --;
    	cntLock.release();
    	//remove from the parent child list
    	if (parentProcess != null) {
    		parentProcess.childList.remove(this);
    	}
    }
    
    private int handleExit(int status) {
        Lib.debug(dbgProcess, "syscall exit()");
    	//save status
    	this.status = status;
    	this.goodExit = true;
    	//do an exit related to parents & # of live processes
    	exit();
    	//free all pages
    	unloadSections();
    	UThread.finish();
    	return 0;
    }

    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
   
	Lib.debug(dbgProcess, "enter syscall " + syscall);
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallCreate:
      	return handleCreat(a0);
    case syscallOpen:
        return handleOpen(a0);
    case syscallRead:
        return handleRead(a0,a1,a2);
    case syscallWrite:
        return handleWrite(a0,a1,a2);
    case syscallClose:
        return handleClose(a0);
    case syscallUnlink:
        return handleUnlink(a0);
   	case syscallExec:
	    Lib.debug(dbgProcess, "enter syscall exec()");
   		return handleExec(a0,a1,a2);
   	case syscallJoin:
		return handleJoin(a0,a1);
	case syscallExit:
	    Lib.debug(dbgProcess, "enter syscall exit()");
		return handleExit(a0);
   
	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    protected OpenFile[] fileTable;
    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    /** status of this thread*/
    private int status;
    //my parent process
    UserProcess parentProcess = null;
    
    //the process id of this thread
    public int pid;
    //what is this thread?
    UThread thread;
    //list of child processes
    private LinkedList<UserProcess> childList = new LinkedList<UserProcess>();
    //is the thread exiting normally?
    private boolean goodExit;
    
    protected static int pidNum = 0;
    /** number of active process */
    protected static int numOfProcess = 0;
    
    //lock for #of process
    protected static Lock cntLock = new Lock();
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
}
