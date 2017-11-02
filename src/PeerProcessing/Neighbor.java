package PeerProcessing;

/**
 * Define a neighbor who is connected with current host
 */
public class Neighbor {

    /* instance variables */
    private int peerID;
    private String address;
    private int portNum;
    private byte[] bitfield;

    /** constructor */
    public Neighbor(int peerID, String address, int portNum) {
        this.peerID = peerID;
        this.address = address;
        this.portNum = portNum;
    }

    /** set bitfiled per file status: 0 or 1 */
    public void setBitfield(int fileStatus) {

        // init bitfield (all 1 or all 0)
        int nPieces = peerProcess.FileSize/peerProcess.PieceSize + 1;
        this.bitfield= new byte[(nPieces-1)/8 + 1]; // set bitfield to all 0

        // flip bitfield if current peer already has complete file
        if (fileStatus == 1) {
            for (int i = 0; i < nPieces; i++) {
                bitfield[i] = (byte) ~bitfield[i];
            }
        }

    }

    /** set bitfiled per bitfiled[] */
    public void setBitfield(byte[] bitfield) {
        this.bitfield = bitfield;
    }

    /** get neighbor address */
    public String getAddress() {
        return this.address;
    }

}
