package Communication;

import java.io.Serializable;

/**
 * Actual P2P message containing 4 bytes length, 1 byte type and a payload with variable size
 */
public class Message implements Serializable {

    /** instance variables */
    private int length;     // message length
    private int type;       // message type
    private byte[] index;   // piece index, only valid when type = 7
    private byte[] payload; // message body

    /** constructor for "piece" message */
    public Message(int length, int type, byte[] index, byte[] piece) {
        this.length = length;
        this.type = type;
        this.index = index;
        this.payload = piece;
    }

    /** constructor for other messages */
    public Message(int length, int type, byte[] payload) {
        this.length = length;
        this.type = type;
        this.payload = payload;
    }

    /** return message length */
    public int getLength() {
        return this.length;
    }

    /** return message type */
    public int getType() {
        return this.type;
    }

    /** return piece index, only valid when type equal 7 */
    public byte[] getIndex() {
        return this.index;
    }

    /** return message payload */
    public byte[] getPayload() {
        return this.payload;
    }

}
