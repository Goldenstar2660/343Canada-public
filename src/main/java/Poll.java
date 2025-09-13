import java.time.LocalDate;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Poll {
    // Attributes
    @JsonIgnore public String pollID;
    public Region region;
    public Pollster pollster;
    public LocalDate date;
    public int sampleSize;
    public HashMap<Party, Double> results = new HashMap<>();
    public boolean generated;

    // Constructor
    public Poll(
        @JsonProperty("region") Region region,
        @JsonProperty("pollster") Pollster pollster,
        @JsonProperty("date") LocalDate date,
        @JsonProperty("sampleSize") int sampleSize,
        @JsonProperty("results") HashMap<Party, Double> results,
        @JsonProperty("generated") boolean generated
    ) {
        this.pollID = pollster.getFullName() + date.toString() + Integer.toString(sampleSize) + region.getFullName();
        this.region = region;
        this.pollster = pollster;
        this.date = date;
        this.sampleSize = sampleSize;
        this.results = results;
        this.generated = generated;
    }
    
    // Getter Methods
    @JsonIgnore 
    public String getID() {
        return pollID; 
    }
    
    @JsonIgnore 
    public String getRegionName() {
        return region.getFullName(); 
    }
    
    @JsonIgnore 
    public String getPollsterName() {
        return pollster.getFullName(); 
    }
    
    public String getDate() {
        return date.toString(); 
    }
    
    public int getSampleSize() {
        return sampleSize; 
    }
    
    @JsonIgnore 
    public String getResults() { 
        String output = "";
        for (int i = 0; i < Party.values().length; i++) {
            output += Party.values()[i] + ": " + results.get(Party.values()[i]) + " ";
        } 
        return output;
    }
    
    @JsonIgnore 
    public Double getPartyResult(Party party) { 
        return results.get(party);
    }
}
