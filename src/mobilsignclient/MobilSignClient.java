/**
 * TODO - po nadviazani spojenia (ako doteraz) cakat spravu s novym verejnym
 * klucom noveho klucoveho paru V2 P2 - (pridat nejaky status, aby sme vedeli ci
 * klient je sparovany, ci ma nove kluce, ci je dokoncena autentifikacia) -
 * pridat podmienky na ukoncenie spojenie v pripade utoku - pridat spravy do
 * protokolu (nieco na vymenu klucov)
 */
package mobilsignclient;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import communicator.Crypto;
import communicator.Listener;
import communicator.Sender;
import communicator.Util;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import jni.JNI;
import jni.JNICommunicator;
import jni.JNIResponder;
import org.apache.commons.codec.binary.Base64;

public class MobilSignClient implements JNIResponder {

    private String serverAddress; // ip adresa servera
    private int serverPort; // port na ktorom server pocuva
    private PrivateKey applicationKey; // kluc desktopovej aplikacie
    private RSAPublicKey mobileKey; // kluc aplikacie v mobile
    private JTextArea console; // consola z GUI, zaznamenava cinnost
    private Sender clientSender;
    private Listener clientListener;
    private Socket socket;
    private Crypto crypto;
    private boolean mKeysChanged;
    private JNICommunicator c_communicator;

    public MobilSignClient(String serverAddress, int serverPort, JTextArea console) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.console = console;
        generateKeys();
        crypto = new Crypto(applicationKey);
        mKeysChanged = false;


        Util.PCOperacnySystem os = Util.getOS();
        Util.PCArchitektura arch = Util.getArch();
        //je potrebne nacitat kniznicu v zavislosti od OS
        if (os == Util.PCOperacnySystem.LINUX) {
            if (arch == Util.PCArchitektura.BIT64) {
                System.load("/home/jano/NetBeansProjects/JNI_C/dist/Debug/GNU-Linux-x86/libJNI_C.so");
//                System.load("/home/peter/sources/projektSign/MobilSignClient/jni/libjni64.so");
            } else {
                System.load("/home/jano/NetBeansProjects/JNI_C/dist/Debug/GNU-Linux-x86/libJNI_C.so");
//                System.load("/home/peter/sources/projektSign/MobilSignClient/jni/libjni32.so");
            }
        } else if (Util.getOS() == Util.PCOperacnySystem.WINDOWS) {
            if (arch == Util.PCArchitektura.BIT64) {
                System.load("C:/Users/Jano/Documents/NetBeansProjects/MobilSignClient/jni/jni_windows/libjni64.dll");
            } else {
                System.load("C:/Users/Jano/Documents/NetBeansProjects/MobilSignClient/jni/jni_windows/libjni32.dll");
            }
        } else {
            System.err.println("Nepodporovana platforma");
            System.exit(0);
        }



