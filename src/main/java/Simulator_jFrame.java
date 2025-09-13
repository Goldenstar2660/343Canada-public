import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Collections;
import java.util.Comparator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Paint;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.DefaultCategoryDataset;

public class Simulator_jFrame extends javax.swing.JFrame {
    public ArrayList<Seat> seatDatabase;
    public ArrayList<Seat> seatProjection;
    public PollHashTable pollDatabase;
    public HashMap<Party, Integer> winsPerParty = new HashMap<>();
    public HashMap<Party, Integer> secondPlacePerParty = new HashMap<>();
    public HashMap<Party, Integer> majorityWinsPerParty = new HashMap<>();
    public HashMap<Party, Integer> minorityWinsPerParty = new HashMap<>();
    public HashMap<Party, Integer> maxSeatsPerParty = new HashMap<>();
    public HashMap<Party, Integer> minSeatsPerParty = new HashMap<>();
    public int numParties = Party.values().length ;
    public int numRegions = Region.values().length;
    
    public void setDatabases(PollHashTable pollDatabase, ArrayList<Seat> seatDatabase, ArrayList<Seat> seatProjection) {
        this.seatDatabase = seatDatabase;
        this.seatProjection = seatProjection; 
        this.pollDatabase = pollDatabase;
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
   
    public void setHashMaps(){
        for (int i = 0; i < numParties; i++) {
                Party currentParty = Party.values()[i];
                this.winsPerParty.put(currentParty, 0);
        }
        for (int i = 0; i < numParties; i++) {
                Party currentParty = Party.values()[i];
                this.secondPlacePerParty.put(currentParty, 0);
        }
        for (int i = 0; i < numParties; i++) {
                Party currentParty = Party.values()[i];
                this.minorityWinsPerParty.put(currentParty, 0);
        }
        for (int i = 0; i < numParties; i++) {
                Party currentParty = Party.values()[i];
                this.majorityWinsPerParty.put(currentParty, 0);
        }
        for (int i = 0; i < numParties; i++) {
                Party currentParty = Party.values()[i];
                this.maxSeatsPerParty.put(currentParty, 0);
        }
        for (int i = 0; i < numParties; i++) {
                Party currentParty = Party.values()[i];
                this.minSeatsPerParty.put(currentParty, 9999);
        }
    }
        
    public Simulator_jFrame() {
        initComponents();
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
    //public HashMap<Party, Double> calcRegionStdDev(PollHashTable pollList, Region region){
      public HashMap<Party, Double> calcRegionStdDev(PollHashTable pollList, Region region){
        ArrayList<Poll> polls = pollList.buckets[region.bucket];
        HashMap<Party, Double> results = new HashMap<>();
        for (int i = 0; i < numParties; i++) {
            Party currentParty = Party.values()[i];
            results.put(currentParty, 0.0);
        }
        
       
        for (int i = 0; i < numParties; i++) {
            Party currentParty = Party.values()[i];
            ArrayList<Double> listOfResults = new ArrayList();
            int totalSampleSize = 0;
            double sum = 0.0, standard_deviation = 0.0;
            for (int j = 0; j < polls.size(); j++) {
                Poll poll = polls.get(j);    
                    int sampleSize = poll.getSampleSize();
                    totalSampleSize += sampleSize;
                    for (int k = 0; k < sampleSize; k++){
                        listOfResults.add(poll.results.get(currentParty));
                        sum += poll.results.get(currentParty);
                    }
                }
            double mean = sum/totalSampleSize;
            double meanVarianceSquared = 0;
            for (double value : listOfResults) {
                meanVarianceSquared += Math.pow((value-mean), 2);
            }
            standard_deviation = Math.sqrt(meanVarianceSquared/(totalSampleSize-1));
            results.put(currentParty, standard_deviation);
        }
        return results;
    }
      
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
    
    public void createBarGraph(DefaultCategoryDataset dataset, Paint[] coloursArray, javax.swing.JPanel graphPanel, String title, String categoryAxisLabel, String valueAxisLabel) {
        // make the graph
        JFreeChart seatProjectionGraph = ChartFactory.createBarChart(title, categoryAxisLabel, valueAxisLabel, dataset, PlotOrientation.HORIZONTAL, false, true, false);
        
        ChartPanel chPanel = new ChartPanel(seatProjectionGraph);
        CategoryPlot plot = (CategoryPlot) seatProjectionGraph.getPlot();
        
        // set the size
        chPanel.setPreferredSize(new java.awt.Dimension(339, 193));
        
        CustomRenderer renderer = new CustomRenderer(coloursArray);
        
        // add labels
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        ItemLabelPosition position = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_LEFT);
        renderer.setItemLabelAnchorOffset(5);
        renderer.setDefaultPositiveItemLabelPosition(position);
        
        // extend the x axis to account for labels
        double currentMax = plot.getRangeAxis().getUpperBound();
        plot.getRangeAxis().setUpperBound(currentMax + 15); 
        
        plot.setRenderer(renderer);

        graphPanel.removeAll();
        graphPanel.setLayout(new java.awt.BorderLayout());
        graphPanel.add(chPanel,BorderLayout.CENTER);
        graphPanel.validate();
    }
    
    public void updateOutcomeGraph() {
        // graph dataset
        DefaultCategoryDataset winChancesDataset = new DefaultCategoryDataset();
        
        HashMap<Party, Double> majorityWinChancePerParty = new HashMap<>();
        HashMap<Party, Double> minorityWinChancePerParty = new HashMap<>();
        
        double totalWins = 0;
                
        for (int i = 0; i < Party.values().length; i++) {
            Party currentParty = Party.values()[i];
            totalWins += winsPerParty.get(currentParty);
        }
        
        HashMap<String, Double> winChances = new HashMap<>();
        
        ArrayList<String> partyWinTypes = new ArrayList<>();
        
        for (int i = 0; i < Party.values().length; i++) {
            Party currentParty = Party.values()[i];
            majorityWinChancePerParty.put(currentParty, (majorityWinsPerParty.get(currentParty) / totalWins) * 100);
            minorityWinChancePerParty.put(currentParty, (minorityWinsPerParty.get(currentParty) / totalWins) * 100);
            winChances.put(currentParty + " Majority", majorityWinChancePerParty.get(currentParty));
            winChances.put(currentParty + " Minority", minorityWinChancePerParty.get(currentParty));
            partyWinTypes.add(currentParty.name() + " Majority");
            partyWinTypes.add(currentParty.name() + " Minority");
        }

        Collections.sort(partyWinTypes, Comparator.comparingDouble(winChances::get).reversed());
        Collections.sort(partyWinTypes, Comparator.comparing(winChances::get).reversed());
       
        // add all the data to the graph dataset
        for (int i = 0; i < partyWinTypes.size(); i++) {
            String currentParty = partyWinTypes.get(i);
            if (winChances.get(currentParty) > 0) {
                winChancesDataset.addValue(winChances.get(currentParty), "Chance", currentParty);
            }
        }
        
        // colour
        Paint[] partyColours = new Paint[partyWinTypes.size()];
        for (int i = 0; i < partyWinTypes.size(); i++) {
            partyColours[i] = Party.valueOf(partyWinTypes.get(i).split(" ")[0]).getColour();  
        }
        
        createBarGraph(winChancesDataset, partyColours, winChanceGraphPanel, "Chances of Election Outcome", "Party", "Chance (%)");
    }
    
    public void updateSecondPlaceGraph() {
        // graph dataset
        DefaultCategoryDataset secondPlaceChancesDataset = new DefaultCategoryDataset();
        
        HashMap<Party, Double> secondPlaceChancePerParty = new HashMap<>();

        double totalWins = 0;
        
        ArrayList<Party> sortedPartyList = new ArrayList<>();
                 
        for (int i = 0; i < Party.values().length; i++) {
            Party currentParty = Party.values()[i];
            sortedPartyList.add(currentParty);
            totalWins += winsPerParty.get(currentParty);
        }
        
        for (int i = 0; i < Party.values().length; i++) {
            Party currentParty = Party.values()[i];
            secondPlaceChancePerParty.put(currentParty, (secondPlacePerParty.get(currentParty) / totalWins) * 100);
        }
        
        Collections.sort(sortedPartyList, Comparator.comparingInt(secondPlacePerParty::get).reversed());
       
        // add all the data to the graph dataset
        for (int i = 0; i < sortedPartyList.size(); i++) {
            Party currentParty = sortedPartyList.get(i);
            if (secondPlaceChancePerParty.get(currentParty) > 0) {
                secondPlaceChancesDataset.addValue(secondPlaceChancePerParty.get(currentParty), "Chance", currentParty);
            }
        }
        
        // colour
        Paint[] partyColours = new Paint[numParties];
        for (int i = 0; i < numParties; i++) {
            partyColours[i] = sortedPartyList.get(i).getColour();  
        }
        
        createBarGraph(secondPlaceChancesDataset, partyColours, oppositionChanceGraphPanel, "Chances of Opposition", "Party", "Chance (%)");
    }
     
    public void updateSeatGraphs() {
        // graph dataset
        DefaultCategoryDataset minDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset maxDataset = new DefaultCategoryDataset();
        
        ArrayList<Party> sortedMinPartyList = new ArrayList<>();
        ArrayList<Party> sortedMaxPartyList = new ArrayList<>();
                 
        for (int i = 0; i < Party.values().length; i++) {
            Party currentParty = Party.values()[i];
            sortedMinPartyList.add(currentParty);
            sortedMaxPartyList.add(currentParty);
        }
        
        Collections.sort(sortedMinPartyList, Comparator.comparingInt(minSeatsPerParty::get).reversed());
        Collections.sort(sortedMaxPartyList, Comparator.comparingInt(maxSeatsPerParty::get).reversed());
        
        // add all the data to the graph dataset
        for (int i = 0; i < sortedMinPartyList.size(); i++) {
            Party currentParty = sortedMinPartyList.get(i);
            if (minSeatsPerParty.get(currentParty) > 0) {
                minDataset.addValue(minSeatsPerParty.get(currentParty), "Seats", currentParty);
            }
        }
 
        for (int i = 0; i < sortedMaxPartyList.size(); i++) {
            Party currentParty = sortedMaxPartyList.get(i);
            if (maxSeatsPerParty.get(currentParty) > 0) {
                maxDataset.addValue(maxSeatsPerParty.get(currentParty), "Seats", currentParty);
            }
        }
        
        // colour
        Paint[] minColours = new Paint[numParties];
        Paint[] maxColours = new Paint[numParties];
        for (int i = 0; i < numParties; i++) {
            minColours[i] = sortedMinPartyList.get(i).getColour();  
            maxColours[i] = sortedMaxPartyList.get(i).getColour();  
        }
        
        createBarGraph(minDataset, minColours, minSeatsGraphPanel, "Minimum Seats", "Party", "Seats");
        createBarGraph(maxDataset, maxColours, maxSeatsGraphPanel, "Maximum Seats", "Party", "Seats");
    }
    
    public HashMap calcVotePercentages (ArrayList<Seat> seatList) {
        int totalSeats = 0;
        HashMap<Party, Double> votePercentages = new HashMap<>();
        
        // Fill the results with 0s to add to later
        for (int i = 0; i < numParties; i++) {
            votePercentages.put(Party.values()[i], 0.0);
        }
        
        for (int i = 0; i < seatList.size(); i++) {
            Seat seat = seatList.get(i);
            
            for (int j = 0; j < numParties; j++) {
                Party currentParty = Party.values()[j];
                votePercentages.put(currentParty, votePercentages.get(currentParty) + seat.getPartyResult(currentParty));
            }
            totalSeats++;
        }
        
        for (int i = 0; i < numParties; i++) {
            Double result = votePercentages.get(Party.values()[i]) / totalSeats;
            votePercentages.put(Party.values()[i], Math.round(result * 10.0) / 10.0);
        }
        
        return votePercentages;
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        runSimulationButton = new javax.swing.JButton();
        numSimulationsInput = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        minSeatsGraphPanel = new javax.swing.JPanel();
        winChanceGraphPanel = new javax.swing.JPanel();
        oppositionChanceGraphPanel = new javax.swing.JPanel();
        maxSeatsGraphPanel = new javax.swing.JPanel();

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        runSimulationButton.setText("Run Simulation");
        runSimulationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runSimulationButtonActionPerformed(evt);
            }
        });

        numSimulationsInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                numSimulationsInputFocusLost(evt);
            }
        });

        jLabel1.setText("Number of Simulations:");

        minSeatsGraphPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout minSeatsGraphPanelLayout = new javax.swing.GroupLayout(minSeatsGraphPanel);
        minSeatsGraphPanel.setLayout(minSeatsGraphPanelLayout);
        minSeatsGraphPanelLayout.setHorizontalGroup(
            minSeatsGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 337, Short.MAX_VALUE)
        );
        minSeatsGraphPanelLayout.setVerticalGroup(
            minSeatsGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 191, Short.MAX_VALUE)
        );

        winChanceGraphPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout winChanceGraphPanelLayout = new javax.swing.GroupLayout(winChanceGraphPanel);
        winChanceGraphPanel.setLayout(winChanceGraphPanelLayout);
        winChanceGraphPanelLayout.setHorizontalGroup(
            winChanceGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 337, Short.MAX_VALUE)
        );
        winChanceGraphPanelLayout.setVerticalGroup(
            winChanceGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 191, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(winChanceGraphPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(minSeatsGraphPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(minSeatsGraphPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(winChanceGraphPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        oppositionChanceGraphPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout oppositionChanceGraphPanelLayout = new javax.swing.GroupLayout(oppositionChanceGraphPanel);
        oppositionChanceGraphPanel.setLayout(oppositionChanceGraphPanelLayout);
        oppositionChanceGraphPanelLayout.setHorizontalGroup(
            oppositionChanceGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        oppositionChanceGraphPanelLayout.setVerticalGroup(
            oppositionChanceGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 191, Short.MAX_VALUE)
        );

        maxSeatsGraphPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout maxSeatsGraphPanelLayout = new javax.swing.GroupLayout(maxSeatsGraphPanel);
        maxSeatsGraphPanel.setLayout(maxSeatsGraphPanelLayout);
        maxSeatsGraphPanelLayout.setHorizontalGroup(
            maxSeatsGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 337, Short.MAX_VALUE)
        );
        maxSeatsGraphPanelLayout.setVerticalGroup(
            maxSeatsGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 191, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(maxSeatsGraphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(oppositionChanceGraphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(numSimulationsInput, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(runSimulationButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(numSimulationsInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(runSimulationButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(maxSeatsGraphPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(oppositionChanceGraphPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void runSimulationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runSimulationButtonActionPerformed
        Random rand = new Random();
        
        try{
            Integer.valueOf(numSimulationsInput.getText());
        }
        catch (Exception e){
            return;
        }
        if (Integer.valueOf(numSimulationsInput.getText()) == 0){
            return;
        }
        setHashMaps();
        
        HashMap<Region, HashMap<Party, Double>> regionDevList = new HashMap<>();
        
        for (int i = 0; i < numRegions; i++) {
            Region currentRegion = Region.values()[i];
            HashMap<Party, Double> regionDev = calcRegionStdDev(pollDatabase, currentRegion);
            regionDevList.put(currentRegion, regionDev);
        }
        
        for (int i = 0; i < Integer.parseInt(numSimulationsInput.getText()); i++){
            // seat count in each simulation
            HashMap<Party, Integer> results = new HashMap<>();
            
            for (int j = 0; j < numParties; j++) {
                Party currentParty = Party.values()[j];
                results.put(currentParty, 0);
            }

            // iterate through every currentSeat
            for (int j = 0; j < seatProjection.size(); j++) {
                Seat currentSeat = seatProjection.get(j);
                HashMap<Party, Double> regionDev = regionDevList.get(currentSeat.region);

                // results after applying random deviation
                HashMap<Party, Double> seatResults = new HashMap<>();
                for (int k = 0; k < numParties; k++) {
                    double seatDeviation = rand.nextGaussian();
                    Party currentParty = Party.values()[k];
                    double partyResultsForSeat = currentSeat.getPartyResult(currentParty)+(seatDeviation*regionDev.get(currentParty));
                    seatResults.put(currentParty, partyResultsForSeat);
                }

                // get winner based off new results
                Party winningParty = null;
                double winningVote = 0.0;

                for (int k = 0; k < Party.values().length; k++) {
                    if (seatResults.get(Party.values()[k]) > winningVote) {
                        winningParty = Party.values()[k];
                        winningVote = seatResults.get(Party.values()[k]);
                    }
                }
                results.put(winningParty, results.get(winningParty)+1);
            }
            
            // find winner of the simulation and add to output
            Party winningParty = null;
            double winningVote = 0.0;
                
            for (int j = 0; j < Party.values().length; j++) {
                if (results.get(Party.values()[j]) > winningVote) {
                    winningParty = Party.values()[j];
                    winningVote = results.get(Party.values()[j]);
                    }
                }
            winsPerParty.put(winningParty, winsPerParty.get(winningParty)+1);
            
            if (results.get(winningParty) > 171) {
                majorityWinsPerParty.put(winningParty, majorityWinsPerParty.get(winningParty)+1);
            } else {
                minorityWinsPerParty.put(winningParty, minorityWinsPerParty.get(winningParty)+1);
            }
            
            //max value
            for (int j = 0; j < Party.values().length; j++) {
                if (results.get(Party.values()[j]) > maxSeatsPerParty.get(Party.values()[j])) {
                    maxSeatsPerParty.put(Party.values()[j],results.get(Party.values()[j]));
                    }
                }
            //min value
            for (int j = 0; j < Party.values().length; j++) {
                if (results.get(Party.values()[j]) < minSeatsPerParty.get(Party.values()[j])) {
                    minSeatsPerParty.put(Party.values()[j],results.get(Party.values()[j]));
                    }
                }
            //do the other stuff before second since second removes one
            //System.out.println(results);       

            //Tally second place
            results.put(winningParty, 0);
            Party secondPlaceParty = null;
            double secondVote = 0.0;
                for (int j = 0; j < Party.values().length; j++) {
                    if (results.get(Party.values()[j]) > secondVote) {
                        secondPlaceParty = Party.values()[j];
                        secondVote = results.get(Party.values()[j]);
                        }
                    }
                secondPlacePerParty.put(secondPlaceParty, secondPlacePerParty.get(secondPlaceParty)+1);
        }
        updateOutcomeGraph();
        updateSecondPlaceGraph();
        updateSeatGraphs();
    }//GEN-LAST:event_runSimulationButtonActionPerformed

    private void numSimulationsInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_numSimulationsInputFocusLost
        checkNumericInput(numSimulationsInput);
    }//GEN-LAST:event_numSimulationsInputFocusLost

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
            java.util.logging.Logger.getLogger(Simulator_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Simulator_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Simulator_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Simulator_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Simulator_jFrame().setVisible(true);
            }
        });
    }
    


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JPanel maxSeatsGraphPanel;
    private javax.swing.JPanel minSeatsGraphPanel;
    private javax.swing.JTextField numSimulationsInput;
    private javax.swing.JPanel oppositionChanceGraphPanel;
    private javax.swing.JButton runSimulationButton;
    private javax.swing.JPanel seatGraphPanel1;
    private javax.swing.JPanel seatGraphPanel2;
    private javax.swing.JPanel winChanceGraphPanel;
    // End of variables declaration//GEN-END:variables
}
