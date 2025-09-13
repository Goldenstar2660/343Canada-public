import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class LiveResults_jFrame extends javax.swing.JFrame {
    private DefaultTableModel model;

    private ArrayList<Seat> seatDatabase;
    private ArrayList<Seat> seatProjection;
    private ArrayList<SeatResult> liveResults;

    public LiveResults_jFrame() {
        initComponents();
        this.setTitle("Detailed Projection");
        
        DefaultComboBoxModel model = new DefaultComboBoxModel();

        model.addElement("All");
        
        for (int i = 0; i < Party.values().length; i++) {
            model.addElement(Party.values()[i].getFullName());
        }
        
        resultFilterValueDropdown.setModel(model);
        
        updateFilterDropdowns();
    }

    public void setDatabases(ArrayList<Seat> seatDatabase, ArrayList<Seat> seatProjection, ArrayList<SeatResult> liveResults) {
        this.seatDatabase = seatDatabase;
        this.seatProjection = seatProjection;
        this.liveResults = liveResults;
    }

    public Double simpleWinProbabilities(SeatResult seatResult) {
        HashMap<Party, Double> winProbabilities = new HashMap<>();
        HashMap<Party, Double> blendedVoteShares = new HashMap<>();
        double pollsReporting = seatResult.getPollsReporting();
        Seat seatInProjection = null;

        // Find the matching projected seat
        for (Seat seat : seatProjection) {
            if (seat.getSeatName().equals(seatResult.getSeatName())) {
                seatInProjection = seat;
                break;
            }
        }

        // Handle null case for seatInProjection
        if (seatInProjection == null) {
            System.err.println("Warning: No matching projection seat found for seat: " + seatResult.getSeatName());
            return 0.0; // Return a default value to avoid NullPointerException
        }

        double liveResultWeight = Math.pow(pollsReporting, 0.5);

        for (Party party : Party.values()) {
            if (seatInProjection.getPartyResult(party) != null && seatResult.getPartyResult(party) != null) {
                blendedVoteShares.put(party, (1 - liveResultWeight) * seatInProjection.getPartyResult(party)
                        + liveResultWeight * seatResult.getPartyResult(party));
            }
        }

        ArrayList<Party> sortedPartyList = new ArrayList<>();

        for (Party party : Party.values()) {
            sortedPartyList.add(party);
        }

        Collections.sort(sortedPartyList, Comparator.comparingDouble(blendedVoteShares::get).reversed());

        double lead = blendedVoteShares.get(sortedPartyList.get(0)) - blendedVoteShares.get(sortedPartyList.get(1));

        double totalVoteShare = 0.0;

        for (Party party : Party.values()) {
            if (party == sortedPartyList.get(0)) {
                winProbabilities.put(party, lead / 100 + liveResultWeight);
            } else if (seatInProjection.getPartyResult(party) != 0.0) {
                totalVoteShare += blendedVoteShares.get(party);
            }
        }

        for (Party party : Party.values()) {
            if (party != sortedPartyList.get(0) && seatInProjection.getPartyResult(party) != 0.0) {
                winProbabilities.put(party, ((blendedVoteShares.get(party) / 100) / (totalVoteShare / 100))
                        * (1 - winProbabilities.get(sortedPartyList.get(0))));
            } else if (seatInProjection.getPartyResult(party) == 0.0) {
                winProbabilities.put(party, 0.0);
            }
        }

        return winProbabilities.get(sortedPartyList.get(0));
    }

    public HashMap<Party, Double> bayesianWinProbabilities(SeatResult seatResult) {
        HashMap<Party, Double> posteriorProbabilities = new HashMap<>();
        double pollsReporting = seatResult.getPollsReporting();
        Seat seatInProjection = null;

        for (Seat seat : seatProjection) {
            if (seat.getSeatName().equals(seatResult.getSeatName())) {
                seatInProjection = seat;
            }
        }

        // Make hash with current results
        HashMap<Party, Double> voteShare = new HashMap<>();
        for (Party party : Party.values()) {
            voteShare.put(party, seatResult.getPartyResult(party) / 100);
        }

        // Get polling data to compare with
        HashMap<Party, Double> priorProbabilities = new HashMap<>();
        for (Party party : Party.values()) {
            double historicalProjection = seatInProjection.getPartyResult(party) / 100;
            priorProbabilities.put(party, historicalProjection);
        }
        System.out.println("priorProbabilities " + priorProbabilities) ;

        // Set up for bayes theorem; find the prior and likelihood with a hard coded variance
        // maybe find variance from previous election cycles
        HashMap<Party, Double> likelihoods = new HashMap<>();
        double variance = 0.05 * (1 / pollsReporting);
        for (Party party : Party.values()) {
            double observedVoteShare = voteShare.get(party); // polls
            double predictedVoteShare = seatInProjection.getPartyResult(party) / 100; // historical
            double likelihood = Math.exp(-Math.pow(observedVoteShare - predictedVoteShare, 2) / (2 * variance)) / Math.sqrt(2 * Math.PI * variance);
            likelihoods.put(party, likelihood);
        }

        // Given current evidence, use bayes theorem to update posteriorProbabilities, being the seats predicted results for each party
        double evidence = 0.0;

        for (Party party : Party.values()) {
            double prior = priorProbabilities.get(party);
            double likelihood = likelihoods.get(party);
            evidence += (prior * likelihood);
        }
        for (Party party : Party.values()) {
            double posterior = likelihoods.get(party) * priorProbabilities.get(party) / evidence;
            if (posterior < 0.0){
                posteriorProbabilities.put(party, 0.0);
            }
            else{
                posteriorProbabilities.put(party, posterior);
            }
        }

        return posteriorProbabilities;
    }
    
    public void setLocationFilterValueIndex(int index) {
        locationFilterValueDropdown.setSelectedIndex(index);
    }

    private class SeatTableCellRenderer extends DefaultTableCellRenderer {
        private ArrayList<SeatResult> liveResults;

        public SeatTableCellRenderer(ArrayList<SeatResult> liveResults) {
            this.liveResults = liveResults;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            SeatResult seat = liveResults.get(row);

            if (isSelected) {
                cell.setBackground(table.getSelectionBackground());
            } else {
                cell.setBackground(Color.WHITE);
            }

            if (!liveResults.isEmpty() && seat != null && seat.pollsReporting != 0.0) {
                if (simpleWinProbabilities(seat) > 0.7) {
                    cell.setBackground(seat.getWinner().getColour());
                    cell.setForeground(Color.WHITE); // <- set text color to WHITE
                } else {
                    cell.setBackground(seat.getWinner().getLeaningColour());
                    cell.setForeground(Color.BLACK); // <- otherwise use BLACK
                }
            } else {
                cell.setForeground(Color.BLACK); // default
            }

            return cell;
        }
    }

    private String isGain(SeatResult seatResult) {
        String resultSeatName = seatResult.getSeatName();
        Party resultSeatWinner = seatResult.getWinner();
        Party oldSeatWinner = null;
        for (int i = 0; i < seatDatabase.size(); i++){
            Seat oldSeat = seatDatabase.get(i);
            if (oldSeat.getSeatName().equals(resultSeatName)) {
                oldSeatWinner = oldSeat.getWinner();
                break;
            }
        }
        if (resultSeatWinner == oldSeatWinner) {
            return "Hold";
        }
        return "Gain";
    }

    private void updateTable(ArrayList<SeatResult> seatList) {
        ArrayList<SeatResult> formattedSeatList = new ArrayList<>();
        formattedSeatList = sortTable(filterTable(seatList));
        
        String[] parties = new String[Party.values().length];
        
        for (int i = 0; i < Party.values().length; i++) {
            parties[i] = Party.values()[i].name();
        }
        
        String[] columnNames = new String[7 + Party.values().length];
        
        columnNames[0] = "Name";
        columnNames[1] = "Province";
        columnNames[2] = "Winner";
        columnNames[3] = "Margin";
        columnNames[4] = "Win Chance";
        columnNames[5] = "Polls Reporting";
        columnNames[6] = "Gain";
        
        for (int i = 7; i < columnNames.length; i++) {
            columnNames[i] = parties[i - 7];
        }

        model = new DefaultTableModel(columnNames, formattedSeatList.size());
        seatTable.setModel(model);
        seatTable.setAutoCreateColumnsFromModel(true);
        seatTable.setDefaultRenderer(Object.class, new LiveResults_jFrame.SeatTableCellRenderer(formattedSeatList));

        int rowCounter = -1; 
      
        for (int i = 0; i < formattedSeatList.size(); i++) {
            SeatResult seat = formattedSeatList.get(i);

            rowCounter++;

            model.setValueAt(seat.getSeatName(), rowCounter, 0);
            model.setValueAt(seat.province, rowCounter, 1);
            model.setValueAt(seat.getWinner() + " " + seat.getWinType(), rowCounter, 2);
            model.setValueAt(Math.round(seat.getWinMargin() * 10.0) / 10.0, rowCounter, 3);
            model.setValueAt(Math.round(simpleWinProbabilities(seat) * 100 * 10.0) / 10.0 + "%", rowCounter, 4);
            model.setValueAt(Math.round(seat.getPollsReporting() * 100 * 10.0) / 10.0 + "%", rowCounter, 5);
            model.setValueAt(isGain(seat), rowCounter, 6);
            
            seatTable.getColumnModel().getColumn(0).setPreferredWidth(250);
            seatTable.getColumnModel().getColumn(1).setPreferredWidth(20);
            seatTable.getColumnModel().getColumn(2).setPreferredWidth(50);
            seatTable.getColumnModel().getColumn(3).setPreferredWidth(10);
            seatTable.getColumnModel().getColumn(4).setPreferredWidth(60);
            seatTable.getColumnModel().getColumn(5).setPreferredWidth(60);
            seatTable.getColumnModel().getColumn(6).setPreferredWidth(30);
            
            for (int j = 0; j < Party.values().length; j++) {
                Double result = seat.getPartyResult(Party.values()[j]);
                model.setValueAt(Math.round(result * 10.0) / 10.0, rowCounter, j + 7);
                seatTable.getColumnModel().getColumn(j + 7).setPreferredWidth(10);
            }
        }
    }
    
    public ArrayList<SeatResult> filterTable(ArrayList<SeatResult> seatList) {
        ArrayList<SeatResult> filteredSeatList = new ArrayList<>();
        
        int resultFilterNthPlace = resultFilterTypeDropdown.getSelectedIndex();
        String resultFilterValue = resultFilterValueDropdown.getSelectedItem().toString();
        
        String locationFilterType = locationFilterTypeDropdown.getSelectedItem().toString();
        String locationFilterValue = locationFilterValueDropdown.getSelectedItem().toString();

        String searchString = searchInput.getText().toLowerCase();
        
        for (int i = 0; i < seatList.size(); i++) {
            SeatResult seat = seatList.get(i);
            
            // if no search or matches search string
            if ((seat.getSeatName().toLowerCase().contains(searchString) || searchString.equals("")) && seat.getWinner() != null) {
                // if filtering by neither
                if (locationFilterValue.equals("All") && resultFilterValue.equals("All")) {
                    filteredSeatList.add(seat);
                // if filtering by something
                } else {
                    // if meets requirement for result filter
                    if (resultFilterValue.equals("All") || resultFilterValue.equals(seat.getNthPlace(resultFilterNthPlace).getFullName())) {
                        switch (locationFilterType) {
                            case "Region":
                                // if meets requirement for location filter
                                if (locationFilterValue.equals("All") || locationFilterValue.equals(seat.getRegionName())) {
                                    filteredSeatList.add(seat);
                                }
                                break;

                            case "Province":
                                if (locationFilterValue.equals("All") || locationFilterValue.equals(seat.getProvinceName())) {
                                    filteredSeatList.add(seat);
                                }
                                break;
                        }
                    }
                }
            }
        }
        return filteredSeatList;
    } 
    
    public ArrayList sortTable(ArrayList<SeatResult> seatList) {
        String sortType = sortTypeDropdown.getSelectedItem().toString();
        String sortDirection = sortDirectionDropdown.getSelectedItem().toString();
        
        sortPartyDropdown.setVisible(false);
        
        switch (sortType) {
            case "Name":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparing(SeatResult::getSeatName));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparing((SeatResult::getSeatName)).reversed());
                }
                break;
                
            case "Province":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparing(SeatResult::getProvinceName));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparing((SeatResult::getProvinceName)).reversed());
                }
                break;
                
            case "Region":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparing(SeatResult::getRegionName));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparing((SeatResult::getRegionName)).reversed());
                }
                break;
                
            case "Winner":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparing(SeatResult::getWinner));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparing((SeatResult::getWinner)).reversed());
                }
                break;
                
            case "Win Margin":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparingDouble(SeatResult::getWinMargin));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparingDouble((SeatResult::getWinMargin)).reversed());
                }
                break;

            case "Polls Reporting":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparingDouble(SeatResult::getPollsReporting));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparingDouble((SeatResult::getPollsReporting)).reversed());
                }
                break;

            case "Win Chance":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparingDouble(this::simpleWinProbabilities));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparingDouble(this::simpleWinProbabilities).reversed());
                }
                break;

            case "Party Result":
            sortPartyDropdown.setVisible(true);
            Party sortParty = (Party) sortPartyDropdown.getSelectedItem();
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparingDouble(Seat -> Seat.getPartyResult(sortParty)));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparing(seat -> seat.getPartyResult(sortParty), Comparator.reverseOrder()));
                }
                break;
        }
        return seatList;
    }
    
    private void updateFilterDropdowns() {
        String locationFilterType = locationFilterTypeDropdown.getSelectedItem().toString();
        
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        
        model.removeAllElements();
        
        switch (locationFilterType) {
            case "Region":
                model.addElement("All");
                
                for (int i = 0; i < Region.values().length; i++) {
                    model.addElement(Region.values()[i].getFullName());
                }
                locationFilterValueDropdown.setModel(model);
                break;

            case "Province":
                model.addElement("All");
                
                for (int i = 0; i < Province.values().length; i++) {
                    model.addElement(Province.values()[i].getFullName());
                }
                locationFilterValueDropdown.setModel(model);
                break;
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        seatTable = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        searchInput = new javax.swing.JTextField();
        locationFilterValueDropdown = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        resultFilterValueDropdown = new javax.swing.JComboBox<>();
        locationFilterTypeDropdown = new javax.swing.JComboBox<>();
        resultFilterTypeDropdown = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        sortTypeDropdown = new javax.swing.JComboBox<>();
        sortDirectionDropdown = new javax.swing.JComboBox<>();
        sortPartyDropdown = new javax.swing.JComboBox<>();
        refreshButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        seatTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        seatTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        seatTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(seatTable);

        jLabel1.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        jLabel1.setText("Search");

        searchInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                searchInputKeyTyped(evt);
            }
        });

        locationFilterValueDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        locationFilterValueDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                locationFilterValueDropdownItemStateChanged(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        jLabel2.setText("Filters");

        resultFilterValueDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                resultFilterValueDropdownItemStateChanged(evt);
            }
        });

        locationFilterTypeDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Region", "Province" }));
        locationFilterTypeDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                locationFilterTypeDropdownItemStateChanged(evt);
            }
        });

        resultFilterTypeDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Winner", "Second Place", "Third Place", "Fourth Place", "Fifth Place", "Sixth Place" }));
        resultFilterTypeDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                resultFilterTypeDropdownItemStateChanged(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        jLabel5.setText("Sorting");

        sortTypeDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Name", "Region", "Province", "Winner", "Win Margin", "Party Result", "Win Chance", "Polls Reporting" }));
        sortTypeDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                sortTypeDropdownItemStateChanged(evt);
            }
        });

        sortDirectionDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Ascending", "Descending" }));
        sortDirectionDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                sortDirectionDropdownItemStateChanged(evt);
            }
        });

        sortPartyDropdown.setModel(new DefaultComboBoxModel(Party.values()));
        sortPartyDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                sortPartyDropdownItemStateChanged(evt);
            }
        });

        refreshButton.setText("Refresh");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1138, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(searchInput, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(refreshButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(locationFilterTypeDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(resultFilterTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(locationFilterValueDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(resultFilterValueDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(193, 193, 193)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel5)
                            .addComponent(sortTypeDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(sortPartyDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sortDirectionDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(searchInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(28, 28, 28))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel5)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(sortDirectionDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(sortTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(sortPartyDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(locationFilterValueDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(locationFilterTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(resultFilterTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(resultFilterValueDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(refreshButton))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 387, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void searchInputKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchInputKeyTyped
        updateTable(liveResults);
    }//GEN-LAST:event_searchInputKeyTyped

    private void locationFilterTypeDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_locationFilterTypeDropdownItemStateChanged
        updateTable(liveResults);
        updateFilterDropdowns();
    }//GEN-LAST:event_locationFilterTypeDropdownItemStateChanged

    private void locationFilterValueDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_locationFilterValueDropdownItemStateChanged
        updateTable(liveResults);
    }//GEN-LAST:event_locationFilterValueDropdownItemStateChanged

    private void resultFilterTypeDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_resultFilterTypeDropdownItemStateChanged
        updateTable(liveResults);
        updateFilterDropdowns();
    }//GEN-LAST:event_resultFilterTypeDropdownItemStateChanged

    private void resultFilterValueDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_resultFilterValueDropdownItemStateChanged
        updateTable(liveResults);
    }//GEN-LAST:event_resultFilterValueDropdownItemStateChanged

    private void sortTypeDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_sortTypeDropdownItemStateChanged
        updateTable(liveResults);
    }//GEN-LAST:event_sortTypeDropdownItemStateChanged

    private void sortDirectionDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_sortDirectionDropdownItemStateChanged
        updateTable(liveResults);
    }//GEN-LAST:event_sortDirectionDropdownItemStateChanged

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        updateTable(liveResults);
    }//GEN-LAST:event_formWindowOpened

    private void sortPartyDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_sortPartyDropdownItemStateChanged
        updateTable(liveResults);
    }//GEN-LAST:event_sortPartyDropdownItemStateChanged

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        updateTable(liveResults);
    }//GEN-LAST:event_refreshButtonActionPerformed

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
            java.util.logging.Logger.getLogger(DetailedResults_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DetailedResults_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DetailedResults_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DetailedResults_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DetailedResults_jFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JComboBox<String> locationFilterTypeDropdown;
    private javax.swing.JComboBox<String> locationFilterValueDropdown;
    private javax.swing.JButton refreshButton;
    private javax.swing.JComboBox<String> resultFilterTypeDropdown;
    private javax.swing.JComboBox<String> resultFilterValueDropdown;
    private javax.swing.JTextField searchInput;
    private javax.swing.JTable seatTable;
    private javax.swing.JComboBox<String> sortDirectionDropdown;
    private javax.swing.JComboBox<String> sortPartyDropdown;
    private javax.swing.JComboBox<String> sortTypeDropdown;
    // End of variables declaration//GEN-END:variables
}
