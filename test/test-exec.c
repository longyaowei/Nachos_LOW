#include "syscall.h"

int main() {
	int pid = exec("matmult.coff",0,0);
	int pid1 = exec("test-exit.coff",0,0);
	printf("ok\n");
	exit(0);
}
