import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.*;

public class ManagePolls_jFrame extends javax.swing.JFrame {
    private DefaultTableModel model;

    private PollHashTable pollDatabase;
    private ArrayList<Seat> seatDatabase;
    
    private Poll selectedPoll;
    
    public ManagePolls_jFrame() {
        initComponents();
        this.setTitle("Manage Polls");
                
        confirmButton.setVisible(false);
        cancelButton.setVisible(false);
        deleteAllButton.setVisible(false);
    }
    
    public void setDatabases(PollHashTable pollDatabase, ArrayList<Seat> seatDatabase) {
        this.pollDatabase = pollDatabase;
        this.seatDatabase = seatDatabase;
    }
    
    private void updateTable(PollHashTable pollList) {
        ArrayList<Poll> formattedPollList = new ArrayList<>();
        formattedPollList = sortTable(filterTable(pollList));
        
        String[] parties = new String[Party.values().length];
        
        for (int i = 0; i < Party.values().length; i++) {
            parties[i] = Party.values()[i].name();
        }
        
        String[] columnNames = new String[4 + Party.values().length];
        
        columnNames[0] = "Region";
        columnNames[1] = "Pollster";
        columnNames[2] = "Date";
        columnNames[3] = "Sample Size";
        
        for (int i = 4; i < columnNames.length; i++) {
            columnNames[i] = parties[i - 4];
        }

        model = new DefaultTableModel(columnNames, formattedPollList.size());
        pollTable.setModel(model);
        pollTable.setAutoCreateColumnsFromModel(true);

        int rowCounter = -1; 
        
      
        for (int i = 0; i < formattedPollList.size(); i++) {
            Poll poll = formattedPollList.get(i);

            rowCounter++;

            model.setValueAt(poll.getRegionName(), rowCounter, 0);
            model.setValueAt(poll.getPollsterName(), rowCounter, 1);
            model.setValueAt(poll.getDate(), rowCounter, 2);
            model.setValueAt(poll.getSampleSize(), rowCounter, 3);
            for (int j = 0; j < Party.values().length; j++) {
                model.setValueAt(poll.getPartyResult(Party.values()[j]), rowCounter, j + 4);
            }
        }
    }
    
    private void updateFilterDropdowns() {
        String filterType = filterTypeDropdown.getSelectedItem().toString();
        
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.removeAllElements();
        
        switch (filterType) {
            case "Region":
                model.addElement("All");
                
                for (int i = 0; i < Region.values().length; i++) {
                    model.addElement(Region.values()[i].getFullName());
                }
                filterValueDropdown.setModel(model);
                break;

            case "Pollster":
                model.addElement("All");
                
                for (int i = 0; i < Pollster.values().length; i++) {
                    model.addElement(Pollster.values()[i].getFullName());
                }
                filterValueDropdown.setModel(model);
                break;
        }
    }
    
    public ArrayList<Poll> filterTable(PollHashTable pollList) {
        ArrayList<Poll> filteredPollList = new ArrayList<>();
    
        String filterType = filterTypeDropdown.getSelectedItem().toString();
        String filterValue = filterValueDropdown.getSelectedItem().toString();
        
        for (int i = 0; i < pollList.buckets.length; i++) {
            for (int j = 0; j < pollList.buckets[i].size(); j++) {
                Poll poll = pollList.buckets[i].get(j);

                try {
                    String pollDay = String.format("%02d", poll.date.getDayOfMonth());
                    String pollMonth = String.format("%02d", poll.date.getMonthValue());
                    String pollYear = String.format("%04d", poll.date.getYear());

                    String filterDay = dayInput.getText();
                    String filterMonth = monthInput.getText();
                    String filterYear = yearInput.getText();
                    
                    dayInput.setBackground(Color.white);
                    monthInput.setBackground(Color.white);
                    yearInput.setBackground(Color.white);

                    if (filterDay.equals("DD")) {
                        filterDay = pollDay;
                    } else {
                        filterDay = String.format("%02d", Integer.valueOf(dayInput.getText()));
                    }

                    if (filterMonth.equals("MM")) {
                        filterMonth = pollMonth;
                    } else {
                        filterMonth = String.format("%02d", Integer.valueOf(monthInput.getText()));
                    }

                    if (filterYear.equals("YYYY")) {
                        filterYear = pollYear;
                    } else {
                        filterYear = String.format("%04d", Integer.valueOf(yearInput.getText()));
                    }

                    if (filterDay.equals(pollDay) && filterMonth.equals(pollMonth) && filterYear.equals(pollYear)) {
                        if (filterValue.equals("All")) {                        
                            filteredPollList.add(poll);
                        } else {
                            switch (filterType) {
                                case "Region":
                                    if (poll.getRegionName().equals(filterValue)) {
                                        filteredPollList.add(poll); 
                                    }
                                    break;

                                case "Pollster":
                                    if (poll.getPollsterName().equals(filterValue)) {
                                        filteredPollList.add(poll); 
                                    }
                                    break;
                            }
                        }
                    }
                } catch (Exception e) {
                    
                    dayInput.setBackground(new Color(255,209,209));
                    monthInput.setBackground(new Color(255,209,209));
                    yearInput.setBackground(new Color(255,209,209));
                }
            }
        }
        return filteredPollList;
    } 
    
