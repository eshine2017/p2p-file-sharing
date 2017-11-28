package FileSharing;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.BitSet;

/**
 * Define a neighbor who is connected with current host
 */
public class Neighbor {

    /* instance variables */
    private int index;
    private int peerID;
    private String address;
    private int portNum;
    private BitSet bitfield;
    private int nPieces;
    private PeerToPeer thread;
    private Socket connection;
    ObjectInputStream in;
    ObjectOutputStream out;

    /** constructor (connection established from current host) */
    public Neighbor(int index, int peerID, String address, int portNum, int nPieces,
                    Socket connection, ObjectInputStream in, ObjectOutputStream out) {
        this.index = index;
        this.peerID = peerID;
        this.address = address;
        this.portNum = portNum;
        this.nPieces = nPieces;
        this.connection = connection;
        this.in = in;
        this.out = out;
    }

    /** constructor (connection established from other peers) */
    public Neighbor(int index, int nPieces, Socket connection,
                    ObjectInputStream in, ObjectOutputStream out) {
        this.index = index;
        this.nPieces = nPieces;
        this.connection = connection;
        this.in = in;
        this.out = out;
    }

    /** get index of this neighbor */
    public int getIndex() {
        return index;
    }

    /** get ID of this neighbor */
    public int getPeerID() {
        return peerID;
    }

    /** set peer ID to given value */
    public void setPeerID(int peerID) {
        this.peerID = peerID;
    }

    /** get current bitfield */
    public BitSet getBitfield() {
        return bitfield;
    }

    /** check bitfield status at index */
    public boolean checkBitfield(int index) {
        return bitfield.get(index);
    }

    /** set bitfield to given BitSet */
    public synchronized void setBitfield(BitSet bitfield) {
        this.bitfield = bitfield;
    }

    /** set bit at index to true */
    public synchronized void updateBitfield(int index) {
        bitfield.set(index);
    }

    /** check whether this neighbor has got complete file */
    public synchronized boolean isComplete() {
        return bitfield.nextClearBit(0) >= nPieces;
    }

    /** attach p2p thread to this neighbor */
    public void setThread(PeerToPeer thread) {
        this.thread = thread;
    }

    /** close connection to this neighbor */
    public void closeConnection() {
        try {
            thread.stopRunning();
            in.close();
            out.close();
            connection.close();

        } catch(IOException ioException){
            System.out.println("Disconnect with peer " + peerID);
        }
    }

    @Override
    public String toString() {
        String res = "";
        return res + "index: " + index + "; peerID: " + peerID
                + "; address: " + address + "; portNum: " + portNum;
    }

}
