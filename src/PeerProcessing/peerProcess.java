package PeerProcessing;

import java.io.*;

/**
 * @input: peerID
 * read in PeerInfo.cfg and Common.cfg, initialize the peer with specified peerID
 */
public class peerProcess {

    /* Common.cfg configurations */
    static int NumberOfPreferredNeighbors;
    static int UnchokingInterval;
    static int OptimisticUnchokingInterval;
    static String FileName;
    static int FileSize;
    static int PieceSize;

    /* current peer information */
    private static int peerID;          // peer with given ID to init
    private static int portNum;         // host port number
    private static int fileStatus;      // does it have complete file?

    /* read Common.cfg and set all the specified configurations */
    private static void setConfig() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("Common.cfg"));

            // read in configurations one by one
            String line = br.readLine();
            String[] words = line.split(" ");
            NumberOfPreferredNeighbors = Integer.parseInt(words[1]);

            line = br.readLine();
            words = line.split(" ");
            UnchokingInterval = Integer.parseInt(words[1]);

            line = br.readLine();
            words = line.split(" ");
            OptimisticUnchokingInterval = Integer.parseInt(words[1]);

            line = br.readLine();
            words = line.split(" ");
            FileName = words[1];

            line = br.readLine();
            words = line.split(" ");
            FileSize = Integer.parseInt(words[1]);

            line = br.readLine();
            words = line.split(" ");
            PieceSize = Integer.parseInt(words[1]);

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        // read PeerInfo.cfg and set current peer configurations
        peerID = Integer.parseInt(args[0]);
        try {
            BufferedReader br = new BufferedReader(new FileReader("PeerInfo.cfg"));
            while (true) {

                String line = br.readLine();

                // specified peerID not found in PeerInfo.cfg, print error
                if (line == null) {
                    System.out.println("Specified peerID not found!!!");
                    break;
                }

                String[] words = line.split(" ");

                // set peer configurations
                if (Integer.parseInt(words[0]) == peerID) {
                    portNum = Integer.parseInt(words[2]);
                    fileStatus = Integer.parseInt(words[3]);
                    break;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // set Common.cfg configurations
        setConfig();

        // initialize peer with given ID
        Host host = new Host(peerID, portNum, fileStatus);
        host.start();
    }

}
