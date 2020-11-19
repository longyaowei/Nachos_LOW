#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

// This test is designed to show that
// the opened desccriptor can still work
// after unlink is called.

// to generate a text file text.txt
// you can use ``make text''

int main(int argc, char** argv)
{
  int fd, amount;

  fd = creat("text.txt");
  if (fd==-1) {
    printf("Failed to open text.txt\n");
  }else{
  	  printf("open text.txt\n");  
  }

  int i = unlink("text.txt");
  if (i == -1) {
    printf("Unable to remove text.txt\n");
  }


  int fd2 = creat("text.txt");
  if (fd2 != -1) {
    printf("unlink pending and create successfully\n");
  }

  fd2 = open("text.txt");
  if (fd2 != -1) {
    printf("unlink pending and open successfully\n");
  }

  close(fd);
  fd2 = creat("text.txt");
  if (fd2 != -1) {
    printf("create successfully\n");
  } 
  return 0;
}