        //zacneme komunikovat s pkcs
        new Thread(){
            @Override
            public void run(){
                initKomunikaciaSPKCS();
            }        
        }.start();

    }

    private void initKomunikaciaSPKCS() {
        //inicializujeme JNI kniznicu
        JNI.init();

        String spravaPrePKCS = "INIT Z JAVY";
        while (true) {
            String spravaZPKCS = JNI.process(spravaPrePKCS);
            System.out.println("SPRAVA Z PKCS: " + spravaZPKCS);
            spravaPrePKCS = this.spracujSpravuZPKCS(spravaZPKCS);
        }

        //spravaZPKCS = JNI.process("ODPOVED Z JAVY");    
    }

    private String spracujSpravuZPKCS(String spravaZPKCS) {
        this.sendMessage(spravaZPKCS);
        
        //todo komunikacia s androidom - synchronna
        return "TODO ODPOVED Z JAVY";
    }

    public Crypto getCrypto() {
        return crypto;
    }

    /**
     * Vrati kluc aplikacie
     *
     * @return kluc aplikacie
     */
    public PrivateKey getApplicationKey() {
        return applicationKey;
    }

    /**
     * Vrati kluc mobilu
     *
     * @return kluc mobilu
     */
    public RSAPublicKey getMobileKey() {
        return mobileKey;
    }

    /**
     * Pripoji sa na server
     */
    public void connectToServer() {
        try {
            socket = new Socket(serverAddress, serverPort);

            //vytvorenie a spustenie listenera a sendera
            this.clientSender = new Sender(socket);
            this.clientListener = new Listener(socket);
            this.clientListener.start();
            this.clientSender.start();


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
     *
     * @param str - odosielana sprava
     */
    private void sendMessageToServer(String str) {
        final String str1 = str;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (clientSender == null) {
                    System.out.println("Sprava je null");
                }
                clientSender.putMesssageToQueue(str1);
            }
        }).start();
    }

    /**
     * Odosle zasifrovanu spravu.
     */
    public void sendMessage(String message) {

        this.sendMessageToServer("SEND:" + crypto.encrypt(message));
    }

    /**
     * Odosle parovaci request na server.
     */
    public void pairRequest() {
        try {
            BigInteger modulus = this.getMobileKey().getModulus();

            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(modulus.toByteArray());
            String sha1 = new BigInteger(1, md.digest()).toString(16);

            System.out.println("PAIR:" + sha1);

            this.connectToServer();
            this.sendMessageToServer("PAIR:" + sha1);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(MobilSignClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Spusti vlakno prijimajuce spravy zo servera TODO
     */
    public synchronized void receiveMsg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (clientListener.hasMessage()) {
                        processMsg(clientListener.getMessage());
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MobilSignClient.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();

    }

    /**
     * Spracuje spravu a rozhodne co dalej
     *
     * @param msg
     */
    public boolean processMsg(String msg) {
        System.out.println("cela MSG: " + msg);
        if (msg.length() < 5) {
            System.err.println("Zly format spravy.");
            return false;
        }
        /*byte[] data = Base64.decodeBase64(msg.substring(5));
         byte[] decrypted = Crypto.decrypt(data, applicationKey);
         msg = new String(decrypted).trim();*/
        String typSpravy = msg.substring(0, 5);
        String teloSpravy = "";
        switch (typSpravy) {
            case Util.TYPE_SEND:
                teloSpravy = crypto.decrypt(msg.substring(5)).trim();
                msg = "Message recieved: [" + teloSpravy + "]";
                break;
            case Util.TYPE_PAIR:
                break;
            case Util.TYPE_RESP:
                teloSpravy = msg.substring(5);
                switch (teloSpravy) {
                    case "paired":
                        msg = "Response: [" + teloSpravy + "]";
                        break;
                    case "unpaired":
                        msg = "Response: [" + teloSpravy + "]";
                        break;
                    default:
                        msg = "Unknown message: [" + msg + "]";
                        break;
                }
                break;
            case Util.TYPE_MPUB:
                if (mKeysChanged == true) {
                    System.err.println("Druhy mobil sa chce pripojit. Rusime.");
                    return false;
                }
                teloSpravy = crypto.decrypt(msg.substring(5)).trim();
                System.out.println("Dekryptovany MPUB modulus: " + teloSpravy);
                // Teraz zasifrujeme verejny kluc mobila sukromnym klucom PC 
                String modulusVerejnehoKlucaMobiluZasifrovanyPrvyKrat = crypto.encrypt(teloSpravy);
                RSAPublicKey verejnyKlucMobilu = getKeyFromModulus(new BigInteger(teloSpravy));
                // Teraz ho zasifrujeme verejnym klucom mobilu    
                crypto.setKey(verejnyKlucMobilu);
                String modulusVerejnehoKlucaMobiluZasifrovanyDruhyKrat = crypto.encrypt(modulusVerejnehoKlucaMobiluZasifrovanyPrvyKrat);
                // Posleme ho do mobilu
                sendMessageToServer(Util.TYPE_MPUB + modulusVerejnehoKlucaMobiluZasifrovanyDruhyKrat);
                // ulozit niekde => mal by uz byt nastaveny v Crypte
                msg = "MPUB request: " + msg;
                mKeysChanged = true;
                break;
            default:
                System.err.println("Zly format spravy 2.");
                return false;
        }
        console.append(msg + "\n");
        return true;
    }

    private RSAPublicKey getKeyFromModulus(BigInteger pModulus) {
        RSAPublicKey key = null;
        try {
            //BigInteger modulus = new BigInteger(pModulus + "", 16); // vyrobi BigInteger z exponenta 65537 
            RSAPublicKeySpec spec = new RSAPublicKeySpec(pModulus, new BigInteger("65537"));
            KeyFactory factory = KeyFactory.getInstance("RSA");
            key = (RSAPublicKey) factory.generatePublic(spec);
        } catch (Exception e) {
            System.err.println("Chyba v metode getKeyFromModulus.");
        }
        return key;
    }

    /**
     * Vrati obrazok QR kodu z bigintegra
     *
     * @param data - data, z ktorych sa ma vyrobit QR kod
     * @return obrazok QR kodu
     */
    public BufferedImage getQrCode(BigInteger data) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            HashMap<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>(2); // pravidla QR kodu
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8"); // kodovanie QR kodu
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L); // mnozstvo informacie pre opravu chyby            

            String base64 = Base64.encodeBase64String(data.toByteArray());

            BitMatrix bitMatrix = writer.encode(base64, BarcodeFormat.QR_CODE, 750, 750, hints); // vytvori QR kod ako maticu bitov z bigintegera
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix); // prevedie QR kod matice bitov do obrazku
            return qrImage;
        } catch (WriterException ex) {
            System.err.println("Nastala chyba pri generovani QR kodu");
            return null;
        }
    }

    /**
     * Vygeneruje RSA 2048 bitovy klucovy par
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
}
