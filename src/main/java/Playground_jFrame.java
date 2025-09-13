import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
import java.awt.BorderLayout;
import java.awt.Paint;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.Color;
import javax.swing.DefaultComboBoxModel;
import org.jfree.chart.*;
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

public class Playground_jFrame extends javax.swing.JFrame {
    public PollHashTable pollDatabase;
    public ArrayList<Seat> seatDatabase;
    
    public javax.swing.JLabel[] partyLabels;
    public javax.swing.JSlider[] swingSliders;
    public javax.swing.JLabel[] swingLabels;
    
    private int numParties = Party.values().length;
    
    private HashMap<Region, HashMap<Party, Double>> regionSwings = new HashMap<>();
    
    public Playground_jFrame() {
        initComponents();
        this.setTitle("343 Canada");
        
        partyLabels = new javax.swing.JLabel[]{party1Label, party2Label, party3Label, party4Label, party5Label, party6Label};
        swingSliders = new javax.swing.JSlider[]{party1Slider, party2Slider, party3Slider, party4Slider, party5Slider, party6Slider};
        swingLabels = new javax.swing.JLabel[]{party1SwingLabel, party2SwingLabel, party3SwingLabel, party4SwingLabel, party5SwingLabel, party6SwingLabel};
        for (int i = 0; i < partyLabels.length; i++) {
            partyLabels[i].setText(Party.values()[i].toString() + " Swing");
            swingLabels[i].setText(String.valueOf(swingSliders[i].getValue()));
        }
        
        for (int i = 0; i < Region.values().length; i++) {
            HashMap<Party, Double> partySwings = new HashMap<>();
            for (int j = 0; j < Party.values().length; j++) {
                partySwings.put(Party.values()[j], 0.0);
            }
            regionSwings.put(Region.values()[i], partySwings);
        }
    }
    
    public void setDatabases(PollHashTable pollDatabase, ArrayList<Seat> seatDatabase) {
        this.pollDatabase = pollDatabase;
        this.seatDatabase = seatDatabase;
    }
    
    public void updateSwings() {
        HashMap<Party, Double> partySwings = new HashMap<>();
        for (int i = 0; i < numParties; i++) {
            partySwings.put(Party.values()[i], (Double.valueOf(swingSliders[i].getValue()) / 10));
            swingLabels[i].setText(String.valueOf(Double.valueOf(swingSliders[i].getValue()) / 10));
        }
        regionSwings.put(Region.values()[swingRegionDropdown.getSelectedIndex()], partySwings);
    }
    
    public void setDateToToday() {
        LocalDate dateToday = LocalDate.now();
        
        String dayToday = String.format("%02d", dateToday.getDayOfMonth());
        String monthToday = String.format("%02d", dateToday.getMonthValue());
        String yearToday = String.format("%04d", dateToday.getYear());
        
        dayInput.setText(dayToday);
        monthInput.setText(monthToday);
        yearInput.setText(yearToday);
    }
    
    // input list of seats, return number of seats won per party 
    public HashMap countSeatsWon(ArrayList<Seat> seatList) {
        HashMap<Party, Integer> results = new HashMap<>();
        
        for (int i = 0; i < numParties; i++) {
            results.put(Party.values()[i], 0);
        }
        
        for (int i = 0; i < seatList.size(); i++) {
            Seat seat = seatList.get(i);
            Party winner = seat.getWinner();
            results.put(winner, results.get(winner) + 1);
        }
        
        return results;
    }
    
    // input list of polls, return polling average after weighting
    public HashMap getVoteProjection(PollHashTable pollList, LocalDate date, Region region) {
        // Variables for weightage calculation
        int sampleSize;
        double pollsterWeight;
        long daysSince;
        double timeDecayWeight = 0.8;
        
        double totalWeight = 0.0;

        ArrayList<Poll> polls = pollList.buckets[region.bucket];
        HashMap<Party, Double> weightedResults = new HashMap<>();
        
        // Fill the results with 0s to add to later
        for (int i = 0; i < numParties; i++) {
            weightedResults.put(Party.values()[i], 0.0);
        }
        
        // Multiply all results by weight
        for (int i = 0; i < polls.size(); i++) {
            Poll poll = polls.get(i);
            
            if (poll.date.isBefore(date)) {
                sampleSize = poll.getSampleSize();
                pollsterWeight = poll.pollster.getWeight();
                daysSince = ChronoUnit.DAYS.between(poll.date, date);

                // Check if poll is too far away to matter 
                if (daysSince < 60) {
                    double pollWeight = Math.pow(pollsterWeight, 0.5) * Math.pow(sampleSize, 0.5) * Math.pow(timeDecayWeight, daysSince);
                    if (poll.generated == true) {
                        pollWeight = pollWeight / 2;
                    }

                    totalWeight += pollWeight;
                        
                    for (int j = 0; j < numParties; j++) {
                        Party currentParty = Party.values()[j];
                        double weightedVote = poll.getPartyResult(currentParty) * pollWeight;
                        weightedResults.put(currentParty, weightedResults.get(currentParty) + weightedVote);
                    }
                }
            }
        }
        
        // Divide everything by total weight
        for (int i = 0; i < numParties; i++) {
            double result = weightedResults.get(Party.values()[i]) / totalWeight;
            double swingAdded = regionSwings.get(region).get(Party.values()[i]) + regionSwings.get(Region.CA).get(Party.values()[i]);
            weightedResults.put(Party.values()[i], (Math.round(result * 10.0) / 10.0) + swingAdded);
        }
        
        return weightedResults;
    }
    
