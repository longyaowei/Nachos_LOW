#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

char buf[1024];
int fd, fd2, fdread, n, i, j;

int main()
{
  fdread = open("fileTest.in");
  printf( "fileTest.in fd = %d\n", fdread );
  i = read(fdread, buf, 1);
  printf( "number of bytes read = %d\n", i );
  n = buf[0]-'0';
  printf( "n = %d\n", n );
  fd = creat("fileTest.out");
  fd2 = creat( "unlink.out" );
  printf("fileTest.out fd = %d\n", fd);
  printf("unlink.out fd = %d\n", fd2);
  for (i=0; i<n; i++) {
    for (j=0; j<n; j++) {
        buf[i * (n + 1) + j] = (char) (j + '0');
      }
    buf[i * (n + 1) + n] = '\n';
  }
  buf[n * (n + 1)] = 'I';
  buf[n * (n + 1) + 1] = ' ';
  buf[n * (n + 1) + 2] = 'L';
  buf[n * (n + 1) + 3] = 'O';
  buf[n * (n + 1) + 4] = 'V';
  buf[n * (n + 1) + 5] = 'E';
  buf[n * (n + 1) + 6] = ' ';
  buf[n * (n + 1) + 7] = 'U';
  buf[n * (n + 1) + 8] = '\n';
  i = write(fd, buf, n * (n + 1) + 9);
  printf("number of bytes written = %d\n", i);
  int r = close(fd);
  printf("r = %d\n", r);
  r = close(-1);
  printf("r = %d\n", r);
  r = unlink("unlink.out");
  printf( "unlink unlink.out flag : %d\n", r );
  r = unlink("nonexist.out");
  printf( "unlink nonexist.out flag : %d\n", r );
  return 0;
}
