#include "syscall.h"

int main() {
	printf("testing system call join!\n");
	int pid = exec("matmult.coff",0,0);
	int status, value;
	value = join(pid, &status);
	printf("join 1 %d %d %d\n", pid, value, status);
	int pid1 = exec("test-exit.coff",0,0);
	value = join(pid1, &status);
	printf("join 2 %d %d %d\n", pid1, value, status);
	//what if multiple join to the same child process?
	value = join(pid1, &status);
	printf("join 3 %d %d %d\n", pid1, value, status);
	//memory overflow
	int pid3 = exec("test-exec.coff",0,0);
	value = join(pid3, &status);
	printf("join 5 %d %d %d\n", pid3, value, status);
	int pid2 = exec("test-thisdoesnotexist.coff",0,0); // join something that does not exist at all
	value = join(pid2, &status);
	printf("join 4 %d %d %d\n", pid2, value, status);
	printf("ok\n");
	exit(0);
}

