package FileSharing;

import Communication.Message;

import java.io.*;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Chock extends Thread {

    private volatile boolean running; // running control flag
    //ObjectOutputStream out;
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
    private ConcurrentHashMap<Integer, Integer> curSendMeMsg;
    private boolean[] curInterestedMe;
    private ConcurrentHashMap<Integer, Neighbor> neighborsInfo;
    private BitSet bitfield;
    private int ID_me;
    private String filePath;

    public Chock(ConcurrentHashMap<Integer,Integer> curSendMeMsg, boolean[] curInterestedMe, Common x,
                 ConcurrentHashMap<Integer, Neighbor> neighborsInfo, BitSet bitfield, int ID_me) {
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
        this.ID_me = ID_me;
        filePath = System.getProperty("user.dir") + File.separator
                + "peer_" + ID_me + File.separator;
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
        List<Integer> preRandomNeighbor = new ArrayList<>();
        while(running){
            try{
                sleep(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            count++;
            //unchock Interval
            if(count%unchockInterval==0){
                int logFlag = 0;
                //contain the index of neighbor interested in P
                preferNeighbor = maxRateNeighbor();
                System.out.println(preferNeighbor);

//                if(preferNeighbor==null||preferNeighbor.size()==0){
//                    System.out.println("unchoke failed");
//                }

                //make sure optimistically unchock
                //isChock[optIndex] = false;
                //make sure other peer chocked
                boolean flag0;
                synchronized (bitfield) {
                    flag0 = bitfield.nextClearBit(0) >= numOfPeers;
                }
                if(flag0) {   //receive all file parts

//                    for(int i=0; i<isChock.length; i++){   //if receive all files, do not need to consider preferNeighbor
//                        if(!isChock[i] && i!=optIndex){
//                            isChock[i] = true;
//                            //create chock message;
//                            Message msg = new Message(1, 0, null);
//                            //send chock to peer i;
//                            sendMessage(msg, i);
//                            System.out.println("[" + count + "]" + "Peer choke peer " + i);
//                            logFlag = 1;
//                        }
//                    }

                    int m = 0;
                    List<Integer> interestedMe = new ArrayList<>();
                    for(int i=0; i<curInterestedMe.length; i++) {
                        if(curInterestedMe[i]) {
                            interestedMe.add(i);
                        }
                    }
                    List<Integer> randomNeighbor = new ArrayList<>();
                    while(!interestedMe.isEmpty() && m < numOfPreferedNerghbor) {   //use a list to save the peer interested in me
                        int i = (int)(Math.random()*interestedMe.size());
                        int index = interestedMe.get(i);
                        if(isChock[index]) {
                            interestedMe.remove(i);
                            randomNeighbor.add(index);
                            m++;
                        }
                    }

                    for(int i : preRandomNeighbor) {  //chose previous but not choose now -- choke
                        if(!randomNeighbor.contains(i) && i!=optIndex) {
                            isChock[i] = true;
                            //create chock message;
                            Message msg = new Message(1, 0, null);
                            //send chock to peer i;
                            sendMessage(msg, i);
                            System.out.println("[" + count + "]" + "Peer choke peer " + i);
                            logFlag = 1;
                        }
                    }

                    for(int i : randomNeighbor) {  //choose now but not chose previous -- unchoke
                        if(!preRandomNeighbor.contains(i) && i!=optIndex) {
                            isChock[i] = false;
                            Message msg = new Message(1, 1, null);
                            sendMessage(msg, i);
                            System.out.println("[" + count + "]" + "Peer unchoke peer" + i);
                            logFlag = 1;
                        }
                    }

                    if(logFlag == 1) {
                        List<String> unchokeList = new ArrayList<>();
//                    for(int i=0; i<isChock.length; i++) {
//                        if(!isChock[i] && ((i!=optIndex) ||(i==optIndex && preferNeighbor.contains(i)))) {
//                            unchokeList.add("" + neighborsInfo.get(i).getPeerID());
//                        }
//                    }
                        if(!randomNeighbor.isEmpty()) {
                            for (int i : randomNeighbor) {
                                unchokeList.add("" + neighborsInfo.get(i).getPeerID());
                            }
                        }
                        writelog("Peer " + ID_me + " has preferred neighbors " + unchokeList);
                    }

                    preRandomNeighbor.clear();
                    preRandomNeighbor.addAll(randomNeighbor);
                }
                else {

                    for(int i=0; i<isChock.length; i++){
                        if(!isChock[i] && !preferNeighbor.contains(i) && i!=optIndex){
                            isChock[i] = true;
                            //create chock message;
                            Message msg = new Message(1, 0, null);
                            //send chock to peer i;
                            sendMessage(msg, i);
                            System.out.println("[" + count + "]" + "Peer choke peer " + i);
                            logFlag = 1;
                        }
                    }

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
                            System.out.println("[" + count + "]" + "Peer unchoke peer" + index);
                            logFlag = 1;
                        }
                    }

                    if(logFlag == 1) {
                        List<String> unchokeList = new ArrayList<>();
//                    for(int i=0; i<isChock.length; i++) {
//                        if(!isChock[i] && ((i!=optIndex) ||(i==optIndex && preferNeighbor.contains(i)))) {
//                            unchokeList.add("" + neighborsInfo.get(i).getPeerID());
//                        }
//                    }
                        if(!preferNeighbor.isEmpty()) {
                            for (int i : preferNeighbor) {
                                unchokeList.add("" + neighborsInfo.get(i).getPeerID());
                            }
                        }
                        writelog("Peer " + ID_me + " has preferred neighbors " + unchokeList);
                    }
                }


            }

            //optimistically unchock interval
            //&& preferNeighbor.size()>=numOfPreferedNerghbor
            if(count%optUnchockInterval==0){
                //actually not new, waiting for coming
                //boolean[] curInterestedMe = new boolean[numOfNeighbor];
                if(curInterestedMe.length==0){
                    System.out.println("all other peers have my messages");
                    //return;
                }

                //List<Integer> preferNeighbor = maxRateNeighbor();

                if(optIndex!=-1 && preferNeighbor!=null&&!preferNeighbor.contains(optIndex) && !isChock[optIndex]) {
                    //create chock message;
                    Message msg = new Message(1, 0, null);
                    //send chock to peer index;
                    sendMessage(msg, optIndex);
                    isChock[optIndex] = true;
                    System.out.println("[" + count + "]" + "Peer choke peer " + optIndex);
                }
                //\\send chock to peer optIndex;
                //add all satisfactory peers into a list
                List<Integer> optChooseList = new ArrayList<>();
                for(int i=0; i<numOfPeers; i++){
                    if(isChock[i] && curInterestedMe[i]) {
                        optChooseList.add(i);
                    }
                }
                //randomly unchock among currently chocked peer
                if(!optChooseList.isEmpty()) {  //if optChooseList is empty, do not need to choose opt
                    int i = (int) (Math.random() * optChooseList.size());
                    if(optIndex!=optChooseList.get(i)) {
                        optIndex = optChooseList.get(i);
                        Message msg = new Message(1, 1, null);
                        sendMessage(msg, optIndex);
                        isChock[optIndex] = false;

                        writelog("Peer " + ID_me + " has the optimistically unchoked neighbor " +
                                neighborsInfo.get(optIndex).getPeerID());

                        System.out.println("[" + count + "]" + "Peer optimistically unchoke peer" + optIndex);
                    }
                }
            }
        }
//        System.out.println("Choke thread is stopped.");
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
        Queue<Pair> maxRate = new PriorityQueue<>();  ///what about the equal number??
        for (int index : curSendMeMsg.keySet())
            System.out.println("current sending me message: " + index);
        for (int k : curSendMeMsg.keySet()) {
            if(curInterestedMe[k]) {
                maxRate.offer(new Pair(curSendMeMsg.get(k), k));
            }
        }
        while (i < numOfPreferedNerghbor && !maxRate.isEmpty()) {
            ReMaxRate.add(maxRate.poll().index);
            i++;
        }
        System.out.println("my reMaxRate is " + ReMaxRate);
        return ReMaxRate;
    }

    private void sendMessage(Message msg, int i)
    {
        ObjectOutputStream out = neighborsInfo.get(i).out;
        try{
            synchronized (out) {
                //stream write the message
                out.writeObject(msg);
                out.flush();
            }

        } catch (SocketException e) {
            // this is fine
        } catch (EOFException e) {
            // also fine
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    private void writelog(String log){

        String filePath = System.getProperty("user.dir") + File.separator;
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
            if(that.rate != this.rate) return that.rate - this.rate;
            else {
                Random random = new Random();
                boolean b = random.nextBoolean();
                if(b) return 1;
                else return -1;
            }
        }
    }

}
