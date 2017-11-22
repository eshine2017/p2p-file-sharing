import java.util.PriorityQueue;

public class Test {

    /* nested pair (transferRate, peerIndex) class */
    private static class Pair implements Comparable<Pair> {
        int rate;
        int index;
        Pair(int rate, int index) {
            this.rate = rate;
            this.index = index;
        }

        @Override
        // sort sequence: high rate pair first
        public int compareTo(Pair that) {
            return that.rate - this.rate;
        }

        @Override
        public String toString() {
            return "index: " + index + "; rate: " + rate;
        }
    }

    public static void main(String[] args) {
        PriorityQueue<Pair> pq= new PriorityQueue<>();
        pq.offer(new Pair(3,0));
        pq.offer(new Pair(1,1));
        pq.offer(new Pair(2,2));
        pq.offer(new Pair(7,3));

        while (!pq.isEmpty()) System.out.println(pq.poll());
    }

}
