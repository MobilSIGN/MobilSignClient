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
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import org.apache.commons.codec.binary.Base64;
import sun.security.rsa.RSAPublicKeyImpl;

public class MobilSignClient {

    private String serverAddress; // ip adresa servera
    private int serverPort; // port na ktorom server pocuva
    private PrivateKey applicationKey; // kluc desktopovej aplikacie
    private RSAPublicKey mobileKey; // kluc aplikacie v mobile
    private JTextArea console; // consola z GUI, zaznamenava cinnost
    private Sender clientSender;
    private Listener clientListener;
    private Socket socket;
    private Crypto crypto;

    public MobilSignClient(String serverAddress, int serverPort, JTextArea console) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.console = console;
        generateKeys();
        crypto = new Crypto(applicationKey);
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
                System.out.println("typ spravy: " + typSpravy);
                System.out.println("telo spravy: " + teloSpravy);
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
                // Teraz zasifrujeme verejny kluc mobila sukromnym klucom PC a 
                // verejnym klucom mobilu a posleme do mobilu
//                String mobilePublicKey = Base64.encodeBase64String(Crypto.encrypt(response.getBytes(), applicationKey));
//                Base64.encodeBase64String(Crypto.encrypt(mobilePublicKey.getBytes(), new RSAPublicKeyImpl(response.getBytes())));
                // ulozit niekde
                break;
            default:
                System.err.println("Zly format spravy 2.");
                return false;
        }
        console.append(msg + "\n");
        return true;
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
            //hints.put(EncodeHintType.PDF417_COMPACT, true);
            //hints.put(EncodeHintType.PDF417_COMPACTION, Compaction.NUMERIC);

            String base64 = Base64.encodeBase64String(data.toByteArray());
//            System.out.println("Base64: " + base64);

            BitMatrix bitMatrix = writer.encode(base64, BarcodeFormat.QR_CODE, 750, 750, hints); // vytvori QR kod ako maticu bitov z bigintegera
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix); // prevedie QR kod matice bitov do obrazku
            return qrImage;
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
            System.out.println("mobile key: " + mobileKey.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            KeyPairGenerator generatorRSA = KeyPairGenerator.getInstance("RSA"); // vytvori instanciu generatora RSA klucov
            generatorRSA.initialize(2048, new SecureRandom()); // inicializuje generator 2048 bitovych RSA klucov
            KeyPair keyRSA = generatorRSA.generateKeyPair(); // vygeneruje klucovi par
            PrivateKey private1 = keyRSA.getPrivate(); // kluc desktopovej aplikacie je sukromny kluc z klucoveho paru
            PublicKey public1 = (RSAPublicKey) keyRSA.getPublic(); // vrati verejny kluc type RSAPublicKey, lebo z neho mozem dostat modulus 

            generatorRSA = KeyPairGenerator.getInstance("RSA"); // vytvori instanciu generatora RSA klucov
            generatorRSA.initialize(2048, new SecureRandom()); // inicializuje generator 2048 bitovych RSA klucov
            keyRSA = generatorRSA.generateKeyPair(); // vygeneruje klucovi par
            PrivateKey private2 = keyRSA.getPrivate(); // kluc desktopovej aplikacie je sukromny kluc z klucoveho paru
            PublicKey public2 = (RSAPublicKey) keyRSA.getPublic(); // vrati 


            Crypto cryptoPrivate1 = new Crypto(private1);
            Crypto cryptoPrivate2 = new Crypto(private2);
            Crypto cryptoPublic1 = new Crypto(public1);
            Crypto cryptoPublic2 = new Crypto(public2);


            String text = "abcd";

            System.out.println(cryptoPublic1.decrypt(cryptoPrivate1.encrypt(text)));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