    public ArrayList sortTable(ArrayList<Poll> pollList) {
        String sortType = sortTypeDropdown.getSelectedItem().toString();
        String sortDirection = sortDirectionDropdown.getSelectedItem().toString();
        
        switch (sortType) {
            case "Region":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(pollList, Comparator.comparing(Poll::getRegionName));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(pollList, Comparator.comparing((Poll::getRegionName)).reversed());
                }
                break;
                
            case "Date":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(pollList, Comparator.comparing(Poll::getDate));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(pollList, Comparator.comparing((Poll::getDate)).reversed());
                }
                break;
                
            case "Pollster":
                if (sortDirection.equals("Ascending")) {
                    Collections.sort(pollList, Comparator.comparing(Poll::getPollsterName));
                } else if (sortDirection.equals("Descending")) {
                    Collections.sort(pollList, Comparator.comparing((Poll::getPollsterName)).reversed());
                }
                break;
        }
        return pollList;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        pollTable = new javax.swing.JTable();
        addPollButton = new javax.swing.JButton();
        managePollButton = new javax.swing.JButton();
        deletePollButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        filterTypeDropdown = new javax.swing.JComboBox<>();
        filterValueDropdown = new javax.swing.JComboBox<>();
        sortDirectionDropdown = new javax.swing.JComboBox<>();
        sortTypeDropdown = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        dayInput = new javax.swing.JTextField();
        monthInput = new javax.swing.JTextField();
        yearInput = new javax.swing.JTextField();
        confirmButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        refreshButton = new javax.swing.JButton();
        deleteAllButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        pollTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        pollTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        pollTable.getTableHeader().setReorderingAllowed(false);
        pollTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                pollTableMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(pollTable);

        addPollButton.setText("Add Poll");
        addPollButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addPollButtonActionPerformed(evt);
            }
        });

        managePollButton.setText("Manage Selection");
        managePollButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                managePollButtonActionPerformed(evt);
            }
        });

        deletePollButton.setText("Delete Selection");
        deletePollButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deletePollButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Search by date:");
        jLabel1.setToolTipText("");

        jLabel2.setText("Filter by:");

        filterTypeDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Region", "Pollster" }));
        filterTypeDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                filterTypeDropdownItemStateChanged(evt);
            }
        });

        filterValueDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        filterValueDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                filterValueDropdownItemStateChanged(evt);
            }
        });

        sortDirectionDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Descending", "Ascending" }));
        sortDirectionDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                sortDirectionDropdownItemStateChanged(evt);
            }
        });

        sortTypeDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Date", "Pollster", "Region" }));
        sortTypeDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                sortTypeDropdownItemStateChanged(evt);
            }
        });

        jLabel3.setText("Sort by:");

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
        yearInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                yearInputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                yearInputFocusLost(evt);
            }
        });

        confirmButton.setText("Confirm");
        confirmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                confirmButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        refreshButton.setText("Refresh");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });

        deleteAllButton.setText("Delete All");
        deleteAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteAllButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1188, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(managePollButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deletePollButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(confirmButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteAllButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(refreshButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addPollButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(dayInput, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(monthInput, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(yearInput, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(209, 209, 209)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filterTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filterValueDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sortTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sortDirectionDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(filterTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(filterValueDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(yearInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(monthInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(dayInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(sortDirectionDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(sortTypeDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel3)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addPollButton)
                    .addComponent(managePollButton)
                    .addComponent(deletePollButton)
                    .addComponent(confirmButton)
                    .addComponent(refreshButton)
                    .addComponent(deleteAllButton)
                    .addComponent(cancelButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        updateFilterDropdowns();
        updateTable(pollDatabase);
    }//GEN-LAST:event_formWindowOpened

    private void addPollButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addPollButtonActionPerformed
        AddPoll_jFrame AddPoll_jFrame = new AddPoll_jFrame();
        AddPoll_jFrame.setVisible(true);
        AddPoll_jFrame.requestFocusInWindow();
        AddPoll_jFrame.setDatabases(pollDatabase, seatDatabase);
    }//GEN-LAST:event_addPollButtonActionPerformed

    private void managePollButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_managePollButtonActionPerformed
        AddPoll_jFrame AddPoll_jFrame = new AddPoll_jFrame();
        AddPoll_jFrame.setEditingPoll(selectedPoll);
        AddPoll_jFrame.setVisible(true);
        AddPoll_jFrame.requestFocusInWindow();
        AddPoll_jFrame.setDatabases(pollDatabase, seatDatabase);
        AddPoll_jFrame.setTitle("Editing Poll");
    }//GEN-LAST:event_managePollButtonActionPerformed

    private void pollTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pollTableMouseClicked
        try {
            int selectedRow = pollTable.getSelectedRow();
            String selectedPollster = (pollTable.getValueAt(selectedRow, 1)).toString();
            String selectedDate = (pollTable.getValueAt(selectedRow, 2)).toString();
            String selectedSampleSize = (pollTable.getValueAt(selectedRow, 3)).toString();
            String selectedRegion = (pollTable.getValueAt(selectedRow, 0)).toString();
            String selectedPollID = selectedPollster + selectedDate + selectedSampleSize + selectedRegion;
            selectedPoll = pollDatabase.get(selectedPollID);

            confirmButton.setVisible(false);
            cancelButton.setVisible(false);
        } catch (Exception e) {
            
        }
    }//GEN-LAST:event_pollTableMouseClicked

    private void filterTypeDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_filterTypeDropdownItemStateChanged
        updateFilterDropdowns();
        updateTable(pollDatabase);
    }//GEN-LAST:event_filterTypeDropdownItemStateChanged

    private void filterValueDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_filterValueDropdownItemStateChanged
        updateTable(pollDatabase);
    }//GEN-LAST:event_filterValueDropdownItemStateChanged

    private void sortTypeDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_sortTypeDropdownItemStateChanged
        updateTable(pollDatabase);
    }//GEN-LAST:event_sortTypeDropdownItemStateChanged

    private void sortDirectionDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_sortDirectionDropdownItemStateChanged
        updateTable(pollDatabase);
    }//GEN-LAST:event_sortDirectionDropdownItemStateChanged

    private void dayInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dayInputFocusGained
        if (dayInput.getText().equals("DD")) {
            dayInput.setText("");
        }
    }//GEN-LAST:event_dayInputFocusGained

    private void dayInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dayInputFocusLost
        if (dayInput.getText().equals("")) {
            dayInput.setText("DD");
        }
        updateTable(pollDatabase);
    }//GEN-LAST:event_dayInputFocusLost

    private void monthInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_monthInputFocusGained
        if (monthInput.getText().equals("MM")) {
            monthInput.setText("");
        }
    }//GEN-LAST:event_monthInputFocusGained

    private void monthInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_monthInputFocusLost
        if (monthInput.getText().equals("")) {
            monthInput.setText("MM");
        }
        updateTable(pollDatabase);
    }//GEN-LAST:event_monthInputFocusLost

    private void yearInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_yearInputFocusGained
        if (yearInput.getText().equals("YYYY")) {
            yearInput.setText("");
        }
    }//GEN-LAST:event_yearInputFocusGained

    private void yearInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_yearInputFocusLost
        if (yearInput.getText().equals("")) {
            yearInput.setText("YYYY");
        }
        updateTable(pollDatabase);
    }//GEN-LAST:event_yearInputFocusLost

    private void deletePollButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deletePollButtonActionPerformed
        confirmButton.setVisible(true);
        cancelButton.setVisible(true);
        deleteAllButton.setVisible(true);
    }//GEN-LAST:event_deletePollButtonActionPerformed

    private void confirmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confirmButtonActionPerformed
        pollDatabase.remove(selectedPoll.getID());
        confirmButton.setVisible(false);
        cancelButton.setVisible(false);
        deleteAllButton.setVisible(false);
        updateTable(pollDatabase);
        requestFocusInWindow();
    }//GEN-LAST:event_confirmButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        confirmButton.setVisible(false);
        cancelButton.setVisible(false);
        deleteAllButton.setVisible(false);
        requestFocusInWindow();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        updateTable(pollDatabase);
    }//GEN-LAST:event_refreshButtonActionPerformed

    private void deleteAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteAllButtonActionPerformed
        pollDatabase.removeAll(selectedPoll.getID());
        confirmButton.setVisible(false);
        cancelButton.setVisible(false);
        deleteAllButton.setVisible(false);
        updateTable(pollDatabase);
        requestFocusInWindow();
    }//GEN-LAST:event_deleteAllButtonActionPerformed

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
            java.util.logging.Logger.getLogger(ManagePolls_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ManagePolls_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ManagePolls_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ManagePolls_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ManagePolls_jFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addPollButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton confirmButton;
    private javax.swing.JTextField dayInput;
    private javax.swing.JButton deleteAllButton;
    private javax.swing.JButton deletePollButton;
    private javax.swing.JComboBox<String> filterTypeDropdown;
    private javax.swing.JComboBox<String> filterValueDropdown;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton managePollButton;
    private javax.swing.JTextField monthInput;
    private javax.swing.JTable pollTable;
    private javax.swing.JButton refreshButton;
    private javax.swing.JComboBox<String> sortDirectionDropdown;
    private javax.swing.JComboBox<String> sortTypeDropdown;
    private javax.swing.JTextField yearInput;
    // End of variables declaration//GEN-END:variables
}
