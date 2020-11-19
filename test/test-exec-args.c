#include "syscall.h"

int main() {
	int pid = exec("matmult.coff",0,0);
	int pid1 = exec("test-exit.coff",0,0);
	static char* s[2] = {"tsinghua is", "very cool"};
	char* go[2];
	go[0] = s[0], go[1] = s[1];
	int pid2 = exec("echo.coff", 2, go);
	printf("ok\n");
	exit(0);
}
