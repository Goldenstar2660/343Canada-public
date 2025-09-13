import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;

public class DetailedResults_jFrame extends javax.swing.JFrame {
    private DefaultTableModel model;

    private PollHashTable pollDatabase;
    private ArrayList<Seat> seatDatabase;
    private ArrayList<Seat> seatProjection;

    public DetailedResults_jFrame() {
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

    public void setDatabases(PollHashTable pollDatabase, ArrayList<Seat> seatDatabase, ArrayList<Seat> seatProjection) {
        this.pollDatabase = pollDatabase;
        this.seatDatabase = seatDatabase;
        this.seatProjection = seatProjection;
    }
    
    public void setLocationFilterValueIndex(int index) {
        locationFilterValueDropdown.setSelectedIndex(index);
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
    
    private void updateTable(ArrayList<Seat> seatList) {
        ArrayList<Seat> formattedSeatList = new ArrayList<>();
        formattedSeatList = sortTable(filterTable(seatList));
        
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

        model = new DefaultTableModel(columnNames, formattedSeatList.size());
        seatTable.setModel(model);
        seatTable.setAutoCreateColumnsFromModel(true);

        int rowCounter = -1; 
      
        for (int i = 0; i < formattedSeatList.size(); i++) {
            Seat seat = formattedSeatList.get(i);
            
            String seatHeldStatus = "Gain";
            if (seat.getWinner() == get2021Winner(seat)) {
                seatHeldStatus = "Hold";
            }

            rowCounter++;

            model.setValueAt(seat.getSeatName(), rowCounter, 0);
            model.setValueAt(seat.province, rowCounter, 1);
            model.setValueAt(seat.getWinner() + " " + seat.getWinType(), rowCounter, 2);
            model.setValueAt(Math.round(seat.getWinMargin() * 10.0) / 10.0, rowCounter, 3);
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
    
    public ArrayList<Seat> filterTable(ArrayList<Seat> seatList) {
        ArrayList<Seat> filteredSeatList = new ArrayList<>();
        
        int resultFilterNthPlace = resultFilterTypeDropdown.getSelectedIndex();
        String resultFilterValue = resultFilterValueDropdown.getSelectedItem().toString();
        
        String locationFilterType = locationFilterTypeDropdown.getSelectedItem().toString();
        String locationFilterValue = locationFilterValueDropdown.getSelectedItem().toString();
        
        String searchString = searchInput.getText().toLowerCase();
        
        for (int i = 0; i < seatList.size(); i++) {
            Seat seat = seatList.get(i);
            
            // if no search or matches search string
            if (seat.getSeatName().toLowerCase().contains(searchString) || searchString.equals("")) {
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
    
    public ArrayList sortTable(ArrayList<Seat> seatList) {
        String sortType = sortTypeDropdown.getSelectedItem().toString();
        String sortDirection = sortDirectionDropdown.getSelectedItem().toString();
        
        sortPartyDropdown.setVisible(false);
        
        switch (sortType) {
            case "Name":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparing(Seat::getSeatName));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparing((Seat::getSeatName)).reversed());
                }
                break;
                
            case "Province":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparing(Seat::getProvinceName));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparing((Seat::getProvinceName)).reversed());
                }
                break;
                
            case "Region":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparing(Seat::getRegionName));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparing((Seat::getRegionName)).reversed());
                }
                break;
                
            case "Winner":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparing(Seat::getWinner));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparing((Seat::getWinner)).reversed());
                }
                break;
                
            case "Win Margin":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(seatList, Comparator.comparingDouble(Seat::getWinMargin));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(seatList, Comparator.comparingDouble((Seat::getWinMargin)).reversed());
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

        sortTypeDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Name", "Region", "Province", "Winner", "Win Margin", "Party Result" }));
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
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(locationFilterTypeDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(resultFilterTypeDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(locationFilterValueDropdown, 0, 200, Short.MAX_VALUE)
                                    .addComponent(resultFilterValueDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addGap(63, 63, 63)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel5)
                            .addComponent(sortTypeDropdown, 0, 120, Short.MAX_VALUE)
                            .addComponent(sortPartyDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sortDirectionDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                            .addComponent(locationFilterValueDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(locationFilterTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(sortDirectionDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sortTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sortPartyDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(resultFilterTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(resultFilterValueDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void searchInputKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchInputKeyTyped
        updateTable(seatProjection);
    }//GEN-LAST:event_searchInputKeyTyped

    private void locationFilterTypeDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_locationFilterTypeDropdownItemStateChanged
        updateTable(seatProjection);
        updateFilterDropdowns();
    }//GEN-LAST:event_locationFilterTypeDropdownItemStateChanged

    private void locationFilterValueDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_locationFilterValueDropdownItemStateChanged
        updateTable(seatProjection);
    }//GEN-LAST:event_locationFilterValueDropdownItemStateChanged

    private void resultFilterTypeDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_resultFilterTypeDropdownItemStateChanged
        updateTable(seatProjection);
        updateFilterDropdowns();
    }//GEN-LAST:event_resultFilterTypeDropdownItemStateChanged

    private void resultFilterValueDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_resultFilterValueDropdownItemStateChanged
        updateTable(seatProjection);
    }//GEN-LAST:event_resultFilterValueDropdownItemStateChanged

    private void sortTypeDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_sortTypeDropdownItemStateChanged
        updateTable(seatProjection);
    }//GEN-LAST:event_sortTypeDropdownItemStateChanged

    private void sortDirectionDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_sortDirectionDropdownItemStateChanged
        updateTable(seatProjection);
    }//GEN-LAST:event_sortDirectionDropdownItemStateChanged

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        updateTable(seatProjection);
    }//GEN-LAST:event_formWindowOpened

    private void sortPartyDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_sortPartyDropdownItemStateChanged
        updateTable(seatProjection);
    }//GEN-LAST:event_sortPartyDropdownItemStateChanged

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
    private javax.swing.JComboBox<String> resultFilterTypeDropdown;
    private javax.swing.JComboBox<String> resultFilterValueDropdown;
    private javax.swing.JTextField searchInput;
    private javax.swing.JTable seatTable;
    private javax.swing.JComboBox<String> sortDirectionDropdown;
    private javax.swing.JComboBox<String> sortPartyDropdown;
    private javax.swing.JComboBox<String> sortTypeDropdown;
    // End of variables declaration//GEN-END:variables
}
