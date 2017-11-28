package FileSharing;

import java.io.*;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.ByteBuffer;
import java.math.BigInteger;
import Communication.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeerToPeer extends Thread {

    private volatile boolean running;

    private int peerid;

    private int myid;

    private boolean handshake_flag;

    private ConcurrentHashMap<Integer, Neighbor> neighborsInfo;

    private int FileSize;

    private int PieceSize;

    //private double npieced;

    private int npiece;

    private ConcurrentHashMap<Integer, Integer> Rate;

    private int ID_me;

    private BitSet bitfield;

    private BitSet completedLabel;

    private boolean[] isIntersetedOnMe;

    boolean[] Myinterest;

    private boolean[] pieceRequested;

    private boolean chokeflag=true;

    private String filePath;

    String fileName;

    public PeerToPeer(ConcurrentHashMap<Integer, Neighbor> neighborsInfo, int index_peer, int index_me, int ID_me,
                      BitSet bitfield, boolean[] pieceRequested, BitSet completedLabel, boolean[] isIntersetedOnMe,
                      boolean handshake_flag, Common common, ConcurrentHashMap<Integer, Integer> Rate, boolean[] Myinterest) {
        this.neighborsInfo = neighborsInfo;
        // neighborsInfo.get(index).in or .out
        this.peerid = index_peer;
        this.myid = index_me;
        this.handshake_flag = handshake_flag;//true=client
        this.FileSize = common.FileSize;
        this.PieceSize = common.PieceSize;
        this.Rate=Rate;
        this.ID_me=ID_me;
        this.bitfield=bitfield;
        //System.out.println(myid + "'s bitfield: " + bitfield);
        this.completedLabel=completedLabel;
        this.isIntersetedOnMe=isIntersetedOnMe;
        this.pieceRequested=pieceRequested;
        npiece = common.FileSize/common.PieceSize;
        if (common.FileSize%common.PieceSize != 0) npiece++;
        filePath = System.getProperty("user.dir") + File.separator
                + "peer_" + ID_me + File.separator;
        this.fileName = common.FileName;
        running = true;
        this.Myinterest=Myinterest;

    }

    private void createsendhandshake() {
        String standardhead = "P2PFILESHARINGPROJ";
        Handshake message = new Handshake(standardhead, ID_me);
        try {
            ObjectOutputStream out;
            synchronized (neighborsInfo) {
                out = neighborsInfo.get(peerid).out;
            }
            synchronized (out) {
                //stream write the message
                out.writeObject(message);
                out.flush();
            }

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private boolean checkhandshake(Handshake peerhandshake, Neighbor a) {
        boolean head_flag = false;
        boolean peerid_flag = false;
        String standardhead = "P2PFILESHARINGPROJ";
        String header = peerhandshake.getHeader();
        int peeridhead = peerhandshake.getPeerID();
        if (header.equals(standardhead)) {
            head_flag = true;
        }
        if (peeridhead == (a.getPeerID())) {
            peerid_flag = true;
        }
        return head_flag & peerid_flag;
    }

    private void proc_sendmessage(Neighbor a, Message sendmessage) {
        try {
            synchronized (a.out) {
                //stream write the message
                a.out.writeObject(sendmessage);
                a.out.flush();
            }

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private int checkneed(Neighbor peer)/*check unchoke*/ {
        BitSet bitfieldpeer = peer.getBitfield();
        int n=0;
        int length=npiece;
        //System.out.println(npiece);
        int[] intArray0 = new int[length];
        synchronized (bitfield) {
            for (int i = 0; i < length; i++) {
                synchronized (bitfieldpeer) {
                    //if (bitfieldpeer.get(i)&&!bitfield.get(i)&&!pieceRequested[i]) {///& not requested
                    if (bitfieldpeer.get(i) && !bitfield.get(i)) {///& not requested
                        intArray0[n] = i;
                        n = n + 1;
                    }
                }
            }
        }
        Random indexselect = new Random();
        if(n==0){
            return -1;
        }
        int h;
        h = indexselect.nextInt(n);
        return intArray0[h];
    }

    private byte[] int2byte(final int i) {

        ByteBuffer settobyte = ByteBuffer.allocate(4);
        settobyte.putInt(i);
        return settobyte.array();

    }

    private int byte2int(byte[] b1) {

        return new BigInteger(b1).intValue();
    }

    private boolean checkneedbit(Message peerhave)/*check have*/ {
        byte[] bitindex = peerhave.getPayload();
        int bitindexpeer = byte2int(bitindex);
        synchronized (bitfield) {
            return bitfield.get(bitindexpeer);
        }
    }

    private boolean checkinterest(Neighbor peer)/*check bitfield*/ {
        boolean need_flag2 = false;
        BitSet bitfieldpeer = peer.getBitfield();
        int length=npiece;
        synchronized (bitfield) {
            for (int i = 0; i < length; i++) {
                synchronized (bitfieldpeer) {
                    if (bitfieldpeer.get(i) && !bitfield.get(i)) {
                        need_flag2 = true;
                    }
                }
            }
        }
        return need_flag2;
    }

    private byte[] proc_sendpiece(Message sendmessage) {

        int index = byte2int(sendmessage.getPayload());
        int size;
        if (index < npiece-1) {
            size = PieceSize;
        } else {
            size = FileSize - (npiece - 1) * PieceSize;
        }
        byte[] what = new byte[size];

        try {
            File file = new File(filePath + fileName + index + ".part");
            FileInputStream in = new FileInputStream(file);
            in.read(what);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return what;
    }

    private void downloadprocess(Message piecemessage) {
        int index = byte2int(piecemessage.getIndex());
        try {
            //File file = new File(index+".part");
            //FileOutputStream out = new FileOutputStream(file);
            FileOutputStream out = new FileOutputStream(filePath + fileName + index + ".part");
            out.write(piecemessage.getPayload());
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendhavetoallpeer(Message havepiece) {
        synchronized (neighborsInfo) {
            for (Neighbor x : neighborsInfo.values()) {
                proc_sendmessage(x, havepiece);
                //writelog("Peer " + ID_me + " sent the 'have' message to " + x.getPeerID());
            }
        }
    }

    public static BitSet byte2bit(byte[] bytes) {
        BitSet bits = new BitSet();
        for (int i=0; i<bytes.length*8; i++) {
            if ((bytes[bytes.length-i/8-1]&(1<<(i%8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    public static byte[] bit2byte(BitSet bits) {
        byte[] bytes = new byte[bits.length()/8+1];
        for (int i=0; i<bits.length(); i++) {
            if (bits.get(i)) {
                bytes[bytes.length-i/8-1] |= 1<<(i%8);
            }
        }
        return bytes;
    }

    private void writelog(String log){

        String logname= filePath+"log_peer_"+ ID_me + ".log";
        try {
            SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String logtime = time.format(new Date().getTime());
            log="[" + logtime + "]: " + log;
            FileWriter fw = new FileWriter(new File(logname), true);
            PrintWriter pw = new PrintWriter(fw);
            pw.println(log);
            pw.flush();
            fw.flush();
            pw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
 /*   private class Filewriter {

        private static final String LINE_SEPARATOR = System.getProperty("line.separator");

        public static void main(String[] args) throws IOException {
            FileWriter fw = new FileWriter("C:\\demo1.txt",false);
            fw.write("aello"+LINE_SEPARATOR+"world!");
            fw.close();
        }
    }
*/
 /*   private void writelog(String status, int myid)
    {
        String filename="log_peer_"+myid+".log";
        File file = new File(filename);
        PrintStream out = new PrintStream(file);
        out.println(status);
        out.close();
    }

*/

    public void run() {

        int length = 10; // temp length, all set to 10
        Neighbor a=null;

        synchronized (neighborsInfo){
            a = neighborsInfo.get(peerid);
        }

        // if this is client, send handshake immediately
        if (handshake_flag) {
            createsendhandshake();
            System.out.println(myid + " sent handshake to " + peerid);
            writelog("Peer " + ID_me + " makes a connection to Peer " + a.getPeerID());
        }

//        System.out.println(myid + " goes to main loop.");
        while (running) {
            Object receivemessage = null;
            try {
                //a.in = new ObjectInputStream();
                synchronized (a.in) {
                    receivemessage = a.in.readObject();
                }

            } catch (SocketException e) {
                // this is fine

            } catch (EOFException e) {
                // this is fine

//            } catch (StreamCorruptedException e) {
//                try {
//                    sleep(100);
//                } catch (InterruptedException error) {
//                    error.printStackTrace();
//                }

            } catch (IOException ioException) {
                ioException.printStackTrace();

            } catch (ClassNotFoundException e) {
                System.err.println("Data received in unknown format!");
            }

//            if (receivemessage != null)
//                System.out.println("Hey, " + myid + " received something!");

            if (receivemessage instanceof Handshake) {

                // if client, check neighbor ID, if error, kill this thread
                if (handshake_flag) {
                    if (!checkhandshake((Handshake) receivemessage, a)) break;

                // if server, update neighbor ID, send handshake
                } else {
                    a.setPeerID(((Handshake) receivemessage).getPeerID());
                    createsendhandshake();
                    System.out.println(myid + " sent handshake to " + peerid);
                    writelog("Peer " + ID_me + " is connected from Peer " + a.getPeerID());
                }

                // send bitfield immediately after handshake
                Message sendbitfield = new Message(length, 5, bit2byte(bitfield) );
                proc_sendmessage(a, sendbitfield);
                System.out.println(myid + " sent bitfield to " + peerid);
                //writelog("Peer " + ID_me + " sent the 'bitfield' message to Peer " + a.getPeerID());

            }

            if (receivemessage instanceof Message) {
                Message abs = (Message) receivemessage;
                if (abs.getType() == 0) { /*chock*/
                    chokeflag = true;
                    System.out.println(myid + " is chocked by " + peerid);
                    writelog("Peer " + ID_me + " is chocked by " + a.getPeerID());
                }

                if (abs.getType() == 1) { /*unchoke*/
                    chokeflag = false;
                    System.out.println(myid + " is unchocked by " + peerid);
                    writelog("Peer " + ID_me + " is unchocked by " + a.getPeerID());
                    int indexneed = checkneed(a);
                    byte[] indexbyte = int2byte(indexneed);
                    if (indexneed>=0) {
                        Message sendrequest = new Message(length, 6, indexbyte);
                        proc_sendmessage(a, sendrequest);  /*send request*/
                        pieceRequested[indexneed]=true;
//                        System.out.println(myid + " sent request to " + peerid + " for part." + indexneed);
                        //writelog("Peer " + ID_me + " sent the 'request' message to " + a.getPeerID());

                    }
                }////add not interest or not

                if (abs.getType() == 2) {
                    synchronized (isIntersetedOnMe) {
                        isIntersetedOnMe[peerid] = true; /*interest*/
                    }
                    System.out.println(myid + " received interest message from " + peerid);
                    writelog("Peer " + ID_me + " received the 'interested' message from " + a.getPeerID());
                    // skip chock, for test
//                    Message msg = new Message(1, 1, null);
//                    proc_sendmessage(a, msg);
                    //
                }

                if (abs.getType() == 3) {
                    synchronized (isIntersetedOnMe) {
                        isIntersetedOnMe[peerid] = false;/*not interest*/
                    }
                    System.out.println(myid + " received not interest message from " + peerid);
                    writelog("Peer " + ID_me + " received the 'not interested' message from " + a.getPeerID());
                }

                if (abs.getType() == 4) {/*have*/
                    writelog("Peer " + ID_me + " received the 'have' message from " + a.getPeerID());
                    int index = byte2int(abs.getPayload());
                    a.updateBitfield(index);
//                    System.out.println(peerid + " has part." + index);
                    if (a.isComplete()) {
                        completedLabel.set(peerid);
                    }
//                    System.out.println("File complete status: " + completedLabel);
                    synchronized (Myinterest) {
                        if (!checkneedbit(abs) & !Myinterest[peerid]) {
                            Message sendinterest = new Message(length, 2, null);
                            proc_sendmessage(a, sendinterest);
                            Myinterest[peerid] = true;
                            //writelog("Peer " + ID_me + " sent the 'interested' message to " + a.getPeerID());
                        }
                    }
                }

                if (abs.getType() == 5) {/*bitfield*/
                    //writelog("Peer " + ID_me + " received the 'bitfield' message from " + a.getPeerID());
                    a.setBitfield(byte2bit(abs.getPayload()));
                    System.out.println(myid + " received bitfield from " + peerid);
                    //System.out.println(myid + "'s bitfield: " + bitfield);
                    //System.out.println(peerid + "'s bitfield: " + a.getBitfield());
                    synchronized (Myinterest) {
                        if (checkinterest(a)) {
                            Message sendinterest = new Message(length, 2, null);
                            proc_sendmessage(a, sendinterest);
                            Myinterest[peerid] = true;
                            //writelog("Peer " + ID_me + " sent the 'interested' message to " + a.getPeerID());
                        } else {
                            Message sendnotinterest = new Message(length, 3, null);
                            proc_sendmessage(a, sendnotinterest);
                            Myinterest[peerid] = false;
                            //writelog("Peer " + ID_me + " sent the 'not interested' message to " + a.getPeerID());
                        }
                    }
                    synchronized (completedLabel) {
                        if (a.isComplete()) {
                            completedLabel.set(peerid);
                        }
//                    System.out.println("File complete status: " + completedLabel);
                    }
                }

                if (abs.getType() == 6) {/*request*/
                    //writelog("Peer " + ID_me + " received the 'request' message from " + a.getPeerID());
//                    System.out.println(myid + " received request from " + peerid + " for part." + byte2int(abs.getPayload()));
                    byte[] payload_file = proc_sendpiece(abs);
                    Message piecemessage = new Message(length, 7, abs.getPayload(), payload_file);
                    proc_sendmessage(a, piecemessage);
                    //writelog("Peer " + ID_me + " sent the 'piece' message to " + a.getPeerID());

                }

                if (abs.getType() == 7) {/*piece*/
                    //writelog("Peer " + ID_me + " received the 'piece' message from " + a.getPeerID());
//                    System.out.println(myid + " received piece part." + byte2int(abs.getIndex()));
                    if (!bitfield.get(byte2int(abs.getIndex()))) {
                        downloadprocess(abs);
//                    try {
//                        sleep(200);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                        writelog("Peer " + ID_me + " has downloaded the piece " + byte2int(abs.getIndex()) + " from " + a.getPeerID());
                        bitfield.set(byte2int(abs.getIndex()));

                    }
                        //System.out.println(myid + "'s bitfield: " + bitfield);
                        synchronized (Rate) {
                            Rate.put(peerid, Rate.get(peerid) + 1);
                        }
                        int newneed = checkneed(a);
                        if (newneed >= 0 & !chokeflag) {
                            Message sendrequest = new Message(length, 6, int2byte(newneed));
                            proc_sendmessage(a, sendrequest);
                            pieceRequested[newneed] = true;
                            //writelog("Peer " + ID_me + " sent the 'request' message to " + a.getPeerID());

                        }
//                    else if(newneed<0) {
//                        Message sendnotinterest = new Message(length, 3, null);
//                        proc_sendmessage(a, sendnotinterest);
//                    }

                    Message havepiece = new Message(length, 4, abs.getIndex());
                    sendhavetoallpeer(havepiece);
                    synchronized (bitfield) {
                        if (bitfield.nextClearBit(0) >= npiece) {
                            synchronized (completedLabel) {
                                if (!completedLabel.get(myid)) {
                                    completedLabel.set(myid);
                                    writelog("peer " + ID_me + " has downloaded the complete file.");
//                                Message completeme = new Message(length, 8, int2byte(myid));
//                                sendhavetoallpeer(completeme);
                                }
                            }
                        }
                    }

//                    System.out.println("File complete status: " + completedLabel);

                        synchronized (neighborsInfo) {
                            for (Neighbor x : neighborsInfo.values()) {//check all peer if interest
                                synchronized (Myinterest) {
                                    if (!checkinterest(x) && Myinterest[x.getIndex()]) {
                                        Message sendnotinterest = new Message(length, 3, null);
                                        proc_sendmessage(x, sendnotinterest);
                                        Myinterest[x.getIndex()] = false;
                                        //writelog("Peer " + ID_me + " sent the 'not interested' message to " + x.getPeerID());
                                    }
                                }
                            }
                        }

                }
                if (abs.getType() == 8)
                {
                    synchronized (completedLabel) {
                        synchronized (Myinterest) {
                            synchronized (bitfield) {
                                if (!(bitfield.nextClearBit(0) >= npiece)) {
                                    writelog("peer " + ID_me + " has downloaded the complete file.");
                                    bitfield.set(0, npiece);
                                }
                            }
                            completedLabel.set(0, Myinterest.length);
                        }
                    }
                }
            }
        }
        System.out.println("P2P from " + myid + " to " + peerid + " is stopped.");
    }

    public void stopRunning() {
        running = false;
    }

}
