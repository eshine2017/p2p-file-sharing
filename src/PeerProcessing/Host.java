package PeerProcessing;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Host is responsible for parameters maintaining, establish connection to running severs,
 * and listen to connection request from other peers
 */
public class Host extends Thread {

    /** host information */
    private int hostID;
    private int portNum;
    private byte[] bitfield;

    /** neighbors information */
    private HashMap<Integer, Neighbor> neighborsInfo;

    /** constructor */
    public Host(int peerID, int portNum, int fileStatus) {
        this.hostID = peerID;
        this.portNum = portNum;
        neighborsInfo = new HashMap<>();

        // init bitfield (all 1 or all 0)
        int nPieces = peerProcess.FileSize/peerProcess.PieceSize + 1;
        this.bitfield= new byte[(nPieces-1)/8 + 1]; // set bitfield to all 0

        // flip bitfield if current peer already has complete file
        if (fileStatus == 1) {

            // under file path, divide the file into pieces
            String filePath = System.getProperty("user.dir") + File.separator
                    + "peer_" + hostID + File.separator;
            FileProcessing.FileProcess fp =
                    new FileProcessing.FileProcess(peerProcess.FileName, filePath,
                            filePath, peerProcess.FileSize, peerProcess.PieceSize);
            fp.divide();

            // flip 0s to 1s
            for (int i = 0; i < nPieces; i++) {
                bitfield[i] = (byte) ~bitfield[i];
            }

        }
    }

    /** establish connection with other servers then wait for request */
    public void run() {

        // first connect to all the peers listed before host in PeerInfo.cfg
        try {
            BufferedReader br = new BufferedReader(new FileReader("PeerInfo.cfg"));
            while (true) {
                String line = br.readLine();

                // print error if goes to the end but still not find current host
                if (line == null) {
                    System.out.println("Specified peerID not found!!!");
                    break;
                }

                // read peer information line by line, connect to the peers who are running right now
                String[] words = line.split(" ");
                if (Integer.parseInt(words[0]) == hostID) {
                    break;
                } else {

                    int peerID = Integer.parseInt(words[0]);
                    String peerAddress = words[1];
                    int peerPort = Integer.parseInt(words[2]);
                    int bitfieldStatus = Integer.parseInt(words[3]);

                    // create a new neighbor and open a client for file sharing
                    Neighbor neighbor = new Neighbor(peerID, peerAddress, peerPort);
                    neighbor.setBitfield(bitfieldStatus);
                    neighborsInfo.put(peerID, neighbor);
                    //(new Client(peerID, neighborsInfo)).start();

                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // wait for connection request from other peers
        try {
            ServerSocket hostSocket = new ServerSocket(portNum);
            while (true) {
                Socket serverSocket = hostSocket.accept();
                // create a new server for file sharing
                //(new Server(serverSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