    // input seats per party in a region, return estimated vote share per party
    // ca means every seat
    public HashMap calcVotePercentages (ArrayList<Seat> seatList, Region region) {
        int totalSeats = 0;
        HashMap<Party, Double> votePercentages = new HashMap<>();
        
        // Fill the results with 0s to add to later
        for (int i = 0; i < numParties; i++) {
            votePercentages.put(Party.values()[i], 0.0);
        }
        
        for (int i = 0; i < seatList.size(); i++) {
            Seat seat = seatList.get(i);
            
            if (region == Region.CA || region == seat.region) {
                for (int j = 0; j < numParties; j++) {
                    Party currentParty = Party.values()[j];
                    votePercentages.put(currentParty, votePercentages.get(currentParty) + seat.getPartyResult(currentParty));
                }
                totalSeats++;
            }
        }
        
        for (int i = 0; i < numParties; i++) {
            Double result = votePercentages.get(Party.values()[i]) / totalSeats;
            votePercentages.put(Party.values()[i], Math.round(result * 10.0) / 10.0);
        }
        
        return votePercentages;
    }

    // input two results, return difference
    public HashMap calcSwings (HashMap<Party, Double> oldResults, HashMap<Party, Double> newResults) {
        HashMap<Party, Double> swings = new HashMap<>();

        for (int i = 0; i < numParties; i++) {
            Party currentParty = Party.values()[i];
            if (oldResults.get(currentParty) != 0.0) {
                swings.put(currentParty, newResults.get(currentParty)/oldResults.get(currentParty));
            } else {
                swings.put(currentParty, 0.0);
            }

        }

        return swings;
    }

    // apply a swing to a seat
    public Seat applySwingToSeat(Seat seat, HashMap<Party, Double> swings) {
        double incumbentBoost = 0.05;

        HashMap<Party, Double> newResults = new HashMap<>();
        for (Party party : Party.values()) {
            double newPercentage = seat.getPartyResult(party) * swings.get(party);

            if (seat.isIncumbent()) {
                if (seat.getWinner() == Party.NDP) {
                    incumbentBoost = 0.15;
                }

                if (party == seat.getWinner()) {
                    newPercentage = newPercentage * (1 + incumbentBoost);
                } else {
                    newPercentage = newPercentage * (1 - incumbentBoost);
                }
            }

            newResults.put(party, newPercentage);
        }

        Seat newSeat = new Seat(seat.getSeatName(), seat.region, seat.province, newResults, seat.isIncumbent());

        return newSeat;
    }
    
    // returns an arraylist of seats after changing results based on polling data
    public ArrayList<Seat> getSeatProjection(PollHashTable pollList, ArrayList<Seat> oldSeatList, LocalDate date, Region region) {
        ArrayList<Seat> newSeatList = new ArrayList<>();
        // If region is everything
        if (region == Region.CA) {
            // Iterate through every seat
            for (int i = 0; i < oldSeatList.size(); i++) {
                Seat currentSeat = oldSeatList.get(i);
                Region seatRegion = currentSeat.region;
                
                // Compare old results with polling average to determine swing (regional)
                HashMap<Party, Double> oldVotePercentages = calcVotePercentages(oldSeatList, seatRegion);
                HashMap<Party, Double> newVotePercentages = getVoteProjection(pollList, date, seatRegion);
                HashMap<Party, Double> swing = calcSwings(oldVotePercentages, newVotePercentages);
                
                // Apply the swing to the new seat and add it to the output
                Seat newSeat = applySwingToSeat(currentSeat, swing);
                newSeatList.add(newSeat);
            }
        // If region is specific
        } else {
            for (int i = 0; i < oldSeatList.size(); i++) {
                Seat currentSeat = oldSeatList.get(i);
                Region seatRegion = currentSeat.region;
                
                // Only add seats in the region inputted
                if (region == seatRegion) {
                    HashMap<Party, Double> oldVotePercentages = calcVotePercentages(oldSeatList, seatRegion);
                    HashMap<Party, Double> newVotePercentages = getVoteProjection(pollList, date, seatRegion);
                    Seat newSeat = applySwingToSeat(currentSeat, calcSwings(oldVotePercentages, newVotePercentages));
                    newSeatList.add(newSeat);
                }
            }
        }
        return newSeatList;
    }

