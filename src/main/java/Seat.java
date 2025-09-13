import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Seat {
    // Attributes
    public String name;
    public Region region;
    public Province province;
    public HashMap<Party, Double> results = new HashMap<>();
    public boolean incumbent;

    // Constructor
    public Seat(
        @JsonProperty("name") String name, 
        @JsonProperty("region") Region region, 
        @JsonProperty("province") Province province, 
        @JsonProperty("results") HashMap<Party, Double> results,
        @JsonProperty("incumbent") boolean incumbent
    ) {
        this.name = name;
        this.region = region;
        this.province = province;
        this.results = results;
        this.incumbent = incumbent;
    }

    // Getter Methods
    @JsonIgnore
    public String getSeatName() {
        return name; 
    }
    
    @JsonIgnore
    public String getRegionName() {
        return region.getFullName(); 
    }
    
    @JsonIgnore
    public String getProvinceName() {
        return province.getFullName(); 
    }
    
    @JsonIgnore
    public String getResults() { 
        String output = "";
        for (var i = 0; i < Party.values().length; i++) {
            output += Party.values()[i] + ": " + Math.round(results.get(Party.values()[i]) * 10.0) / 10.0 + " ";
        } 
        return output;
    }
    
    @JsonIgnore
    public Double getPartyResult(Party party) { 
        return results.get(party);
    }

    @JsonIgnore
    public boolean isIncumbent() {
        return incumbent;
    }
    
    public Party getWinner() {
        double winningVote = 0.0;
        Party winningParty = null;
        for (int i = 0; i < Party.values().length; i++) {
            if (results.get(Party.values()[i]) > winningVote) {
                winningParty = Party.values()[i];
                winningVote = results.get(winningParty);
            }
        }
        return winningParty;
    }
    
    public Party getNthPlace(int n) {
        ArrayList<Party> sortedPartyList = new ArrayList<>();
        
        for (int i = 0; i < Party.values().length; i++) {
            Party currentParty = Party.values()[i];
            sortedPartyList.add(currentParty);
        }
        
        Collections.sort(sortedPartyList, Comparator.comparingDouble(results::get).reversed());
        
        return sortedPartyList.get(n);
    }
    
    public Double getWinMargin() {
        double winningVote = 0.0;
        double secondPlaceVote = 0.0;

        for (var i = 0; i < Party.values().length; i++) {
            if (results.get(Party.values()[i]) > winningVote) {
                winningVote = results.get(Party.values()[i]);
            }
        }

        for (var i = 0; i < Party.values().length; i++) {
            if (results.get(Party.values()[i]) > secondPlaceVote && results.get(Party.values()[i]) != winningVote) {
                secondPlaceVote = results.get(Party.values()[i]);
            }
        }
        
        return winningVote - secondPlaceVote;
    }

    public Double getLossMargin(Party party) {
        double winningVote = getPartyResult(getWinner());
        return winningVote - getPartyResult(party);
    }
    
    public String getWinType() {
        double winMargin = getWinMargin();
        
        double safeMargin = 20.0;
        double likelyMargin = 10.0;
        double leaningMargin = 5.0;
        double tossUpMargin = 0.0;
        
        if (winMargin > safeMargin) {
            return "Safe";
        } else if (winMargin > likelyMargin) {
            return "Likely";
        } else if (winMargin > leaningMargin) {
            return "Leaning";
        } else {
            return "Toss Up";
        }
    }
}