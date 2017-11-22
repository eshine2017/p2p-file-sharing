package FileSharing;

import Communication.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.*;

public class Chock extends Thread {

    private volatile boolean running; // running control flag
    ObjectOutputStream out;
    //value p in the description
    private int unchockInterval;
    //value m in the description
    private int optUnchockInterval;
    //constant decided at the beginning
    //value k in the description
    private int numOfPreferedNerghbor;
    private int numOfPeers;   //num of peers
    //waiting for others, not new actually
    private boolean[] isChock;  // = new boolean[numOfNeighbor];
    //better to set as peer index
    private int optIndex = -1;
    //access to get variable
    private HashMap<Integer, Integer> curSendMeMsg;
    private boolean[] curInterestedMe;
    private HashMap<Integer, Neighbor> neighborsInfo;
    private BitSet bitfield;

    public Chock(HashMap<Integer,Integer> curSendMeMsg, boolean[] curInterestedMe, Common x,
                 HashMap neighborsInfo, BitSet bitfield) {
        // bitfield.nextClearBit(0) >= nPieces
        this.numOfPeers = curInterestedMe.length;
        isChock = new boolean[numOfPeers];
        for(int i = 0; i<isChock.length; i++) {
            isChock[i] = true;
        }
        this.curSendMeMsg = curSendMeMsg;
        this.curInterestedMe = curInterestedMe;
        this.unchockInterval = x.UnchokingInterval;
        this.optUnchockInterval = x.OptimisticUnchokingInterval;
        this.numOfPreferedNerghbor = x.NumberOfPreferredNeighbors;
        this.neighborsInfo = neighborsInfo;
        this.bitfield = bitfield;
        running = true;
    }
    public int getUnchockInterval(){

        return this.unchockInterval;
    }

    public  int getOptUnchockInterval(){

        return this.optUnchockInterval;
    }

    public int getNumOfPreferedNerghbor(){

        return this.numOfPreferedNerghbor;
    }

    public int getNumOfNeighbor(){

        return this.numOfPeers;
    }

    public int getOptIndex(){

        return this.optIndex;
    }

