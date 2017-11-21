import java.io.*;
import java.util.HashMap;

/**
 * @input: peerID
 * read in PeerInfo.cfg, initialize the peer with specified peerID
 */
public class peerProcess {

    public static void main(String[] args) {

        /* current peer information */
        int index = 0;      // peer index form 0 to n-1
        int peerID;         // peer with given ID to init
        int portNum = 0;    // host port number
        int fileStatus = 0; // does it have complete file?
        int nPeers = 0;     // total number of peers
        HashMap<Integer, String[]> peerInfo = new HashMap<>(); // peer index -> id, address.. info

        // read PeerInfo.cfg and set current peer configurations
        peerID = Integer.parseInt(args[0]);
        boolean findPeerID = false; // found the peer info with id?

        try {
            String filePath = System.getProperty("user.dir") + File.separator;
            BufferedReader br = new BufferedReader(new FileReader(filePath + "PeerInfo.cfg"));

            while (true) {

                // read peer information line by line
                String line = br.readLine();
                if (line == null) break; // all peers read
                String[] words = line.split(" ");
                peerInfo.put(nPeers, words);

                // find the peer information with given ID
                if (Integer.parseInt(words[0]) == peerID) {
                    index = nPeers;
                    portNum = Integer.parseInt(words[2]);
                    fileStatus = Integer.parseInt(words[3]);
                    findPeerID = true; // we have found the peer with given ID
                }
                nPeers++;
            }

        } catch (FileNotFoundException e) {
            System.err.println("PeerInfo.cfs does not exist!");

        } catch (IOException e) {
            e.printStackTrace();
        }

        // if given peer not found
        if (!findPeerID) {
            System.out.println("Cannot find the peer with ID " + peerID + "!");
            System.exit(0);
        }

        // read common configurations
        FileSharing.Common common = new FileSharing.Common();

        // initialize peer with given ID
//        System.out.println(index + " " + peerID + " " + portNum + " " + fileStatus + " " + nPeers);
        FileSharing.Host host = new FileSharing.Host(index, peerID, portNum, fileStatus, nPeers, peerInfo, common);
        host.start();
        System.out.println("Local host is started successfully.");

    }

}
