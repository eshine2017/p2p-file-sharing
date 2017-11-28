import java.util.PriorityQueue;
import java.util.*;
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
        ArrayList<Integer> test = new ArrayList<>();
        test.add(0);
        test.add(1);
        test.add(2);
        System.out.println(test.size());
        test.remove(1);
        System.out.println(test.size());
        System.out.println(test.get(0));
        System.out.println(test.get(1));
    }

}
