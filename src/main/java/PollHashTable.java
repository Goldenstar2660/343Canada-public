import java.util.ArrayList;

public class PollHashTable {
    public ArrayList<Poll>[] buckets;
    public int numInHT = 0;
    
    // Constructor
    public PollHashTable (int numBuckets) {
        buckets = new ArrayList[numBuckets];

        for (int i = 0; i < numBuckets; i++) {
                buckets[i] = new ArrayList<Poll>();  // Instantiate the ArrayList for bucket i.
        }
    }
    
    // Methods
    public int getNumInHT() {
        return numInHT;
    }
    
    public boolean add(Poll thePoll) {
        if (thePoll == null) {
                return(false);
        }
        int targetBucket = thePoll.region.bucket;
        boolean addStatus = buckets[targetBucket].add(thePoll);
        numInHT++;
        return addStatus;
    }
    
    public void displayContents() {
        System.out.println("\n\nDISPLAYING THE CONTENTS OF A HASH TABLE!");

        for (int i = 0; i < buckets.length; i++) {
            System.out.println("\nExamining the ArrayList for bucket " + i);
            int listSize = buckets[i].size();
            if (listSize == 0) {
                System.out.println("Nothing in its ArrayList!");
            }
            else {
                for (int j = 0; j < listSize; j++) {
                    Poll poll = buckets[i].get(j);
                    System.out.println(poll.getID() + " " + poll.getPollsterName() + " " + poll.getDate() + " " + poll.getResults());
                }
            }
        }
    }

    public void removeAll() {
        for (int i = 0; i < buckets.length; i++) {
            for (int j = 0; j < buckets[i].size(); j++) {
                buckets[i].clear();
                numInHT -= buckets[i].size();
            }
        }
    } 

    public void remove(String pollID) {
        for (int i = 0; i < buckets.length; i++) {
            for (int j = 0; j < buckets[i].size(); j++) {
                Poll poll = buckets[i].get(j);
                if (poll.getID().equals(pollID)) {
                    buckets[i].remove(j);
                    numInHT--;
                }
            }
        }
    }
    
    public void removeAll(String pollID) {
        String targetPollID = null;
        for (int i = 0; i < buckets.length; i++) {
            for (int j = 0; j < buckets[i].size(); j++) {
                Poll poll = buckets[i].get(j);
                String pollIDRegionless = poll.pollster.getFullName() + poll.date.toString() + Integer.toString(poll.sampleSize);
                if (poll.getID().equals(pollID) || pollIDRegionless.equals(targetPollID)) {
                    targetPollID = poll.pollster.getFullName() + poll.date.toString() + Integer.toString(poll.sampleSize);
                    buckets[i].remove(j);
                    numInHT--;
                }
            }
        }
    }

    // Currently returns just the first poll
    public Poll get(String pollID) {
        for (int i = 0; i < buckets.length; i++) {
            for (int j = 0; j < buckets[i].size(); j++) {
                Poll poll = buckets[i].get(j);
                if (poll.getID().equals(pollID)) {
                    return buckets[i].get(j);
                }
            }
        }
        return null;
    }
}
