package FileSharing;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Host is responsible for parameters maintaining, establish connection to running severs,
 * and listen to connection request from other peers
 */
public class Host extends Thread {

    /* host information */
    private int index;          // host index for mapping 0 - nPeers
    private int hostID;         // host ID
    private int portNum;        // port number
    private BitSet bitfield;    // file status in bits
    private int nPieces;        // number of pieces divided from original file
    private int nPeers;         // number of total peers
    private Common common;      // common configurations

    /* neighbors information */
    private int nConnectedPeers;                        // number of connected peers
    private boolean[] isInterestedOnMe;                 // are peers (index) interested on host
    //private boolean[] isChocked;                        // are peers (index) chocked by host
    private BitSet completedLabel;                      // does peer own whole file?
    private HashMap<Integer, String[]> peerInfo;        // index -> peer info from PeerInfo.cfg
    private HashMap<Integer, Neighbor> neighborsInfo;   // neighbor index -> neighbor information
    private HashMap<Integer, Integer> sharingRate;      // neighbor index -> sending rate in n of parts

    /** constructor */
    public Host(int index, int peerID, int portNum, int fileStatus, int nPeers,
                HashMap<Integer, String[]> peerInfo, Common common) {

        // init host info
        this.index = index;
        this.hostID = peerID;
        this.portNum = portNum;
        nPieces = common.FileSize/common.PieceSize;
        if (common.FileSize%common.PieceSize != 0) nPieces++;
        this.bitfield = initBitfield(fileStatus); // init bitfield (all 1 or all 0)
        this.nPeers = nPeers;
        this.common = common;

        // init neighbors info
        nConnectedPeers = 0;
        isInterestedOnMe = new boolean[nPeers];
        // when start, all neighbors are chocked
        //isChocked = new boolean[nPeers];
        //for (int i = 0; i < nPeers; i++) isChocked[i] = true;
        completedLabel = new BitSet(nPeers);
        this.peerInfo = peerInfo;
        neighborsInfo = new HashMap<>();
        sharingRate = new HashMap<>();

        // if already have complete file, divide the file into parts
        if (fileStatus == 1) {
            String filePath = System.getProperty("user.dir") + File.separator
                    + "peer_" + hostID + File.separator;
            FileProcessing.FileProcess fp =
                    new FileProcessing.FileProcess(common.FileName, filePath,
                            filePath, common.FileSize, common.PieceSize);
            fp.divide();
            // set complete label to true
            completedLabel.set(index);
        }

    }

    /* calculate bitfield per file status: 0 or 1 */
    private BitSet initBitfield(int fileStatus) {
        BitSet bitfield = new BitSet(nPieces);          // set bitfield to all 0
        if (fileStatus == 1) bitfield.flip(0, nPieces); // flip all bits if have complete file
        return bitfield;
    }

    /** establish connection with other servers then wait for request */
    public void run() {

        // first connect to all the peers listed before host in PeerInfo.cfg
        for (int i = 0; i < index; i++) {
            System.out.println("Now connecting to peer " + i);
            connectTo(i, peerInfo.get(i));
        }
        nConnectedPeers = index;
        System.out.println("Successfully connected to all running peers.");

        // open a Choke thread
        Chock choke = new Chock(sharingRate, isInterestedOnMe, common, neighborsInfo);
        choke.start();

        // wait for connection request from other peers
        try {
            ServerSocket hostSocket = new ServerSocket(portNum);
            // as long as there are peers who do not get the whole file
            while (completedLabel.nextClearBit(0) < nPeers) {

                // accept connection request from other peer
                Socket socket = hostSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                // add a new neighbor
                int index = ++nConnectedPeers;
                Neighbor neighbor = new Neighbor(index, nPieces, socket, in, out);
                neighborsInfo.put(index, neighbor);

                // create a new server for file sharing
                PeerToPeer thread = new PeerToPeer(neighborsInfo, index, this.index, false, nPeers, common);

            }
            hostSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // close all sockets and threads
        choke.stopRunning();
        System.out.println("Awesome, all peers have gotten the file!!!");

    }

    /* connect to the given peer */
    private void connectTo(int index, String[] peerInfo) {

        // read in peer information
        int peerID = Integer.parseInt(peerInfo[0]);
        String peerAddress = peerInfo[1];
        int peerPort = Integer.parseInt(peerInfo[2]);
        BitSet bitfield = initBitfield(Integer.parseInt(peerInfo[3]));
        Socket socket = null;           // connection socket
        ObjectOutputStream out = null;  // output buffer
        ObjectInputStream in = null;    // input buffer

        // create a new socket, init input and output stream
        try{
            socket = new Socket(peerAddress, peerPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

        } catch (ConnectException e) {
            System.err.println("Connection refused.");

        } catch(UnknownHostException unknownHost){
            System.err.println("Unknown host!");

        } catch(IOException ioException){
            ioException.printStackTrace();
        }

        // create a new neighbor and link it to given index
        Neighbor neighbor = new Neighbor(index, peerID, peerAddress, peerPort, nPieces, socket, in, out);
        neighbor.setBitfield(bitfield);
        neighborsInfo.put(index, neighbor);

        // open a new thread for file sharing between host and this peer
        PeerToPeer thread = new PeerToPeer(neighborsInfo, index, this.index, true, nPeers, common);

    }

}