    public void run(){
        if(numOfPeers==0){
            System.out.println("peer connection fail");
            //return;
        }
        if(numOfPreferedNerghbor==0){
            System.out.println("process may finished");
            //return;
        }
        if(numOfPreferedNerghbor>numOfPeers){
            System.out.println("Check the process");
            //return;
        }
        int count = 1;
        List<Integer> preferNeighbor = new LinkedList<>();
        while(running){
            try{
                sleep(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            count++;
            //unchock Interval
            if(count%unchockInterval==0){
                //contain the index of neighbor interested in P
                preferNeighbor = maxRateNeighbor();
                System.out.println(preferNeighbor);

                if(preferNeighbor==null||preferNeighbor.size()==0){
                    System.out.println("unchock failed");
                }

                //make sure optimistically unchock
                //isChock[optIndex] = false;
                //make sure other peer chocked
                for(int i=0; i<isChock.length; i++){
                    if(!isChock[i]&&!preferNeighbor.contains(i)&&i!=optIndex){
                        isChock[i] = true;
                        //create chock message;
                        Message msg = new Message(1, 0, null);
                        //send chock to peer i;
                        sendMessage(msg, i);
                        System.out.println("[" + count + "]" + "Peer chock peer " + i);
                    }
                }

                if(bitfield.nextClearBit(0) >= numOfPeers) {
                    int m = 0;
                    while(m < preferNeighbor.size() && m < numOfPreferedNerghbor) {
                        int index = (int)(Math.random()*numOfPeers);
                        if(curInterestedMe[index]&&isChock[index]) {
                            Message msg = new Message(1, 1, null);
                            sendMessage(msg, index);
                            isChock[index] = false;
                            System.out.println("[" + count + "]" + "Peer unchock peer" + index);
                            m++;
                        }
                    }
                }
                else {
                    //choose high speed to unchock
                    for (int i = 0; i < numOfPreferedNerghbor && i < preferNeighbor.size(); i++) {
                        int index = preferNeighbor.get(i);
                        if (isChock[index]) {
                            //create unchock message
                            Message msg = new Message(1, 1, null);
                            //send unchock to peer i;
                            sendMessage(msg, index);
                            //\\waiting for request message from i;
                            isChock[index] = false;
                            System.out.println("[" + count + "]" + "Peer unchock peer" + index);
                        }
                    }
                }
            }

            //optimistically unchock interval
            if(count%optUnchockInterval==0 && preferNeighbor.size()>numOfPreferedNerghbor){
                //actually not new, waiting for coming
                //boolean[] curInterestedMe = new boolean[numOfNeighbor];
                if(curInterestedMe.length==0){
                    System.out.println("all other peers have my messages");
                    //return;
                }

                //List<Integer> preferNeighbor = maxRateNeighbor();

                if(preferNeighbor!=null&&!preferNeighbor.contains(optIndex)) {
                    //create chock message;
                    Message msg = new Message(1, 0, null);
                    //send chock to peer index;
                    sendMessage(msg, optIndex);
                    isChock[optIndex] = true;
                    System.out.println("[" + count + "]" + "Peer chock peer " + optIndex);
                }
                //\\send chock to peer optIndex;
                //List<Integer> preferNeighbor = maxRateNeighbor();
                for(int i=0; ; i++){
                    //randomly unchock among currently chocked peer
                    int index = (int)(Math.random()*numOfPeers);
                    if(isChock[index] && curInterestedMe[index]){
                        optIndex = index;
                        Message msg = new Message(1, 1, null);
                        sendMessage(msg, optIndex);
                        isChock[optIndex] = false;
                        break;
                    }
                }
                //\\create unchock message;
                //\\send unchock to peer index;
                //\\waiting for request message from index;
                System.out.println("[" + count + "]" + "Peer optimistically unchock peer" + optIndex);
            }
        }
        System.out.println("Choke thread is stopped.");
    }

    //find a list of high speed neighbor also interested at P
    /* private List<Integer> maxRateNeighbor(){
        //actually not new
        //curSendMeMsg = new HashMap<>();
        //HashMap<Integer, Boolean> curInterestedMe = new HashMap<>();
         //curInterestedMe = new boolean[numOfNeighbor];

        if(curSendMeMsg.size()==0){
            System.out.println("Peer not receiving message");
            return null;
        }

        if(curInterestedMe.length==0){
            System.out.println("all other peers have my messages");
            return null;
        }
        List<Integer> maxRate = new LinkedList<>();
        //choose limit number of peer
        for(int i=0; i<numOfPreferedNerghbor; i++){
            //initial
            int max = 0;
            int index = 0;
            //get index of peers currently send me message
            for(int k : curSendMeMsg.keySet()){
                //compare rate with max
                if(curSendMeMsg.get(k)>max){
                    max = curSendMeMsg.get(k);
                    index = k;
                }
                //for same rate randomly choose one
                else if(curSendMeMsg.get(k)==max){
                    Random random = new Random();
                    boolean b = random.nextBoolean();
                    if(b){
                        max = curSendMeMsg.get(k);
                        index = k;
                    }
                }
            }
            //if not interested in my message, not count in
            if(!curInterestedMe[index]){
                curSendMeMsg.put(index, 0);
                i--;
            }else {
                //choose as prefered
                maxRate.add(index);
                curSendMeMsg.put(index, 0);
            }
        }
        return maxRate;
    }*/

    private List<Integer> maxRateNeighbor() {
        List<Integer> ReMaxRate = new LinkedList<>();
        int i = 0;
        Queue<Pair> maxRate = new PriorityQueue<>();
        for (int k : curSendMeMsg.keySet()) {
            if(curInterestedMe[k]) {
                maxRate.offer(new Pair(curSendMeMsg.get(k), k));
            }
        }
        while (i < numOfPreferedNerghbor && !maxRate.isEmpty()) {
            ReMaxRate.add(maxRate.poll().index);
            i++;
        }
        return ReMaxRate;
    }

    private void sendMessage(Message msg, int i)
    {
        try{
            //stream write the message
            neighborsInfo.get(i).out.writeObject(msg);
            neighborsInfo.get(i).out.flush();
        } catch (SocketException e) {
            // this is fine
        } catch (EOFException e) {
            // also fine
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    public void stopRunning() {
        running = false;
    }

    private class Pair implements Comparable<Pair> {
        int rate;
        int index;
        public Pair(int rate, int index) {
            this.rate = rate;
            this.index = index;
        }

        @Override
        // sort sequence: high rate pair first
        public int compareTo(Pair that) {
            return that.rate - this.rate;
        }
    }

}
