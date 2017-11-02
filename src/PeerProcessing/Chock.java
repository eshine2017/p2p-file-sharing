package PeerProcessing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Chock extends Thread {
    //value p in the description
    private int unchockInterval;
    //value m in the description
    private int optUnchockInterval;
    //constant decided at the beginning
    //value k in the description
    private int numOfPreferedNerghbor;
    private int numOfNeighbor;
    //waiting for others, not new actually
    private boolean[] isChock = new boolean[numOfNeighbor];
    //better to set as peer index
    private int optIndex = 0;

    //access to get variable
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

        return this.numOfNeighbor;
    }

    public int getOptIndex(){

        return this.optIndex;
    }

    public void run(){
        if(numOfNeighbor==0){
            System.out.println("peer connection fail");
            return;
        }
        if(numOfPreferedNerghbor==0){
            System.out.println("process may finished");
            return;
        }
        if(numOfPreferedNerghbor>numOfNeighbor){
            System.out.println("Check the process");
            return;
        }
        int count = 0;
        while(true){
            try{
                sleep(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            count++;
            //unchock Interval
            if(count%unchockInterval==0){
                //contain the index of neighbor interested in P
                List<Integer> preferNeighbor = maxRateNeighbor();
                if(preferNeighbor==null||preferNeighbor.size()==0){
                    System.out.println("unchock failed");
                    return;
                }
                //firstly chock all neighbor

                //choose high speed to unchock
                for(int i=0; i<numOfPreferedNerghbor; i++){
                    if(isChock[preferNeighbor.get(i)]){
                        //\\create unchock message
                        //\\send unchock to peer i;
                        //\\waiting for request message from i;
                        isChock[i] = false;
                        System.out.println("[" + count + "]" + "Peer unchock peer" + i);
                    }
                }
                //make sure optimistically unchock
                isChock[optIndex] = false;
                //make sure other peer chocked
                for(int i=0; i<isChock.length; i++){
                    if(!isChock[i]&&!preferNeighbor.contains(i)&&i!=optIndex){
                        isChock[i] = true;
                        //\\create chock message;
                        //\\send chock to peer i;
                    }
                }
            }

            //optimistically unchock interval
            if(count%optUnchockInterval==0){
                //actually not new, waiting for coming
                boolean[] curInterestedMe = new boolean[numOfNeighbor];
                if(curInterestedMe.length==0){
                    System.out.println("all other peers have my messages");
                    return;
                }
                //\\create chock message;
                //\\send chock to peer index;
                isChock[optIndex] = true;
                //\\send chock to peer optIndex;
                //List<Integer> preferNeighbor = maxRateNeighbor();
                for(int i=0; i<numOfNeighbor; i++){
                    //randomly unchock among currently chocked peer
                    int index = (int)(Math.random()*numOfNeighbor);
                    if(isChock[index]){
                        optIndex = index;
                        isChock[index] = false;
                        break;
                    }
                }
                //\\create unchock message;
                //\\send unchock to peer index;
                //\\waiting for request message from index;
                System.out.println("[" + count + "]" + "Peer optimistically unchock peer" + optIndex);
            }
        }
    }

    //find a list of high speed neighbor also interested at P
     private List<Integer> maxRateNeighbor(){
        //actually not new
        HashMap<Integer, Integer> curSendMeMsg = new HashMap<>();
        HashMap<Integer, Boolean> curInterestedMe = new HashMap<>();

        if(curSendMeMsg.size()==0){
            System.out.println("Peer not receiving message");
            return null;
        }

        if(curInterestedMe.size()==0){
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
                if(curSendMeMsg.get(k)==max){
                    Random random = new Random();
                    boolean b = random.nextBoolean();
                    if(b){
                        max = curSendMeMsg.get(k);
                        index = k;
                    }
                }
            }
            //if not interested in my message, not count in
            if(!curInterestedMe.get(index)){
                i--;
            }else {
                //choose as prefered
                maxRate.add(index);
                curSendMeMsg.put(index, 0);
            }
        }
        return maxRate;
    }
}
