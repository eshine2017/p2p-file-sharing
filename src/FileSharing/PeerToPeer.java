package FileSharing;

import java.io.*;
import java.io.Serializable;
import java.util.*;
import java.lang.Object;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.math.BigInteger;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.File;
import java.io.FileOutputStream;
import Communication.*;

public class PeerToPeer {

    //private ArrayList<String> bitfieldarray = new ArrayList<String>();

    private boolean need_flag = false;

    //private ArrayList<Integer> interestlist = new ArrayList<Integer>();//need to change

    //private ArrayList<Integer> peerlist = new ArrayList<Integer>();//need to change

    private int peerid;

    private int myid;

    private boolean handshake_flag;

    private HashMap<Integer, Neighbor> neighborsInfo;

    private int peernumber;

    private int[] completeflag = new int[peernumber];

    private int FileSize;

    private int PieceSize;

    private double npieced = Math.ceil(FileSize / PieceSize);

    private int npiece = (int) npieced;

    private String Filename;

    public PeerToPeer(HashMap<Integer, Neighbor> neighborsInfo, int index_peer, int index_me, int ID_me, BitSet bitfield, BitSet completedLabel, boolean[] isIntersetedOnMe, boolean handshake_flag, int peernumber, Common common) {
        this.neighborsInfo = neighborsInfo;
        // neighborsInfo.get(index).in or .out
        this.peerid = index_peer;
        this.myid = index_me;
        this.handshake_flag = handshake_flag;//true=client
        this.peernumber = peernumber;
        this.FileSize = common.FileSize;
        this.PieceSize = common.PieceSize;
        this.Filename = common.FileName;
    }

