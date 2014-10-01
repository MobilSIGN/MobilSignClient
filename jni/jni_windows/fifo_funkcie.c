#include <windows.h>

#define MAX_BUF 1024

void posliSpravuDoFifo(const char* sprava, char* fifo_name){
  HANDLE pipe = CreateFile(fifo_name, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, 0, NULL); 
  if (pipe == INVALID_HANDLE_VALUE)
  {
      printf("ERROR");
  }             
  DWORD numWritten;
  WriteFile(pipe, sprava, MAX_BUF, &numWritten, NULL); 
}

char* dajSpravuZFIFO(char* fifo_name){    
  HANDLE pipe = CreateNamedPipe(fifo_name, PIPE_ACCESS_INBOUND | PIPE_ACCESS_OUTBOUND , PIPE_WAIT, 1, MAX_BUF, MAX_BUF, 120 * 1000, NULL);

    if (pipe == INVALID_HANDLE_VALUE)
    {
        printf("ERROR");
    }

    char data[MAX_BUF];
    DWORD numRead;

    ConnectNamedPipe(pipe, NULL);

    ReadFile(pipe, data, MAX_BUF, &numRead, NULL);
    char* navrat;
    if (numRead > 0){
      navrat = data;        
    }
    else{
      navrat = "NIC";
    }
    
    CloseHandle(pipe);
    return navrat;        
}  