/*
	Pomocne funkcie na citanie a zapis do FIFO
*/

#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <stdio.h>

#define MAX_BUF 1024

void posliSpravuDoFifo(const char* sprava, char* fifo_name){
	int fd;	

    /* create the FIFO (named pipe) */
    mkfifo(fifo_name, 0666);

    /* write "Hi" to the FIFO */
    fd = open(fifo_name, O_WRONLY);
    write(fd, sprava, MAX_BUF);
    close(fd);

    /* remove the FIFO */
    //unlink(myfifo);
}

char* dajSpravuZFIFO(char* fifo_name){
	int fd;
    char buf[MAX_BUF];

    /* open, read, and display the message from the FIFO */
    fd = open(fifo_name, O_RDONLY);
	while(1 == 1){
		int resp = read(fd, buf, MAX_BUF);
		if(resp == -1){ //error
			usleep(10000);		
		}
		else if(resp == 0){ //eof
			usleep(10000);		
		}
		else if(resp > 0){ //ok
		 break;		
		}
	}
    close(fd);
	char* odpoved = buf;
	
    return odpoved;
}