    private void createsendhandshake(Neighbor a) {
        String standardhead = "P2PFILESHARINGPROJ";
        byte[] zeroBits = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Handshake message = new Handshake(standardhead, zeroBits, a.peerID);
        try {
            //stream write the message
            a.out.writeObject(message);
            a.out.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private boolean checkhandshake(Handshake peerhandshake, int peerid) {
        boolean head_flag = false;
        boolean peerid_flag = false;
        String standardhead = "P2PFILESHARINGPROJ";
        String header = peerhandshake.getHeader();
        int peeridhead = peerhandshake.getPeerID();
        if (header.equals(standardhead)) {
            head_flag = true;
        }
        if (peeridhead == (peerid)) {
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

    private int checkneed(Neighbor peer, Neighbor me)/*check unchoke*/ {
        byte[] bitfieldpeer = peer.getBitfield();
        byte[] bitfieldme = me.getBitfield();
        int ipeer = byte2int(bitfieldpeer);
        int ime = byte2int(bitfieldme);
        int a = ipeer ^ ime;
        int b = a ^ ime;
        int c = b & a;
        String s;
        s = String.valueOf(c);
        while (s.length() < npiece) {
            s = "0" + s; // fill 0
        }
        int n = 1;
        int length = npiece;
        int[] intArray0 = new int[length];
        for (int i = 0; i < length; i++) {
            if (s.charAt(i) == 1) {
                intArray0[n] = i;
                n = n + 1;
                need_flag = true;
            }
        }
        Random indexselect = new Random();
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

    private boolean checkneedbit(Message peerhave, Neighbor b)/*check have*/ {
        byte[] bitindex = peerhave.getPayload();
        int bitindexpeer = byte2int(bitindex);
        return b.checkbitfield(bitindexpeer);
    }

    private void updatebitfield(Neighbor b, Message peerindex) {
        int index = byte2int(peerindex.getIndex());
        b.updateBitfield(index);
    }

    private boolean checkinterest(Neighbor peer, Neighbor me)/*check bitfield*/ {
        need_flag = false;
        byte[] bitfieldpeer = peer.getBitfield();
        byte[] bitfieldme = me.getBitfield();
        int ipeer = byte2int(bitfieldpeer);
        int ime = byte2int(bitfieldme);
        int a = ipeer ^ ime;
        int b = a ^ ime;
        int c = b & a;
        String s;
        s = String.valueOf(c);
        while (s.length() < npiece) {
            s = "0" + s; // fill 0
        }
        int n = 1;
        int length = npiece;
        for (int i = 0; i < length; i++) {
            if (s.charAt(i) == 1) {
                need_flag = true;
            }
        }
        return need_flag;
    }

    private byte[] proc_sendpiece(Message sendmessage) {

        int index = byte2int(sendmessage.getIndex());
        index = index + 1;//string start from 0, fileindex start from 1
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
            in.close();
            ;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return what;
    }

    private void downloadprocess(Message piecemessage) {
        int index = byte2int(piecemessage.getIndex());
        index = index + 1;//string start from 0, fileindex start from 1
        try {
            //File file = new File(index+".part");
            //FileOutputStream out = new FileOutputStream(file);
            FileOutputStream out = new FileOutputStream(index + ".part");
            out.write(piecemessage.getPayload());
            out.flush();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendhavetoallpeer(Message havepiece) {
        for (Neighbor x : neighborsInfo.values()) {
            proc_sendmessage(x, havepiece);
        }
    }

    private boolean checkifcomplete(Neighbor b) {
        boolean filecomplete_flag = false;
        byte[] bitfield = b.getBitfield();
        int result = byte2int(bitfield);
        String s;
        s = String.valueOf(result);
        while (s.length() < npiece) {
            s = "0" + s; // fill 0
        }
        int length = npiece;
        int flag = 0;
        for (int i = 0; i < length; i++) {
            if (s.charAt(i) == 0) {
                flag++;
            }
        }
        if (flag == 0) {
            filecomplete_flag = true;
        }
        return filecomplete_flag;
    }

    private boolean checkfinish(int[] completeflag) {
        boolean flag = true;
        for (int i = 0; i < peernumber; i++) {
            if (completeflag[i] == 0) {
                flag = false;
            }
        }
        return flag;
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

        int choke = 0;
        int length = 10;

        Neighbor a = neighborsInfo.get(peerid);
        Neighbor b = neighborsInfo.get(myid);

        while (true) {
            Object receivemessage = null;
            try {
                receivemessage = a.in.readObject();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (ClassNotFoundException whatever) {
                whatever.printStackTrace();
            } finally {
                //Close connections
                try {
                    a.in.close();
                    a.out.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

            if (handshake_flag) {
                createsendhandshake(a);
                if (receivemessage instanceof Handshake) {
                    //String statushand=new Date().getTime()+": Peer "+peerid+"makes a connection to Peer "+myid;
                    if (checkhandshake((Handshake) receivemessage, a.peerID)) {
                        Message sendbitfield = new Message(length, 5, b.getBitfield());
                        proc_sendmessage(a, sendbitfield);

                    }
                }
            } else {
                if (receivemessage instanceof Handshake) {
                    a.setPeerID(((Handshake) receivemessage).getPeerID());
                    if (checkhandshake((Handshake) receivemessage, a.peerID)) {
                        createsendhandshake(a);
                    }
                }
            }

            if (receivemessage instanceof Message) {
                Message abs = (Message) receivemessage;
                if (abs.getType() == 0) {
                    choke = 1;
                    peerlist.remove(peerid);
                /*choke*/
                }

                if (abs.getType() == 1) { /*unchoke*/
                    int indexneed = checkneed(a, b);
                    byte[] indexbyte = int2byte(indexneed);
                    if (need_flag = true) {
                        Message sendrequest = new Message(length, 6, indexbyte);
                        need_flag = false;
                        proc_sendmessage(a, sendrequest);  /*send request*/
                    }
                }

                if (abs.getType() == 2) {
                    interestlist.add(peerid); /*interest*/
                }

                if (abs.getType() == 3) {
                    interestlist.remove(peerid);/*not interest*/
                }

                if (abs.getType() == 4) {/*have*/
                    updatebitfield(a, abs);
                    if (checkifcomplete(a)) {
                        completeflag[a.index] = 1;
                    }
                    if (checkneedbit(abs, b)) {
                        Message sendinterest = new Message(length, 2, null);
                        proc_sendmessage(a, sendinterest);
                    }
                    checkfinish(completeflag);
                }

                if (abs.getType() == 5) {/*bitfield*/
                    a.setBitfield(abs.getPayload());
                    if (checkinterest(a, b)) {
                        Message sendinterest = new Message(length, 2, null);
                        proc_sendmessage(a, sendinterest);
                    } else {
                        Message sendnotinterest = new Message(length, 3, null);
                        proc_sendmessage(a, sendnotinterest);
                    }
                }

                if (abs.getType() == 6) {/*request*/
                    byte[] payload_file = proc_sendpiece(abs);
                    Message piecemessage = new Message(length, 7, abs.getIndex(), payload_file);
                    proc_sendmessage(a, piecemessage);

                }

                if (abs.getType() == 7) {/*piece*/
                    peerlist.add(peerid);
                    downloadprocess(abs);
                    updatebitfield(b, abs);
                    int newneed = checkneed(a, b);
                    if (need_flag = true) {
                        Message sendrequest = new Message(length, 6, int2byte(newneed));
                        need_flag = false;
                        proc_sendmessage(a, sendrequest);
                    } else {
                        Message sendnotinterest = new Message(length, 3, null);
                        proc_sendmessage(a, sendnotinterest);
                    }
                    Message havepiece = new Message(length, 4, abs.getIndex());
                    sendhavetoallpeer(havepiece);
                    if (checkifcomplete(b)) {
                        completeflag[b.index] = 1;
                        //mergefile();
                    }
                    checkfinish(completeflag);
                }
            }
        }
    }
}
