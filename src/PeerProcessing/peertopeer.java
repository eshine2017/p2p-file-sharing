package PeerProcessing;

import java.io.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Object;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.math.BigInteger;
import Communication.*;

public class peertopeer{

    private ArrayList<String> bitfieldarray=new ArrayList<String>();

    private int filetotallength=100;

    private boolean need_flag=false;

    private boolean filecomplete_flag=false;

    private int peernumber=4;

    private ArrayList<Integer> interestlist=new ArrayList<Integer>();

    private int[] peergroup;

    private int[] allpeerid=new int[peernumber];

    private String filepath;

    private boolean checkhandshake(Handshake peerhandshake, int peerid)
    {
        boolean head_flag=false;
        boolean peerid_flag=false;
        String standardhead="P2PFILESHARINGPROJ";
        String header=peerhandshake.getHeader();
        int peeridhead=peerhandshake.getPeerID();
        if(header.equals(standardhead)){
            head_flag=true;
        }
        if(peeridhead==(peerid)){
            peerid_flag=true;
        }
        return head_flag&peerid_flag;
    }

    private void proc_sendbitfield(int peerid, Message sendbit){

        byte[] receive;
        receive=sendbit.getPayload();

    }

    private void savebitfield(int peerid,Message peer){

        byte[] bitfield;
        bitfield=peer.getPayload();
        String bitfieldpeer=new String(bitfield);
        bitfieldarray.add(bitfieldpeer);
        int peerposition=bitfieldarray.indexOf(bitfieldpeer);
        peergroup[peerid]=peerposition;
    }

    private String getbitfield(int id){

        String bitfield;
        return bitfield=bitfieldarray.get(peergroup[id]);

    }

    private int checkneed(int peerid,int myid,int filetotallength)/*check unchoke*/
    {
        String bitfieldpeer=getbitfield(peerid);
        String bitfieldme=getbitfield(myid);
        int ipeer = Integer.parseInt(bitfieldpeer);
        int ime = Integer.parseInt(bitfieldme);
        int a=ipeer^ime;
        int b=a&ime;
        String s;
        s=String.valueOf(b);
        int n=1;
        int[] intArray0=new int[filetotallength];
        for(int i=0;i<filetotallength;i++){
            if(s.charAt(i)==0){
                intArray0[n]=i;
                n=n+1;
                need_flag=true;
            }
        }
        Random indexselect = new Random();
        int h;
        h=indexselect.nextInt(n);
        return intArray0[h];
    }

    private byte[] int2byte( final int i ) {

        ByteBuffer settobyte = ByteBuffer.allocate(4);
        settobyte.putInt(i);
        return settobyte.array();

    }

    private int byte2int(byte[] b1){
        return new BigInteger(b1).intValue();
    }

    private void proc_sendmessage(int peerid, Message sendmessage){

        Message receive;
        receive=sendmessage;

    }

    private boolean checkneedbit(Message peerhave,int myid)/*check have*/
    {
        boolean needbit_flag=false;
        byte[] bitindex=peerhave.getPayload();
        int bitindexpeer=byte2int(bitindex);
        String bitfieldme=getbitfield(myid);
        if(bitfieldme.charAt(bitindexpeer)==0)
        {
            needbit_flag=true;
        }
        return needbit_flag;
    }

    private void updatemybitfield(int myid,Message peerindex){
        int index=byte2int(peerindex.getIndex());
        String mybitfield=getbitfield(myid);
        StringBuilder newbitfield = new StringBuilder(mybitfield);
        newbitfield.setCharAt(index, '1');
        mybitfield=newbitfield.toString();
        bitfieldarray.add(mybitfield);
        int peerposition=bitfieldarray.indexOf(mybitfield);
        peergroup[myid]=peerposition;
    }

    private boolean checkinterest(Message peerbitfield,int myid,int filetotallength)/*check bitfield*/
    {
        byte[] bitfieldpeer=peerbitfield.getPayload();
        String bitfieldme=getbitfield(myid);
        int ipeer = byte2int(bitfieldpeer);
        int ime = Integer.parseInt(bitfieldme);
        int a=ipeer^ime;
        int b=a&ime;
        String s;
        s=String.valueOf(b);
        int n=1;
        int[] intArray0=new int[filetotallength];
        for(int i=0;i<filetotallength;i++){
            if(s.charAt(i)==0){
                need_flag=true;
            }
        }
        return need_flag;
    }

