#include  <stdio.h>                                         
#include "fifo_funkcie.c"

char* out_fifo = "\\\\.\\pipe\\java_in";
char* in_fifo = "\\\\.\\pipe\\java_out";

main(){
	while(1 == 1){                                                           
		char  line[81], character;                              
		int   c;                                                
		c = 0;                                                  
		printf("$: ");          
		do{                                                       
			character = getchar();                              
		    line[c]   = character;                              
		    c++;                                                
		}                                                       
		while(character != '\n');                                
		
		c = c - 1;                                              
		line[c] = '\0';                                         
		//printf("\n%s\n", line);                                 
		//zapisat line do fifo
		posliSpravuDoFifo( line, out_fifo );
		char * odpoved = dajSpravuZFIFO(in_fifo);
		printf("Odpoved: %s\n",odpoved);
	}
}   
