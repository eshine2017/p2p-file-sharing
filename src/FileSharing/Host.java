package FileSharing;

import Communication.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Host is responsible for parameters maintaining, establish connection to running severs,
 * and listen to connection request from other peers
 */
public class Host extends Thread {

    /* host information */
    private int index;                  // host index for mapping 0 - nPeers
    private int hostID;                 // host ID
    private int portNum;                // port number
    private BitSet bitfield;            // file status in bits
    private boolean[] pieceRequested;   // piece request sent
    private int nPieces;                // number of pieces divided from original file
    private int nPeers;                 // number of total peers
    private Common common;              // common configurations
    private FileProcessing.FileProcess fp;
    //private String filePath;

    /* neighbors information */
    private int nConnectedPeers;                        // number of connected peers
    private boolean[] isInterestedOnMe;                 // are peers (index) interested on host
    private boolean[] interestPeers;                    // peers (index) I am interested in
    //private boolean[] isChocked;                        // are peers (index) chocked by host
    private BitSet completedLabel;                      // does peer own whole file?
    private HashMap<Integer, String[]> peerInfo;        // index -> peer info from PeerInfo.cfg
    private ConcurrentHashMap<Integer, Neighbor> neighborsInfo;   // neighbor index -> neighbor information
    private ConcurrentHashMap<Integer, Integer> sharingRate;      // neighbor index -> sending rate in n of parts

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
//        System.out.println(index + "'s bitfield: " + bitfield);
        pieceRequested = new boolean[nPieces];
        this.nPeers = nPeers;
//        System.out.println("I have " + nPeers + "peers.");
        this.common = common;

        // init neighbors info
        nConnectedPeers = 0;
        isInterestedOnMe = new boolean[nPeers];
        interestPeers = new boolean[nPeers];
        // when start, all neighbors are chocked
        //isChocked = new boolean[nPeers];
        //for (int i = 0; i < nPeers; i++) isChocked[i] = true;
        completedLabel = new BitSet(nPeers);
        this.peerInfo = peerInfo;
        neighborsInfo = new ConcurrentHashMap<>();
        sharingRate = new ConcurrentHashMap<>();
        //for (int i = 0; i < nPeers; i++) sharingRate.put(i, 0);

        String filePath = System.getProperty("user.dir") + File.separator
                + "peer_" + hostID + File.separator;

        // check if given directory exist
        File dir = new File(filePath);
        if (!dir.exists()) dir.mkdir();

        // delete old log and create a new one
        File file = new File(filePath + "log_peer_" + hostID + ".log");
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fp = new FileProcessing.FileProcess(common.FileName, filePath,
                filePath, common.FileSize, common.PieceSize);
        // if already have complete file, divide the file into parts
        if (fileStatus == 1) {
            fp.divide();
            // set complete label to true
            completedLabel.set(index);

            //fp.combine(filePath);
        }

    }

    /* calculate bitfield per file status: 0 or 1 */
    private BitSet initBitfield(int fileStatus) {
        BitSet bitfield = new BitSet(nPieces);          // set bitfield to all 0
        if (fileStatus == 1) bitfield.flip(0, nPieces); // flip all bits if have complete file
        //System.out.println(bitfield);
        return bitfield;
    }

    /** establish connection with other servers then wait for request */
    public void run() {

        // first connect to all the peers listed before host in PeerInfo.cfg
        for (int i = 0; i < index; i++) {
            connectTo(i, peerInfo.get(i));
            System.out.println("Now connected to peer " + i);
        }
        nConnectedPeers = index;
        System.out.println("Successfully connected to all running peers.");

        // open a Choke thread
        Chock choke = new Chock(sharingRate, isInterestedOnMe, common, neighborsInfo, bitfield, hostID);
        choke.start();

        // open a Server to listen to TCP request from other peers
        Server server = new Server();
        server.start();

        // wait until every peer gets complete file
        while (true) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Current complete label: " + completedLabel);
            synchronized (completedLabel) {
                if (completedLabel.nextClearBit(0) >= nPeers) break;
            }
        }


        // close all sockets and threads
        String filePath = System.getProperty("user.dir") + File.separator
                + "peer_" + hostID + File.separator;
//        System.out.println(filePath);
//        FileProcessing.FileProcess fp = new FileProcessing.FileProcess(common.FileName, filePath,
//                filePath, common.FileSize, common.PieceSize);
        fp.combine(filePath);
//        try {
//            sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        choke.stopRunning();
        server.stopRunning();
        synchronized (neighborsInfo) {
            for (Neighbor neighbor : neighborsInfo.values()) neighbor.closeConnection();
        }

        // wait 10 sec for service to stop
        try {
            sleep(10000);
            System.out.println("Awesome, all peers have gotten the file!!!");
            System.exit(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void endService() {
        Message msg = new Message(1, 8, null);
        synchronized (neighborsInfo) {
            for (Neighbor x : neighborsInfo.values()) {
                try {
                    synchronized (x.out) {
                        //stream write the message
                        x.out.writeObject(msg);
                        x.out.flush();
                    }

                } catch (SocketException e) {
                    // this is fine

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    private class Server extends Thread {

        private volatile boolean running;
        private ServerSocket hostSocket;

        Server() { running = true; }

        public void run() {

            // wait for connection request from other peers
            try {
                hostSocket = new ServerSocket(portNum);
                // as long as there are peers who do not get the whole file
                while (running) {
//                    System.out.println("Host complete label: " + completedLabel);

                    // accept connection request from other peer
//                    System.out.println("Host is listening to the socket.");
                    Socket socket = hostSocket.accept();
//                    System.out.println("Host received connection.");
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.flush();
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                    // add a new neighbor
                    int neighborIndex = ++nConnectedPeers;
                    Neighbor neighbor = new Neighbor(neighborIndex, nPieces, socket, in, out);

                    // set neighbor bitfield to all 0
                    BitSet bits = initBitfield(0);
                    neighbor.setBitfield(bits);
                    synchronized (neighbor) {
                        neighborsInfo.put(neighborIndex, neighbor);
                    }
                    synchronized (sharingRate) {
                        sharingRate.put(neighborIndex, 0);
                    }

                    // create a new server for file sharing
                    PeerToPeer thread = new PeerToPeer(neighborsInfo, neighborIndex, index, hostID,
                            bitfield, pieceRequested, completedLabel, isInterestedOnMe, false,
                            common, sharingRate, interestPeers);
                    neighbor.setThread(thread);
                    thread.start();
                    System.out.println(index + " get connected from " + neighborIndex);

                }

            } catch (SocketException e) {
                // this is fine

            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Server is stopped.");
        }

        public void stopRunning() {
//            endService();
            running = false;
            try {
                hostSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
        synchronized (neighborsInfo) {
            neighborsInfo.put(index, neighbor);
        }
        synchronized (sharingRate) {
            sharingRate.put(index, 0);
        }

        // open a new thread for file sharing between host and this peer
        PeerToPeer thread = new PeerToPeer(neighborsInfo, index, this.index, hostID, this.bitfield,
                pieceRequested, completedLabel, isInterestedOnMe, true,
                common, sharingRate, interestPeers);
        neighbor.setThread(thread);
        thread.start();

    }

}