    public ArrayList<Seat> getSeatsWonByParty(ArrayList<Seat> seatList, Party party, Region region) {
        ArrayList<Seat> seatsWon = new ArrayList<>();
        for (int i = 0; i < seatList.size(); i++) {
            Seat seat = seatList.get(i);
            if (region == Region.CA || (region == seat.region)) {
                if (party == seat.getWinner()) {
                    seatsWon.add(seat);
                }
            }
        }
        return seatsWon;
    }
    
    // copy pasted code for changing colours [1]
    class CustomRenderer extends BarRenderer {
        private Paint[] colours;

        public CustomRenderer(final Paint[] colours) {
            this.colours = colours;
        }

        @Override
        public Paint getItemPaint(final int row, final int column) {
            return this.colours[column % this.colours.length];
        }
    }

    public void createSeatGraph(ArrayList<Seat> seatList) {
        // graph dataset
        DefaultCategoryDataset seatProjection = new DefaultCategoryDataset();
        
        ArrayList<Seat> projectedSeatList = seatList;
        
        // calculate the number of seats won per party (what we're displaying)
        HashMap<Party, Integer> projectedSeatCounts = countSeatsWon(projectedSeatList);
        
        // create a list of parties and sort it from most to least seats
        ArrayList<Party> sortedPartyList = new ArrayList<>();
        
        for (int i = 0; i < Party.values().length; i++) {
            Party currentParty = Party.values()[i];
            sortedPartyList.add(currentParty);
        }
        
        Collections.sort(sortedPartyList, Comparator.comparingInt(projectedSeatCounts::get).reversed());
        
        // addd all the data to the graph dataset
        for (int i = 0; i < sortedPartyList.size(); i++) {
            Party currentParty = sortedPartyList.get(i);
            if (projectedSeatCounts.get(currentParty) > 0) {
                seatProjection.addValue(projectedSeatCounts.get(currentParty), "Seats", currentParty.getFullName());
            }
        }
        
        // make the graph
        JFreeChart seatProjectionGraph = ChartFactory.createBarChart("Seat Projection", "Party", "Seats", seatProjection, PlotOrientation.HORIZONTAL, false, true, false);
        
        ChartPanel chPanel = new ChartPanel(seatProjectionGraph);
        CategoryPlot plot = (CategoryPlot) seatProjectionGraph.getPlot();
        
        // set the size
        chPanel.setPreferredSize(new java.awt.Dimension(535, 260));
        

        // colour everything
        Paint[] partyColours = new Paint[sortedPartyList.size()];
        for (int i = 0; i < sortedPartyList.size(); i++) {
            partyColours[i] = sortedPartyList.get(i).getColour();  
        }
        
        CustomRenderer renderer = new CustomRenderer(partyColours);
        
        // add labels
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        ItemLabelPosition position = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_LEFT);
        renderer.setItemLabelAnchorOffset(10);
        renderer.setDefaultPositiveItemLabelPosition(position);
        
        // extend the x axis to account for labels
        double currentMax = plot.getRangeAxis().getUpperBound();
        plot.getRangeAxis().setUpperBound(currentMax + 10); 
        
        plot.setRenderer(renderer);

