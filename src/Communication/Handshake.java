package Communication;

import java.io.Serializable;

/**
 * Handshake message containing handshake header, 10 bytes zero bits and 4 bytes peer ID
 */
public class Handshake implements Serializable {

    /** instance variables */
    private String header;      // handshake header
    private byte[] zeroBits;    // 10 bytes zero bits
    private int peerID;         // peer ID

    /** constructor */
    public Handshake(int peerID) {
        header = "P2PFILESHARINGPROJ";
        zeroBits = new byte[10];
        this.peerID = peerID;
    }

    /** return handshake header */
    public String getHeader() {
        return this.header;
    }

    /** return peerID */
    public int getPeerID() {
        return this.peerID;
    }
}
