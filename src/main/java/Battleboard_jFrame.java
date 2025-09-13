import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class Battleboard_jFrame extends JFrame {
    private DefaultTableModel model;

    private PollHashTable pollDatabase;
    private ArrayList<Seat> seatDatabase;
    private ArrayList<Seat> seatProjection;
    private ArrayList<SeatResult> liveResults;

    public Battleboard_jFrame() {
        initComponents();
        this.setTitle("Detailed Projection");
        
        DefaultComboBoxModel model = new DefaultComboBoxModel();

        model.addElement("All");
        
        for (int i = 0; i < Party.values().length; i++) {
            model.addElement(Party.values()[i].getFullName());
        }
        
        targetDropdown.setModel(model);
    }

    public void setDatabases(ArrayList<Seat> seatDatabase, ArrayList<Seat> seatProjection, ArrayList<SeatResult> liveResults) {
        this.seatDatabase = seatDatabase;
        this.seatProjection = seatProjection;
        this.liveResults = liveResults;
    }
    
    public void setLocationFilterValueIndex(int index) {
        //locationFilterValueDropdown.setSelectedIndex(index);
    }
    
    private Party get2021Winner(Seat seat) {
        String seatName = seat.getSeatName();
        for (int i = 0; i < seatDatabase.size(); i++){
            Seat oldSeat = seatDatabase.get(i);
            if (seatName.equals(oldSeat.getSeatName())) {
                return oldSeat.getWinner();
            }
        }
        return null;
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

        double liveResultWeight = Math.pow(pollsReporting, 0.5);

        for (Party party : Party.values()) {
            blendedVoteShares.put(party, (1 - liveResultWeight) * seatInProjection.getPartyResult(party) + liveResultWeight * seatResult.getPartyResult(party));
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
                winProbabilities.put(party, ((blendedVoteShares.get(party) / 100) / (totalVoteShare / 100)) * (1 - winProbabilities.get(sortedPartyList.get(0))));
            } else if (seatInProjection.getPartyResult(party) == 0.0) {
                winProbabilities.put(party, 0.0);
            }
        }

        return winProbabilities.get(sortedPartyList.get(0)) ;
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
            double observedVoteShare = voteShare.get(party);
            double prior = (pollsReporting * observedVoteShare) + ((1 - pollsReporting) * historicalProjection);
            priorProbabilities.put(party, prior);
        }

        // Set up for bayes theorem; find the prior and likelihood with a hard coded variance
        // maybe find variance from previous election cycles
        HashMap<Party, Double> likelihoods = new HashMap<>();
        double variance = 0.05;
        for (Party party : Party.values()) {
            double observedVoteShare = voteShare.get(party);
            double predictedVoteShare = priorProbabilities.get(party);
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

    private class SeatTableCellRenderer extends DefaultTableCellRenderer {
        private ArrayList<Seat> seatsInTable;
        private ArrayList<SeatResult> liveResults;

        public SeatTableCellRenderer(ArrayList<Seat> seatsInTable, ArrayList<SeatResult> liveResults) {
            this.seatsInTable = seatsInTable;
            this.liveResults = liveResults;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            Seat seat = seatsInTable.get(row);

            if (isSelected) {
                cell.setBackground(table.getSelectionBackground());
                cell.setForeground(table.getSelectionForeground()); // when selected, use selection foreground
            } else {
                cell.setBackground(Color.WHITE);
                cell.setForeground(Color.BLACK); // default non-selected text color
            }

            if (!liveResults.isEmpty() && liveResults.get(row) != null) {
                for (SeatResult seatResult : liveResults) {
                    if (seat.getSeatName().equals(seatResult.getSeatName()) && seatResult.pollsReporting != 0.0) {
                        if (simpleWinProbabilities(seatResult) > 0.7) {
                            cell.setBackground(seatResult.getWinner().getColour());
                            cell.setForeground(Color.WHITE); // strong win: white text
                        } else {
                            cell.setBackground(seatResult.getWinner().getLeaningColour());
                            cell.setForeground(Color.BLACK); // lean: black text
                        }
                        break; // stop searching once found
                    }
                }
            }

            return cell;
        }
    }


    private void updateTable(ArrayList<Seat> seatList, ArrayList<SeatResult> liveResults) {
        Party selectedParty = Party.values()[partyDropdown.getSelectedIndex()];
        String selectedType = typeDropdown.getSelectedItem().toString();
        Boolean isReportingOnly = filterButton.isSelected();
        Party targetParty;

        if (targetDropdown.getSelectedIndex() != 0) {
            targetParty = Party.values()[targetDropdown.getSelectedIndex() - 1];
        } else {
            targetParty = null;
        }

        ArrayList<Seat> filteredSeatList = new ArrayList<>();

        if (selectedType.equals("Targets")) {
            for (Seat seat : seatList) {
                if (seat.getWinner() != selectedParty && (seat.getWinner() == targetParty || targetParty == null)) {
                    filteredSeatList.add(seat);
                }
            }
            Collections.sort(filteredSeatList, Comparator.comparingDouble(Seat -> Seat.getLossMargin(selectedParty)));
        } else if (selectedType.equals("Defending")) {
            for (Seat seat : seatList) {
                if (seat.getWinner() == selectedParty) {
                    filteredSeatList.add(seat);
                }
            }
            if (targetParty != null) {
                Collections.sort(filteredSeatList, Comparator.comparingDouble(Seat -> Seat.getLossMargin(targetParty)));
            } else {
                Collections.sort(filteredSeatList, Comparator.comparingDouble(Seat -> Seat.getWinMargin()));
            }
        }

        String[] parties = new String[Party.values().length];

        for (int i = 0; i < Party.values().length; i++) {
            parties[i] = Party.values()[i].name();
        }

        String[] columnNames = new String[5 + Party.values().length];

        columnNames[0] = "Name";
        columnNames[1] = "Province";
        columnNames[2] = "Winner";
        columnNames[3] = "Margin";
        columnNames[4] = "Gain?";

        for (int i = 5; i < columnNames.length; i++) {
            columnNames[i] = parties[i - 5];
        }

        model = new DefaultTableModel(columnNames, filteredSeatList.size());
        seatTable.setModel(model);
        seatTable.setAutoCreateColumnsFromModel(true);
        seatTable.setDefaultRenderer(Object.class, new SeatTableCellRenderer(filteredSeatList, liveResults));

        int rowCounter = -1;

        for (int i = 0; i < filteredSeatList.size(); i++) {
            Seat seat = filteredSeatList.get(i);
            Double margin = 0.0;
            if (selectedType.equals("Targets")) {
                margin = Math.round(seat.getLossMargin(selectedParty) * 10.0) / 10.0;
            } else {
                if (targetParty != null) {
                    margin = Math.round(seat.getLossMargin(targetParty) * 10.0) / 10.0;
                } else {
                    margin = Math.round(seat.getWinMargin() * 10.0) / 10.0;
                }
            }

            String seatHeldStatus = "Gain";
            if (seat.getWinner() == get2021Winner(seat)) {
                seatHeldStatus = "Hold";
            }

            rowCounter++;

            model.setValueAt(seat.getSeatName(), rowCounter, 0);
            model.setValueAt(seat.province, rowCounter, 1);
            model.setValueAt(seat.getWinner() + " " + seat.getWinType(), rowCounter, 2);
            model.setValueAt(margin, rowCounter, 3);
            model.setValueAt(seatHeldStatus, rowCounter, 4);

            seatTable.getColumnModel().getColumn(0).setPreferredWidth(250);
            seatTable.getColumnModel().getColumn(1).setPreferredWidth(20);
            seatTable.getColumnModel().getColumn(2).setPreferredWidth(50);
            seatTable.getColumnModel().getColumn(3).setPreferredWidth(10);
            seatTable.getColumnModel().getColumn(4).setPreferredWidth(10);

            for (int j = 0; j < Party.values().length; j++) {
                Double result = seat.getPartyResult(Party.values()[j]);
                model.setValueAt(Math.round(result * 10.0) / 10.0, rowCounter, j + 5);
                seatTable.getColumnModel().getColumn(j + 5).setPreferredWidth(10);
            }
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        seatTable = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        searchInput = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        partyDropdown = new javax.swing.JComboBox<>();
        typeDropdown = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        targetDropdown = new javax.swing.JComboBox<>();
        refreshButton = new javax.swing.JButton();
        filterButton = new javax.swing.JToggleButton();

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

        jLabel2.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        jLabel2.setText("Party");

        partyDropdown.setModel(new DefaultComboBoxModel(Party.getFullNameList()));
        partyDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                partyDropdownItemStateChanged(evt);
            }
        });

        typeDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Targets", "Defending" }));
        typeDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                typeDropdownItemStateChanged(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        jLabel5.setText("Target");

        targetDropdown.setModel(new DefaultComboBoxModel(Party.getFullNameList()));
        targetDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                targetDropdownItemStateChanged(evt);
            }
        });

        refreshButton.setText("Refresh");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });

        filterButton.setText("Reported Only");
        filterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(searchInput, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(63, 63, 63)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(partyDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 197, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(typeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(62, 62, 62)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(targetDropdown, 0, 222, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(refreshButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filterButton)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(searchInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(partyDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(typeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(targetDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 394, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(refreshButton)
                    .addComponent(filterButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void searchInputKeyTyped(KeyEvent evt) {//GEN-FIRST:event_searchInputKeyTyped
        updateTable(seatDatabase, liveResults);
    }//GEN-LAST:event_searchInputKeyTyped

    private void partyDropdownItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_partyDropdownItemStateChanged
        updateTable(seatDatabase, liveResults);
    }//GEN-LAST:event_partyDropdownItemStateChanged

    private void typeDropdownItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_typeDropdownItemStateChanged
        updateTable(seatDatabase, liveResults);
    }//GEN-LAST:event_typeDropdownItemStateChanged

    private void targetDropdownItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_targetDropdownItemStateChanged
        updateTable(seatDatabase, liveResults);
    }//GEN-LAST:event_targetDropdownItemStateChanged

    private void formWindowOpened(WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        updateTable(seatDatabase, liveResults);
    }//GEN-LAST:event_formWindowOpened

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        updateTable(seatDatabase, liveResults);
    }//GEN-LAST:event_refreshButtonActionPerformed

    private void filterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_filterButtonActionPerformed

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
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DetailedResults_jFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(DetailedResults_jFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(DetailedResults_jFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(DetailedResults_jFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DetailedResults_jFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton filterButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JComboBox<String> partyDropdown;
    private javax.swing.JButton refreshButton;
    private javax.swing.JTextField searchInput;
    private javax.swing.JTable seatTable;
    private javax.swing.JComboBox<String> targetDropdown;
    private javax.swing.JComboBox<String> typeDropdown;
    // End of variables declaration//GEN-END:variables
}