        seatGraphPanel.removeAll();
        seatGraphPanel.setLayout(new java.awt.BorderLayout());
        seatGraphPanel.add(chPanel,BorderLayout.CENTER);
        seatGraphPanel.validate();
    }
    
    public void createVoteGraph(ArrayList<Seat> seatList) {
        DefaultCategoryDataset voteProjection = new DefaultCategoryDataset();
        
        ArrayList<Seat> projectedSeatList = seatList;
        HashMap<Party, Double> projectedVotePercentages = calcVotePercentages(projectedSeatList, Region.CA);
        
        ArrayList<Party> sortedPartyList = new ArrayList<>();
        
        for (int i = 0; i < Party.values().length; i++) {
            Party currentParty = Party.values()[i];
            sortedPartyList.add(currentParty);
        }
        
        Collections.sort(sortedPartyList, Comparator.comparingDouble(projectedVotePercentages::get).reversed());
        
        for (int i = 0; i < sortedPartyList.size(); i++) {
            Party currentParty = sortedPartyList.get(i);
            if (projectedVotePercentages.get(currentParty) > 0.0) {
                voteProjection.addValue(projectedVotePercentages.get(currentParty), "Vote Share", currentParty.getFullName());
            }
        }
        
        JFreeChart voteProjectionGraph = ChartFactory.createBarChart("Vote Projection", "Party", "Vote Share", voteProjection, PlotOrientation.HORIZONTAL, false, true, false);
        
        ChartPanel chPanel = new ChartPanel(voteProjectionGraph);
        CategoryPlot plot = (CategoryPlot) voteProjectionGraph.getPlot();
        
        chPanel.setPreferredSize(new java.awt.Dimension(535, 260));
        
        Paint[] partyColours = new Paint[sortedPartyList.size()];
        for (int i = 0; i < sortedPartyList.size(); i++) {
            partyColours[i] = sortedPartyList.get(i).getColour();  
        }
        
        CustomRenderer renderer = new CustomRenderer(partyColours);

        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        ItemLabelPosition position = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_LEFT);
        renderer.setItemLabelAnchorOffset(10);
        renderer.setDefaultPositiveItemLabelPosition(position);
        
        double currentMax = plot.getRangeAxis().getUpperBound();
        plot.getRangeAxis().setUpperBound(currentMax + 5); 
        
        plot.setRenderer(renderer);
        
        voteGraphPanel.removeAll();
        voteGraphPanel.setLayout(new java.awt.BorderLayout());
        voteGraphPanel.add(chPanel,BorderLayout.CENTER);
        voteGraphPanel.validate();
    }
     
    // rnadom thing i made for fun to make maps - probably not gonna use 
    public String getMapChartImportText(ArrayList<Seat> seatList) {
        double safeMargin = 20.0;
        double likelyMargin = 10.0;
        double leaningMargin = 5.0;
        double tossUpMargin = 0.0;
        
        ArrayList<String> cpcSafe = new ArrayList<>();
        ArrayList<String> cpcLikely = new ArrayList<>();
        ArrayList<String> cpcLeaning = new ArrayList<>();
        ArrayList<String> cpcTossUp = new ArrayList<>();
        ArrayList<String> lpcSafe = new ArrayList<>();
        ArrayList<String> lpcLikely = new ArrayList<>();
        ArrayList<String> lpcLeaning = new ArrayList<>();
        ArrayList<String> lpcTossUp = new ArrayList<>();
        ArrayList<String> ndpSafe = new ArrayList<>();
        ArrayList<String> ndpLikely = new ArrayList<>();
        ArrayList<String> ndpLeaning = new ArrayList<>();
        ArrayList<String> ndpTossUp = new ArrayList<>();
        ArrayList<String> gpcSafe = new ArrayList<>();
        ArrayList<String> gpcLikely = new ArrayList<>();
        ArrayList<String> gpcLeaning = new ArrayList<>();
        ArrayList<String> gpcTossUp = new ArrayList<>();
        ArrayList<String> bqSafe = new ArrayList<>();
        ArrayList<String> bqLikely = new ArrayList<>();
        ArrayList<String> bqLeaning = new ArrayList<>();
        ArrayList<String> bqTossUp = new ArrayList<>();

        for (int i = 0; i < seatList.size(); i++) {
            Seat seat = seatList.get(i);
            double winMargin = seat.getWinMargin();
            Party winner = seat.getWinner();
            if (winMargin > safeMargin) {
                switch (winner) {
                    case Party.CPC:
                        cpcSafe.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.LPC:
                        lpcSafe.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.NDP:
                        ndpSafe.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.GPC:
                        gpcSafe.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.BQ:
                        bqSafe.add("\"" + seat.getSeatName() + "\"");
                        break;
                }
            } else if (winMargin > likelyMargin) {
                switch (winner) {
                    case Party.CPC:
                        cpcLikely.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.LPC:
                        lpcLikely.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.NDP:
                        ndpLikely.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.GPC:
                        gpcLikely.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.BQ:
                        bqLikely.add("\"" + seat.getSeatName() + "\"");
                        break;
                }
            } else if (winMargin > leaningMargin) {
                switch (winner) {
                    case Party.CPC:
                        cpcLeaning.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.LPC:
                        lpcLeaning.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.NDP:
                        ndpLeaning.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.GPC:
                        gpcLeaning.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.BQ:
                        bqLeaning.add("\"" + seat.getSeatName() + "\"");
                        break;
                }
            } else if (winMargin > tossUpMargin) {
                switch (winner) {
                    case Party.CPC:
                        cpcTossUp.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.LPC:
                        lpcTossUp.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.NDP:
                        ndpTossUp.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.GPC:
                        gpcTossUp.add("\"" + seat.getSeatName() + "\"");
                        break;
                    case Party.BQ:
                        bqTossUp.add("\"" + seat.getSeatName() + "\"");
                        break;
                }
            }
        }

        StringBuilder output = new StringBuilder("{\"groups\":");
        output.append("{\"#598dab\":{\"label\":\"CPC Safe\",\"paths\":").append(cpcSafe).append("},")
            .append("\"#82a9c0\":{\"label\":\"CPC Likely\",\"paths\":").append(cpcLikely).append("},")
            .append("\"#acc6d5\":{\"label\":\"CPC Leaning\",\"paths\":").append(cpcLeaning).append("},")
            .append("\"#cddde6\":{\"label\":\"CPC Toss Up\",\"paths\":").append(cpcTossUp).append("},")
            .append("\"#c94141\":{\"label\":\"LPC Safe\",\"paths\":").append(lpcSafe).append("},")
            .append("\"#d67070\":{\"label\":\"LPC Likely\",\"paths\":").append(lpcLikely).append("},")
            .append("\"#e4a0a0\":{\"label\":\"LPC Leaning\",\"paths\":").append(lpcLeaning).append("},")
            .append("\"#efc6c6\":{\"label\":\"LPC Toss Up\",\"paths\":").append(lpcTossUp).append("},")
            .append("\"#e69f4a\":{\"label\":\"NDP Safe\",\"paths\":").append(ndpSafe).append("},")
            .append("\"#ecb777\":{\"label\":\"NDP Likely\",\"paths\":").append(ndpLikely).append("},")
            .append("\"#f3cfa5\":{\"label\":\"NDP Leaning\",\"paths\":").append(ndpLeaning).append("},")
            .append("\"#f8e2c9\":{\"label\":\"NDP Toss Up\",\"paths\":").append(ndpTossUp).append("},")
            .append("\"#88c9c3\":{\"label\":\"BQ Safe\",\"paths\":").append(bqSafe).append("},")
            .append("\"#a6d6d2\":{\"label\":\"BQ Likely\",\"paths\":").append(bqLikely).append("},")
            .append("\"#c4e4e1\":{\"label\":\"BQ Leaning\",\"paths\":").append(bqLeaning).append("},")
            .append("\"#dbefed\":{\"label\":\"BQ Toss Up\",\"paths\":").append(bqTossUp).append("},")
            .append("\"#85ab53\":{\"label\":\"GPC Safe\",\"paths\":").append(gpcSafe).append("},")
            .append("\"#a3c07e\":{\"label\":\"GPC Likely\",\"paths\":").append(gpcLikely).append("},")
            .append("\"#c2d5a9\":{\"label\":\"GPC Leaning\",\"paths\":").append(gpcLeaning).append("},")
            .append("\"#e1ead4\":{\"label\":\"GPC Toss Up\",\"paths\":").append(gpcTossUp).append("}")
            .append("},\"title\":\"\",\"hidden\":[],\"background\":\"#ffffff\",\"borders\":\"#000\",")
            .append("\"legendFont\":\"Century Gothic\",\"legendFontColor\":\"#000\",")
            .append("\"legendBorderColor\":\"#00000000\",\"legendBgColor\":\"#00000000\",")
            .append("\"legendWidth\":150,\"legendBoxShape\":\"square\",\"areBordersShown\":true,")
            .append("\"defaultColor\":\"#d1dbdd\",\"labelsColor\":\"#000000\",\"labelsFont\":\"Arial\",")
            .append("\"strokeWidth\":\"medium\",\"areLabelsShown\":false,\"uncoloredScriptColor\":\"#ffff33\",")
            .append("\"v6\":true,\"legendPosition\":\"top_right\",\"legendSize\":\"medium\",")
            .append("\"legendTranslateX\":\"0.00\",\"legendStatus\":\"hide\",\"scalingPatterns\":true,")
            .append("\"legendRowsSameColor\":true,\"legendColumnCount\":1}");

        return output.toString();
    }
    
    public void updateGraphs() {
        String day = String.format("%02d", Integer.valueOf(dayInput.getText()));
        String month = String.format("%02d", Integer.valueOf(monthInput.getText()));
        String year = String.format("%04d", Integer.valueOf(yearInput.getText()));
        String dateString = year + "-" + month + "-" + day;

        LocalDate projectionDate = LocalDate.parse(dateString);
        
        Region projectionRegion = Region.values()[regionDropdown.getSelectedIndex()];
        
        ArrayList<Seat> seatProjection = getSeatProjection(pollDatabase, seatDatabase, projectionDate, projectionRegion);
        
        createSeatGraph(seatProjection);
        createVoteGraph(seatProjection);
    }
    
    public void checkNumericInput (javax.swing.JTextField field) {
        if (field.getText().equals("")) {
            field.setBackground(Color.white);
            return;
        }
        try { 
            if (Integer.parseInt(field.getText()) > 0) {
                field.setBackground(Color.white);
            } else {
                field.setBackground(new Color(255,209,209));
            }
        }
        catch (Exception e) {
            field.setBackground(new Color(255,209,209));
        }   
    }
    
    public void checkDateInputValidity() {
        checkNumericInput(dayInput);
        checkNumericInput(monthInput);
        checkNumericInput(yearInput);

        try {
            String day = String.format("%02d", Integer.valueOf(dayInput.getText()));
            String month = String.format("%02d", Integer.valueOf(monthInput.getText()));
            String year = String.format("%04d", Integer.valueOf(yearInput.getText()));
            if (Integer.valueOf(yearInput.getText()) < 2022 && Integer.valueOf(monthInput.getText()) < 11){
                throw new java.lang.Exception();
            }
            String dateString = year + "-" + month + "-" + day;
            LocalDate projectionDate = LocalDate.parse(dateString);
        } catch (Exception e) {
            dayInput.setBackground(new Color(255,209,209));
            monthInput.setBackground(new Color(255,209,209));
            yearInput.setBackground(new Color(255,209,209));
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        seatGraphPanel = new javax.swing.JPanel();
        refreshButton = new javax.swing.JButton();
        voteGraphPanel = new javax.swing.JPanel();
        dayInput = new javax.swing.JTextField();
        monthInput = new javax.swing.JTextField();
        yearInput = new javax.swing.JTextField();
        todayButton = new javax.swing.JButton();
        displayDetailedButton = new javax.swing.JButton();
        regionDropdown = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        copyToClipboardButton = new javax.swing.JButton();
        party1Slider = new javax.swing.JSlider();
        party2Slider = new javax.swing.JSlider();
        party3Slider = new javax.swing.JSlider();
        party4Slider = new javax.swing.JSlider();
        party5Slider = new javax.swing.JSlider();
        party6Slider = new javax.swing.JSlider();
        party1Label = new javax.swing.JLabel();
        party2Label = new javax.swing.JLabel();
        party3Label = new javax.swing.JLabel();
        party4Label = new javax.swing.JLabel();
        party5Label = new javax.swing.JLabel();
        party6Label = new javax.swing.JLabel();
        party2SwingLabel = new javax.swing.JLabel();
        party3SwingLabel = new javax.swing.JLabel();
        party1SwingLabel = new javax.swing.JLabel();
        party5SwingLabel = new javax.swing.JLabel();
        party6SwingLabel = new javax.swing.JLabel();
        party4SwingLabel = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        swingRegionDropdown = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        seatGraphPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        seatGraphPanel.setMaximumSize(new java.awt.Dimension(537, 262));

        javax.swing.GroupLayout seatGraphPanelLayout = new javax.swing.GroupLayout(seatGraphPanel);
        seatGraphPanel.setLayout(seatGraphPanelLayout);
        seatGraphPanelLayout.setHorizontalGroup(
            seatGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        seatGraphPanelLayout.setVerticalGroup(
            seatGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 287, Short.MAX_VALUE)
        );

        refreshButton.setText("Refresh");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });

        voteGraphPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        voteGraphPanel.setMaximumSize(new java.awt.Dimension(537, 262));

        javax.swing.GroupLayout voteGraphPanelLayout = new javax.swing.GroupLayout(voteGraphPanel);
        voteGraphPanel.setLayout(voteGraphPanelLayout);
        voteGraphPanelLayout.setHorizontalGroup(
            voteGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 535, Short.MAX_VALUE)
        );
        voteGraphPanelLayout.setVerticalGroup(
            voteGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 260, Short.MAX_VALUE)
        );

        dayInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        dayInput.setText("DD");
        dayInput.setToolTipText("");
        dayInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                dayInputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                dayInputFocusLost(evt);
            }
        });

        monthInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        monthInput.setText("MM");
        monthInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                monthInputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                monthInputFocusLost(evt);
            }
        });

        yearInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        yearInput.setText("YYYY");
        yearInput.setToolTipText("The furthest year it dates back to is last federal election. The year values have to be at least 2022.");
        yearInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                yearInputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                yearInputFocusLost(evt);
            }
        });

        todayButton.setLabel("Today");
        todayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                todayButtonActionPerformed(evt);
            }
        });

        displayDetailedButton.setText("Display Detailed Projection");
        displayDetailedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayDetailedButtonActionPerformed(evt);
            }
        });

        regionDropdown.setModel(new DefaultComboBoxModel(Region.getFullNameList()));
        regionDropdown.setMaximumSize(new java.awt.Dimension(72, 22));
        regionDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                regionDropdownItemStateChanged(evt);
            }
        });

        jLabel1.setText("Select Region");

        jLabel2.setText("Select Date");

        copyToClipboardButton.setText("Copy MapChart Data");
        copyToClipboardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyToClipboardButtonActionPerformed(evt);
            }
        });

        party1Slider.setMaximum(150);
        party1Slider.setMinimum(-150);
        party1Slider.setValue(0);
        party1Slider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                party1SliderStateChanged(evt);
            }
        });

        party2Slider.setMaximum(150);
        party2Slider.setMinimum(-150);
        party2Slider.setValue(0);
        party2Slider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                party2SliderStateChanged(evt);
            }
        });

        party3Slider.setMaximum(150);
        party3Slider.setMinimum(-150);
        party3Slider.setValue(0);
        party3Slider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                party3SliderStateChanged(evt);
            }
        });

        party4Slider.setMaximum(150);
        party4Slider.setMinimum(-150);
        party4Slider.setValue(0);
        party4Slider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                party4SliderStateChanged(evt);
            }
        });

        party5Slider.setMaximum(150);
        party5Slider.setMinimum(-150);
        party5Slider.setValue(0);
        party5Slider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                party5SliderStateChanged(evt);
            }
        });

        party6Slider.setMaximum(150);
        party6Slider.setMinimum(-150);
        party6Slider.setValue(0);
        party6Slider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                party6SliderStateChanged(evt);
            }
        });

        party1Label.setText("jLabel3");

        party2Label.setText("jLabel4");

        party3Label.setText("jLabel5");

        party4Label.setText("jLabel6");

        party5Label.setText("jLabel7");

        party6Label.setText("jLabel8");

        party2SwingLabel.setText("jLabel4");

        party3SwingLabel.setText("jLabel5");

        party1SwingLabel.setText("jLabel3");

        party5SwingLabel.setText("jLabel7");

        party6SwingLabel.setText("jLabel8");

        party4SwingLabel.setText("jLabel6");

        jLabel3.setText("Select Region");

        swingRegionDropdown.setModel(new DefaultComboBoxModel(Region.getFullNameList()));
        swingRegionDropdown.setMaximumSize(new java.awt.Dimension(72, 22));
        swingRegionDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                swingRegionDropdownItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(voteGraphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(seatGraphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(party1Label, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(party2Label, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(party3Label, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(party4Label, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(party5Label, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(party6Label, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(refreshButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(copyToClipboardButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(swingRegionDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(9, 9, 9)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(regionDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addComponent(dayInput, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(monthInput, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(yearInput, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(todayButton, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(party3Slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(party3SwingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(party6Slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(party6SwingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(party1Slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(party1SwingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(party2Slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(party2SwingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(party4Slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(party4SwingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(party5Slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(party5SwingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addComponent(displayDetailedButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(regionDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(yearInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(monthInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dayInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(todayButton)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(refreshButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(displayDetailedButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(swingRegionDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)))
                    .addComponent(voteGraphPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(seatGraphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(party1Slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(party1Label)
                            .addComponent(party1SwingLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(party2Slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(party2Label)
                            .addComponent(party2SwingLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(party3Slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(party3Label)
                            .addComponent(party3SwingLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(party4Slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(party4Label)
                            .addComponent(party4SwingLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(party5Slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(party5Label)
                            .addComponent(party5SwingLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(party6Slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(party6Label)
                            .addComponent(party6SwingLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 116, Short.MAX_VALUE)
                        .addComponent(copyToClipboardButton)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        updateGraphs();
        checkDateInputValidity();
    }//GEN-LAST:event_refreshButtonActionPerformed

    private void dayInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dayInputFocusGained
        if (dayInput.getText().equals("DD")) {
            dayInput.setText("");
        }
    }//GEN-LAST:event_dayInputFocusGained

    private void dayInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dayInputFocusLost
        if (dayInput.getText().equals("")) {
            dayInput.setText("DD");
        } else {
            checkDateInputValidity();
        }
    }//GEN-LAST:event_dayInputFocusLost

    private void monthInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_monthInputFocusGained
        if (monthInput.getText().equals("MM")) {
            monthInput.setText("");
        }
    }//GEN-LAST:event_monthInputFocusGained

    private void monthInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_monthInputFocusLost
        if (monthInput.getText().equals("")) {
            monthInput.setText("MM");
        } else {
            checkDateInputValidity();
        }
    }//GEN-LAST:event_monthInputFocusLost

    private void yearInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_yearInputFocusGained
        if (yearInput.getText().equals("YYYY")) {
            yearInput.setText("");
        }
    }//GEN-LAST:event_yearInputFocusGained

    private void yearInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_yearInputFocusLost
        if (yearInput.getText().equals("")) {
            yearInput.setText("YYYY");
        } else {
            checkDateInputValidity();
        }
    }//GEN-LAST:event_yearInputFocusLost

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        setDateToToday();
        updateGraphs();
        requestFocusInWindow();
    }//GEN-LAST:event_formWindowOpened

    private void todayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_todayButtonActionPerformed
        setDateToToday();
        updateGraphs();
        checkDateInputValidity();
    }//GEN-LAST:event_todayButtonActionPerformed

    private void regionDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_regionDropdownItemStateChanged
        updateGraphs();
    }//GEN-LAST:event_regionDropdownItemStateChanged

    private void displayDetailedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayDetailedButtonActionPerformed
        String day = String.format("%02d", Integer.valueOf(dayInput.getText()));
        String month = String.format("%02d", Integer.valueOf(monthInput.getText()));
        String year = String.format("%04d", Integer.valueOf(yearInput.getText()));
        String dateString = year + "-" + month + "-" + day;

        LocalDate projectionDate = LocalDate.parse(dateString);
       
        DetailedResults_jFrame DetailedResults_jFrame = new DetailedResults_jFrame();
        DetailedResults_jFrame.setVisible(true);
        DetailedResults_jFrame.setDatabases(pollDatabase, seatDatabase, getSeatProjection(pollDatabase, seatDatabase, projectionDate, Region.CA));
        if (!regionDropdown.getSelectedItem().toString().equals("National")) {
            DetailedResults_jFrame.setLocationFilterValueIndex(regionDropdown.getSelectedIndex() + 1);   
        }
    }//GEN-LAST:event_displayDetailedButtonActionPerformed

    private void copyToClipboardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyToClipboardButtonActionPerformed
        String day = String.format("%02d", Integer.valueOf(dayInput.getText()));
        String month = String.format("%02d", Integer.valueOf(monthInput.getText()));
        String year = String.format("%04d", Integer.valueOf(yearInput.getText()));
        String dateString = year + "-" + month + "-" + day;

        LocalDate projectionDate = LocalDate.parse(dateString);
        
        Region projectionRegion = Region.values()[regionDropdown.getSelectedIndex()];
        
        ArrayList<Seat> seatProjection = getSeatProjection(pollDatabase, seatDatabase, projectionDate, projectionRegion);
        
        String mapChartData = getMapChartImportText(seatProjection);
        StringSelection stringSelection = new StringSelection(mapChartData);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }//GEN-LAST:event_copyToClipboardButtonActionPerformed

    private void swingRegionDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_swingRegionDropdownItemStateChanged
        Region selectedRegion = Region.values()[swingRegionDropdown.getSelectedIndex()];
        HashMap<Party, Double> partySwings = regionSwings.get(selectedRegion);
        for (int i = 0; i < Party.values().length; i++) {
            swingLabels[i].setText(String.valueOf(partySwings.get(Party.values()[i])));
            swingSliders[i].setValue((int) (partySwings.get(Party.values()[i]) * 10));
        }
        updateGraphs();
    }//GEN-LAST:event_swingRegionDropdownItemStateChanged

    private void party1SliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_party1SliderStateChanged
        updateSwings();
        updateGraphs();
    }//GEN-LAST:event_party1SliderStateChanged

    private void party2SliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_party2SliderStateChanged
        updateSwings();
        updateGraphs();
    }//GEN-LAST:event_party2SliderStateChanged

    private void party3SliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_party3SliderStateChanged
        updateSwings();
        updateGraphs();
    }//GEN-LAST:event_party3SliderStateChanged

    private void party4SliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_party4SliderStateChanged
        updateSwings();
        updateGraphs();
    }//GEN-LAST:event_party4SliderStateChanged

    private void party5SliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_party5SliderStateChanged
        updateSwings();
        updateGraphs();
    }//GEN-LAST:event_party5SliderStateChanged

    private void party6SliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_party6SliderStateChanged
        updateSwings();
        updateGraphs();
    }//GEN-LAST:event_party6SliderStateChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Main_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Main_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Main_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Main_jFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton copyToClipboardButton;
    private javax.swing.JTextField dayInput;
    private javax.swing.JButton displayDetailedButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JTextField monthInput;
    private javax.swing.JLabel party1Label;
    private javax.swing.JSlider party1Slider;
    private javax.swing.JLabel party1SwingLabel;
    private javax.swing.JLabel party2Label;
    private javax.swing.JSlider party2Slider;
    private javax.swing.JLabel party2SwingLabel;
    private javax.swing.JLabel party3Label;
    private javax.swing.JSlider party3Slider;
    private javax.swing.JLabel party3SwingLabel;
    private javax.swing.JLabel party4Label;
    private javax.swing.JSlider party4Slider;
    private javax.swing.JLabel party4SwingLabel;
    private javax.swing.JLabel party5Label;
    private javax.swing.JSlider party5Slider;
    private javax.swing.JLabel party5SwingLabel;
    private javax.swing.JLabel party6Label;
    private javax.swing.JSlider party6Slider;
    private javax.swing.JLabel party6SwingLabel;
    private javax.swing.JButton refreshButton;
    private javax.swing.JComboBox<String> regionDropdown;
    private javax.swing.JPanel seatGraphPanel;
    private javax.swing.JComboBox<String> swingRegionDropdown;
    private javax.swing.JButton todayButton;
    private javax.swing.JPanel voteGraphPanel;
    private javax.swing.JTextField yearInput;
    // End of variables declaration//GEN-END:variables
}
