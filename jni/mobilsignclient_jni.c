#include "mobilsignclient_JNI.h"
#include "fifo_funkcie.c"

char* out_fifo = "/tmp/fifo_java_out";
char* in_fifo = "/tmp/fifo_java_in";

//------------------BEGIN implementacia JNI------------------------------------------//
jstring JNICALL Java_mobilsignclient_JNI_dajSpravu(JNIEnv* env, jclass myClass){	
	char* odpoved = dajSpravuZFIFO(in_fifo);
	return (*env)->NewStringUTF(env, odpoved);	
}

void JNICALL Java_mobilsignclient_JNI_posliSpravu(JNIEnv* env, jclass myClass, jstring sprava){
	const char* tempSprava = (*env)->GetStringUTFChars(env, sprava, NULL);	
	posliSpravuDoFifo( tempSprava, out_fifo );
}

//-----------------end implementacia JNI---------------------------------------------//




