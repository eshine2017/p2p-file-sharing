package FileSharing;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.math.BigInteger;
import Communication.*;

public class PeerToPeer extends Thread {

    private volatile boolean running;

    private int peerid;

    private int myid;

    private boolean handshake_flag;

    private HashMap<Integer, Neighbor> neighborsInfo;

    private int FileSize;

    private int PieceSize;

    //private double npieced;

    private int npiece;

    private HashMap<Integer, Integer> Rate;

    private int ID_me;

    private BitSet bitfield;

    private BitSet completedLabel;

    private boolean[] isIntersetedOnMe;

    private boolean[] pieceRequested;

    private boolean chokeflag=true;

    public PeerToPeer(HashMap<Integer, Neighbor> neighborsInfo, int index_peer, int index_me, int ID_me,
                      BitSet bitfield, boolean[] pieceRequested, BitSet completedLabel, boolean[] isIntersetedOnMe,
                      boolean handshake_flag, Common common, HashMap<Integer, Integer> Rate) {
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
        System.out.println(myid + "'s bitfield: " + bitfield);
        this.completedLabel=completedLabel;
        this.isIntersetedOnMe=isIntersetedOnMe;
        this.pieceRequested=pieceRequested;
        npiece = (int) Math.ceil(FileSize / PieceSize);
        running = true;

    }

