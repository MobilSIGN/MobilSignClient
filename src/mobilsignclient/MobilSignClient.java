/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobilsignclient;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JTextArea;

/**
 *
 * @author Marek Spalek <marekspalek@gmail.com>
 */
public class MobilSignClient {

    private String serverAddress; // ip adresa servera
    private int serverPort; // port na ktorom server pocuva
    private PrivateKey applicationKey; // kluc desktopovej aplikacie
    private RSAPublicKey mobileKey; // kluc aplikacie v mobile
    //private Socket socket; // sokect na pripojenie na server
    private SSLSocket socket; //ssl
    private JTextArea console; // consola z GUI, zaznamenava cinnost

    public MobilSignClient(String serverAddress, int serverPort, JTextArea console) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.console = console;
        generateKeys();
    }
    
    /**
     * Vrati kluc aplikacie
     * @return kluc aplikacie
     */
    public PrivateKey getApplicationKey() {
        return applicationKey;
    }
    
    /**
     * Vrati kluc mobilu
     * @return kluc mobilu
     */
    public RSAPublicKey getMobileKey() {
        return mobileKey;
    }
    
    /**
     * Pripoji sa na server
     */
    public void connectToServerNoSSL() {
     /*   try {
            socket = new Socket(serverAddress, serverPort); // pripojenie
            console.append("Connected to server " + serverAddress + ":" + serverPort + "\n"); // zaznam do konzoly
        } catch (IOException ioe) {
            System.err.println("Can not establish connection to " + serverAddress + ":" + serverPort + "\n" + ioe.getMessage());
            ioe.printStackTrace(System.out);
            System.exit(-1);
        }
        
        receiveMsg(); // spusti sa prijimanie sprav, pokial boli vyslane*/
    }
    
    /**
     * Pripoji sa na server
     */
    public void connectToServer() {
        try {
            
            SSLSocketFactory factory=(SSLSocketFactory) SSLSocketFactory.getDefault();
            socket=(SSLSocket) factory.createSocket(serverAddress,serverPort);
            console.append("Connected to server " + serverAddress + ":" + serverPort + "\n"); // zaznam do konzoly
        } catch (IOException ioe) {
            System.err.println("Can not establish connection to " + serverAddress + ":" + serverPort + "\n" + ioe.getMessage());
            ioe.printStackTrace(System.out);
            System.exit(-1);
        }
        
        receiveMsg(); // spusti sa prijimanie sprav, pokial boli vyslane
    }
    

    /**
     * Spusti vlakno odosielajuce spravy na server
     * @param str - odosielana sprava
     */
    public void sendMessageToServer(String str) {
        final String str1 = str;

        new Thread(new Runnable() {
            public void run() {
                PrintWriter out;
                try {
                    out = new PrintWriter(socket.getOutputStream());
                    out.println(str1);
                    out.flush();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Spusti vlakno prijimajuce spravy zo servera
     */
    public void receiveMsg() {

        new Thread(new Runnable() {
            public void run() {
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while (true) {
                    String msg = null;
                    try {
                        msg = in.readLine(); 
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (msg == null) {
                        break;
                    } else {
                        displayMsg(msg);
                    }
                }
            }
        }).start();

    }

    /**
     * Zobrazi spravu upravenu na spravny format
     * @param msg - neupravena sprava
     */
    public void displayMsg(String msg) {
        if (msg.length() > 5 && msg.substring(0, 5).equals("SEND:")) {
            msg = "Message recieved: [" + msg.substring(5) + "]";
        } else if (msg.length() > 5 && msg.substring(0, 5).equals("RESP:")) {
            if (msg.substring(5).equals("paired")) {
                msg = "Response: [" + msg.substring(5) + "]";
            }
        } else {
            msg = "Unknown message: [" + msg + "]";
        }
        console.append(msg+"\n");        
    }

    /**
     * Vrati obrazok QR kodu z bigintegra
     * @param data - data, z ktorych sa ma vyrobit QR kod
     * @return obrazok QR kodu
     */
    public BufferedImage getQrCode(BigInteger data) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            HashMap<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>(2); // pravidla QR kodu
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8"); // kodovanie QR kodu
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L); // mnozstvo informacie pre opravu chyby
            //hints.put(EncodeHintType.PDF417_COMPACT, true);
            //hints.put(EncodeHintType.PDF417_COMPACTION, Compaction.NUMERIC);

            BitMatrix bitMatrix = writer.encode(data + "", BarcodeFormat.QR_CODE, 750, 750, hints); // vytvori QR kod ako maticu bitov z bigintegera
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix); // prevedie QR kod matice bitov do obrazku
            return qrImage;

            // !!!!VYROBI Z MODULUSU KLUC!!!! TOTO TREBA DAT DO MOBILU, Z TADETO SA TO ZMAZE!!!!
            //            BigInteger exponenet = new BigInteger(65537 + ""); // vyrobi BigInteger z exponenta 65537
            //            RSAPublicKeySpec spec = new RSAPublicKeySpec(pb.getModulus(), exponenet); // vyrobi verejny kluc z exponenta a modulusu
            //            KeyFactory factory = KeyFactory.getInstance("RSA");
            //            PublicKey pub = factory.generatePublic(spec); // vrati verejny kluc vyrobeny pomocou RSAPublicKeySpec



        } catch (WriterException ex) {
            Logger.getLogger(MobilSignClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    /**
     * Vygeneruje RSA 2048 bitovi klucovi par
     */
    private void generateKeys() {
        try {
            KeyPairGenerator generatorRSA = KeyPairGenerator.getInstance("RSA"); // vytvori instanciu generatora RSA klucov
            generatorRSA.initialize(2048, new SecureRandom()); // inicializuje generator 2048 bitovych RSA klucov
            KeyPair keyRSA = generatorRSA.generateKeyPair(); // vygeneruje klucovi par
            this.applicationKey = keyRSA.getPrivate(); // kluc desktopovej aplikacie je sukromny kluc z klucoveho paru
            this.mobileKey = (RSAPublicKey) keyRSA.getPublic(); // vrati verejny kluc type RSAPublicKey, lebo z neho mozem dostat modulus 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void dispatchMessage(String aMessage) {
//        if (aMessage.length() > 5 && aMessage.substring(0, 5).equals("SEND:")) {
//            System.out.println("Message recieved: [" + aMessage.substring(5) + "]");
//            return;
//        }
//
//        if (aMessage.length() > 5 && aMessage.substring(0, 5).equals("RESP:")) {
//            if (aMessage.substring(5).equals("paired")) {
//                System.out.println("Response: [" + aMessage.substring(5) + "]");
//                return;
//            }
//        }
//        System.out.println("Unknown message: [" + aMessage + "]");
//    }
}
