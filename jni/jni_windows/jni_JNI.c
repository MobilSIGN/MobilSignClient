#include "jni_JNI.h"
#include "fifo_funkcie.c"

char* out_fifo = "\\\\.\\pipe\\java_out";
char* in_fifo = "\\\\.\\pipe\\java_in";

//------------------BEGIN implementacia JNI------------------------------------------//
jstring JNICALL Java_jni_JNI_dajSpravu(JNIEnv* env, jclass myClass){	
	char* odpoved = dajSpravuZFIFO(in_fifo);
	return (*env)->NewStringUTF(env, odpoved);	
}

void JNICALL Java_jni_JNI_posliSpravu(JNIEnv* env, jclass myClass, jstring sprava){
	const char* tempSprava = (*env)->GetStringUTFChars(env, sprava, NULL);	
	posliSpravuDoFifo( tempSprava, out_fifo );
}

//-----------------end implementacia JNI---------------------------------------------//