    private void createsendhandshake() {
        String standardhead = "P2PFILESHARINGPROJ";
        Handshake message = new Handshake(standardhead, ID_me);
        try {
            //stream write the message
            neighborsInfo.get(peerid).out.writeObject(message);
            neighborsInfo.get(peerid).out.flush();
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
            //stream write the message
            a.out.writeObject(sendmessage);
            a.out.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private int checkneed(Neighbor peer)/*check unchoke*/ {
        BitSet bitfieldpeer = peer.getBitfield();
        int n=0;
        int length=npiece;
        int[] intArray0 = new int[length];
        for (int i = 0; i < length; i++) {
            if (bitfieldpeer.get(i)&&!bitfield.get(i)&&!pieceRequested[i]) {///& not requested
                intArray0[n] = i;
                n = n + 1;
            }
        }
        Random indexselect = new Random();
        int h;
        h = indexselect.nextInt(n);
        if(n==0){
            return -1;
        }
        else{
            return intArray0[h];
        }
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
        return bitfield.get(bitindexpeer);
    }

    private boolean checkinterest(Neighbor peer)/*check bitfield*/ {
        boolean need_flag2 = false;
        BitSet bitfieldpeer = peer.getBitfield();
        int length=npiece;
        for (int i = 0; i < length; i++) {
            if (bitfieldpeer.get(i)&&!bitfield.get(i)) {
                need_flag2 = true;
            }
        }
        return need_flag2;
    }

    private byte[] proc_sendpiece(Message sendmessage) {

        int index = byte2int(sendmessage.getIndex());
        int size;
        if (index == npiece) {
            size = PieceSize;
        } else {
            size = FileSize - (npiece - 1) * PieceSize;
        }
        byte[] what = new byte[size];

        try {
            File file = new File(index + ".part");
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
            FileOutputStream out = new FileOutputStream(index + ".part");
            out.write(piecemessage.getPayload());
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendhavetoallpeer(Message havepiece) {
        for (Neighbor x : neighborsInfo.values()) {
            proc_sendmessage(x, havepiece);
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

        Neighbor a = neighborsInfo.get(peerid);

        // if this is client, send handshake immediately
        if (handshake_flag) {
            createsendhandshake();
            System.out.println(myid + " sent handshake to " + peerid);
        }

        System.out.println(myid + " goes to main loop.");
        while (running) {
            Object receivemessage = null;
            try {
                receivemessage = a.in.readObject();
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
                }

                // send bitfield immediately after handshake
                Message sendbitfield = new Message(length, 5, bit2byte(bitfield) );
                proc_sendmessage(a, sendbitfield);
                System.out.println(myid + " sent bitfield to " + peerid);

            }

            if (receivemessage instanceof Message) {
                Message abs = (Message) receivemessage;
                if (abs.getType() == 0) { /*chock*/
                    chokeflag = true;
                    System.out.println(myid + " is chocked by " + peerid);
                }

                if (abs.getType() == 1) { /*unchoke*/
                    chokeflag = false;
                    System.out.println(myid + " is unchocked by " + peerid);
                    int indexneed = checkneed(a);
                    byte[] indexbyte = int2byte(indexneed);
                    if (indexneed>=0) {
                        Message sendrequest = new Message(length, 6, indexbyte);
                        proc_sendmessage(a, sendrequest);  /*send request*/
                        pieceRequested[indexneed]=true;
                        System.out.println(myid + " sent request to " + peerid);
                    }
                }////add not interest or not

                if (abs.getType() == 2) {
                    isIntersetedOnMe[peerid]=true; /*interest*/
                    System.out.println(myid + " get interest to " + peerid);
                }

                if (abs.getType() == 3) {
                    isIntersetedOnMe[peerid]=false;/*not interest*/
                    System.out.println(myid + " is no more interesting to " + peerid);
                }

                if (abs.getType() == 4) {/*have*/
                    int index = byte2int(abs.getPayload());
                    a.updateBitfield(index);
                    System.out.println(peerid + " has part." + index);
                    if (a.isComplete()) {
                        completedLabel.set(peerid);
                    }
                    if (!checkneedbit(abs)) {
                        Message sendinterest = new Message(length, 2, null);
                        proc_sendmessage(a, sendinterest);
                    }
                }

                if (abs.getType() == 5) {/*bitfield*/
                    a.setBitfield(byte2bit(abs.getPayload()));
                    System.out.println(myid + " received bitfield from " + peerid);
                    System.out.println(myid + "'s bitfield: " + bitfield);
                    System.out.println(peerid + "'s bitfield: " + a.getBitfield());
                    if (checkinterest(a)) {
                        Message sendinterest = new Message(length, 2, null);
                        proc_sendmessage(a, sendinterest);
                    } else {
                        Message sendnotinterest = new Message(length, 3, null);
                        proc_sendmessage(a, sendnotinterest);
                    }
                }

                if (abs.getType() == 6) {/*request*/
                    System.out.println(myid + " received request from " + peerid);
                    byte[] payload_file = proc_sendpiece(abs);
                    Message piecemessage = new Message(length, 7, abs.getIndex(), payload_file);
                    proc_sendmessage(a, piecemessage);

                }

                if (abs.getType() == 7) {/*piece*/
                    System.out.println(myid + " received piece part." + abs.getIndex());
                    downloadprocess(abs);
                    bitfield.set(byte2int(abs.getIndex()));
                    Rate.put(peerid,Rate.get(peerid)+1);
                    int newneed = checkneed(a);
                    if (newneed>=0 & !chokeflag) {
                        Message sendrequest = new Message(length, 6, int2byte(newneed));
                        proc_sendmessage(a, sendrequest);
                        pieceRequested[newneed]=true;

                    }
                    else if(newneed<0) {
                        Message sendnotinterest = new Message(length, 3, null);
                        proc_sendmessage(a, sendnotinterest);
                    }

                    for (Neighbor x : neighborsInfo.values()) {//check all peer if interest
                        int newneed2=checkneed(x);
                        if (newneed2<0) {
                            Message sendnotinterest = new Message(length, 3, null);
                            proc_sendmessage(a, sendnotinterest);
                        }
                    }

                    Message havepiece = new Message(length, 4, abs.getIndex());
                    sendhavetoallpeer(havepiece);
                    if (bitfield.nextClearBit(0) >= npiece) {
                        completedLabel.set(myid);
                    }
                }
            }
        }
    }

    public void stopRunning() {
        running = false;
    }

}