    private void proc_sendpiece(int peerid, Message sendmessage){

        Message receive;
        receive=sendmessage;

    }

    private void downloadprocess(Message piecemessage){
        int index=byte2int(piecemessage.getIndex());
        try {
            File file = new File(filepath+"part"+index);
            FileOutputStream out;
            out = new FileOutputStream(file);
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            objOut.writeObject(piecemessage.getPayload());
            objOut.flush();
            objOut.close();

        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void  sendhavetoallpeer(Message havepiece){
        for(int w=1;w<peernumber;w++){
            proc_sendmessage(allpeerid[w],havepiece);
        }
    }

    private void  checkifcomplete(int myid, int firstserverid){
        String bitfieldserver=getbitfield(firstserverid);
        String bitfieldme=getbitfield(myid);
        int ipeer = Integer.parseInt(bitfieldserver);
        int ime = Integer.parseInt(bitfieldme);
        int result=ipeer&ime;
        String s;
        s=String.valueOf(result);
        int n=1;
        int flag=0;
        int[] intArrayti=new int[filetotallength];
        for(int i=0;i<filetotallength;i++){
            if(s.charAt(i)==0){
                flag++;
            }
        }
        if(flag==0){
            filecomplete_flag=true;
        }
    }



    public void run(){

        int peerid=0;
        int myid=1;
        int firstserverid=2;
        int length=10;
        byte[] payload={1,1,1};
        byte[] payload_file={1,1,1,1,1};
        int choke=0;
        String t="testhandshake";
        Handshake receivemessage1= new Handshake(t,peerid);
        Message receivemessage=new Message(10,1,payload);

        if(receivemessage1 instanceof Handshake)
        {
            if(checkhandshake(receivemessage1,peerid)){
                Message sendbitfield= new Message(length,5,payload);
                proc_sendbitfield(peerid,sendbitfield);

            }
        }

        if(receivemessage instanceof Message){
            if(receivemessage.getType()==0){
                choke=1;
                /*choke*/
            }

            if(receivemessage.getType()==1){ /*unchoke*/
                int indexneed=checkneed(peerid,myid,filetotallength);
                byte[] indexbyte=int2byte(indexneed);
                if(need_flag=true){
                    Message sendrequest= new Message(length,6,indexbyte);
                    need_flag=false;
                    proc_sendmessage(peerid,sendrequest);  /*send request*/
                }
            }

            if(receivemessage.getType()==2){
                interestlist.add(peerid); /*interest*/
            }

            if(receivemessage.getType()==3){
                interestlist.remove(peerid) ;/*not interest*/
            }

            if(receivemessage.getType()==4){/*have*/
                if(checkneedbit(receivemessage,myid)){
                    Message sendinterest= new Message(length,2,null);
                    proc_sendmessage(peerid,sendinterest );
                }
            }

            if(receivemessage.getType()==5){/*bitfield*/
                savebitfield(peerid,receivemessage);
                if(checkinterest(receivemessage,myid,filetotallength)){
                    Message sendinterest= new Message(length,2,null);
                    proc_sendmessage(peerid,sendinterest );
                    need_flag=false;
                }
                else{
                    Message sendnotinterest= new Message(length,3,null);
                    proc_sendmessage(peerid,sendnotinterest );
                }
            }

            if(receivemessage.getType()==6){/*request*/
                Message piecemeaasge= new Message(length,7,receivemessage.getIndex(),payload_file);
                proc_sendmessage(peerid,piecemeaasge);

            }

            if(receivemessage.getType()==7){/*piece*/
                downloadprocess(receivemessage);
                updatemybitfield(myid,receivemessage);
                int newneed=checkneed(peerid,myid,filetotallength);
                if(need_flag=true){
                    Message sendrequest= new Message(length,6,int2byte(newneed));
                    need_flag=false;
                    proc_sendmessage(peerid,sendrequest);
                }
                else{
                    Message sendnotinterest= new Message(length,3,null);
                    proc_sendmessage(peerid,sendnotinterest);
                }
                Message havepiece=new Message(length,4,receivemessage.getIndex());
                sendhavetoallpeer(havepiece);
                checkifcomplete(myid,firstserverid);
            }
        }
    }
}
