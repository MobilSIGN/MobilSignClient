/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jni;

/**
 *
 * @author jano
 */
public class JNICommunicator {
    
    private JNIResponder delegat; 
    private Thread prijimacieVlanko;
    
    public JNICommunicator(JNIResponder delegat){
        this.delegat = delegat;
    }
    
    public void posliSpravu(String sprava){
        JNI.posliSpravu(sprava);
    }
    
    public void startPrijimanieSprav() {
        if(this.prijimacieVlanko != null){
            System.out.println("Vlanko uz bezi");
            return;
        }        
        this.prijimacieVlanko = new Thread() {
            @Override
            public void run() {
               while(true){
                   String prijataSprava = JNI.dajSpravu();
                   delegat.spracujSpravu(prijataSprava);
               }
            }
        };
        this.prijimacieVlanko.start();

    }
    
}
