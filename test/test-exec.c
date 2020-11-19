#include "syscall.h"

int main() {
	int pid = exec("~/nachos/test/matmul.coff",0,0);
	int pid1 = exec("~/nachos/test/test-exit.coff",0,0);
	printf("ok\n");
	exit(0);
}
