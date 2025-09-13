import java.awt.Color;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;

public class AddPoll_jFrame extends javax.swing.JFrame {
    public PollHashTable pollDatabase;
    public ArrayList<Seat> seatDatabase;
    public HashMap<Region, HashMap<Party, Double>> resultsToAdd = new HashMap<>();
    
    public javax.swing.JLabel[] partyLabels;
    public javax.swing.JTextField[] resultInputs;
    
    public int currentSelectedIndex;
    
    public Poll editingPoll = null;
    
    public int numParties = Party.values().length;
    
    public AddPoll_jFrame() {
        initComponents();
        this.setTitle("Add Poll");
              
        infoLabel.setVisible(false);
        
        // Arrays of result gui items
        partyLabels = new javax.swing.JLabel[]{party1Label, party2Label, party3Label, party4Label, party5Label, party6Label};
        resultInputs = new javax.swing.JTextField[]{party1ResultInput, party2ResultInput, party3ResultInput, party4ResultInput, party5ResultInput, party6ResultInput};
        for (int i = 0; i < partyLabels.length; i++) {
            partyLabels[i].setText(Party.values()[i].getFullName());
        }
        
        // Region selection
        currentSelectedIndex = regionDropdown.getSelectedIndex();
        resultsLabel.setText("Results for " + regionDropdown.getSelectedItem());
    }
    
    public void setDatabases(PollHashTable pollDatabase, ArrayList<Seat> seatDatabase) {
        this.pollDatabase = pollDatabase;
        this.seatDatabase = seatDatabase;
    }
    
    public void setTitle(String title) {
        titleLabel.setText(title);
    }
    
    public void setEditingPoll(Poll poll) {
        editingPoll = poll;
    }
    
    public void clearResultInputs() {
        for (int i = 0; i < resultInputs.length; i++) {
            resultInputs[i].setText("");
        }
    }
    
    public void showError(String error) {
        infoLabel.setVisible(true);
        infoLabel.setText(error);
        infoLabel.setForeground(Color.RED);
    }
    
    public void showMessage(String message) {
        infoLabel.setVisible(true);
        infoLabel.setText(message);
        infoLabel.setForeground(Color.BLACK);
    }
    
    public void clearError() {
        infoLabel.setVisible(false);
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
    
    public void updateResults() {
        // Stores results of current region
        HashMap<Party, Double> regionalResults = new HashMap<>();
        
        try {
            Region currentRegion = Region.values()[currentSelectedIndex];
            Region newRegion = Region.values()[regionDropdown.getSelectedIndex()];
            
            if (currentRegion == newRegion) {
                return;
            }
            
            int numZeros = 0;
            
            // Read from input fields
            for (int i = 0; i < resultInputs.length; i++) {
                if (resultInputs[i].getText().equals("")) {
                    regionalResults.put(Party.values()[i], 0.0);
                    numZeros++;
                } else {
                    regionalResults.put(Party.values()[i], Double.valueOf(resultInputs[i].getText()));
                }
            }
            
            // Check if all fields are 0
            if (numZeros < resultInputs.length) {
                // Add to hashmap with results from all regions
                resultsToAdd.put(currentRegion, regionalResults);
            }

            // Update input fields for new selected region
            resultsLabel.setText("Results for " + regionDropdown.getSelectedItem());
            currentSelectedIndex = regionDropdown.getSelectedIndex();
            clearError();

            if (resultsToAdd.get(newRegion) != null) {
                for (int i = 0; i < resultInputs.length; i++) {
                    resultInputs[i].setText(Double.toString(resultsToAdd.get(newRegion).get(Party.values()[i])));
                }            
            } else {
                clearResultInputs();
            }
        } catch (Exception e) {
            showError("Error: Invalid Inputs");
            regionDropdown.setSelectedIndex(currentSelectedIndex);
        }
    }
    
    public HashMap getVoteProjection(PollHashTable pollList, LocalDate date, Region region) {
        // Variables for weightage calculation
        int sampleSize;
        double pollsterWeight;
        long daysSince;
        double timeDecayWeight = 0.5;
        
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
                if (daysSince < 14) {
                    double pollWeight = pollsterWeight * Math.pow(sampleSize, 0.9) * Math.pow(timeDecayWeight, daysSince);
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
            weightedResults.put(Party.values()[i], Math.round(result * 10.0) / 10.0);
        }
        
        return weightedResults;
    }
    
    public void selectNextResult() {
        for (int i = 0; i < resultInputs.length; i++) {
            if (resultInputs[i].isFocusOwner()) { // Check which input is selected
                int nextIndex = (i + 1) % resultInputs.length; // Cycle to next
                resultInputs[nextIndex].requestFocus(); // Move focus
                break; // Stop loop after finding the focused field
            }
        }
    }
    

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        titleLabel = new javax.swing.JLabel();
        yearInput = new javax.swing.JTextField();
        monthInput = new javax.swing.JTextField();
        dayInput = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        pollsterDropdown = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        sampleSizeInput = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        regionDropdown = new javax.swing.JComboBox<>();
        resultsLabel = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        party2Label = new javax.swing.JLabel();
        party3Label = new javax.swing.JLabel();
        party2ResultInput = new javax.swing.JTextField();
        party3ResultInput = new javax.swing.JTextField();
        party4ResultInput = new javax.swing.JTextField();
        party5ResultInput = new javax.swing.JTextField();
        party6ResultInput = new javax.swing.JTextField();
        party4Label = new javax.swing.JLabel();
        party5Label = new javax.swing.JLabel();
        party6Label = new javax.swing.JLabel();
        party1ResultInput = new javax.swing.JTextField();
        party1Label = new javax.swing.JLabel();
        cancelButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(75, 22), new java.awt.Dimension(75, 22), new java.awt.Dimension(75, 22));
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(75, 22), new java.awt.Dimension(75, 22), new java.awt.Dimension(75, 22));
        infoLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        titleLabel.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        titleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        titleLabel.setText("Add Poll");

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

        jLabel2.setText("Date");

        pollsterDropdown.setModel(new DefaultComboBoxModel(Pollster.getFullNameList()));

        jLabel3.setText("Pollster");

        sampleSizeInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                sampleSizeInputFocusLost(evt);
            }
        });

        jLabel4.setText("Sample Size");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setText("General Info");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("Select Region");

        regionDropdown.setModel(new DefaultComboBoxModel(Region.getFullNameList()));
        regionDropdown.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                regionDropdownItemStateChanged(evt);
            }
        });

        resultsLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        resultsLabel.setText("Results for Atlantic Canada");

        party2Label.setText("jLabel9");

        party3Label.setText("jLabel10");

        party2ResultInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                party2ResultInputFocusLost(evt);
            }
        });
        party2ResultInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                partyResultInputKeyPressed(evt);
            }
        });

        party3ResultInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                party3ResultInputFocusLost(evt);
            }
        });
        party3ResultInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                partyResultInputKeyPressed(evt);
            }
        });

        party4ResultInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                party4ResultInputFocusLost(evt);
            }
        });
        party4ResultInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                partyResultInputKeyPressed(evt);
            }
        });

        party5ResultInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                party5ResultInputFocusLost(evt);
            }
        });
        party5ResultInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                partyResultInputKeyPressed(evt);
            }
        });

        party6ResultInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                party6ResultInputFocusLost(evt);
            }
        });
        party6ResultInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                partyResultInputKeyPressed(evt);
            }
        });

        party4Label.setText("jLabel11");

        party5Label.setText("jLabel12");

        party6Label.setText("jLabel13");

        party1ResultInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                party1ResultInputFocusLost(evt);
            }
        });
        party1ResultInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                partyResultInputKeyPressed(evt);
            }
        });

        party1Label.setText("jLabel8");

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        saveButton.setText("Save");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        infoLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        infoLabel.setText("jLabel7");
        infoLabel.setFocusable(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addComponent(titleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(pollsterDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(dayInput, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(monthInput, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(yearInput, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(sampleSizeInput, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addComponent(regionDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(party2Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(party3Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(party4Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(party5Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(party6Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(party1Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 115, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(party5ResultInput, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(party6ResultInput)
                            .addComponent(party4ResultInput)
                            .addComponent(party1ResultInput, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE)
                            .addComponent(party2ResultInput)
                            .addComponent(party3ResultInput)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cancelButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(infoLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(resultsLabel)
                            .addComponent(jLabel6)
                            .addComponent(jLabel5))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(titleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(yearInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(monthInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dayInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pollsterDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sampleSizeInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(regionDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(resultsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(party1ResultInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(party1Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(party2ResultInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(party2Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(party3ResultInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(party3Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(party4ResultInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(party4Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(party5ResultInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(party5Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(party6ResultInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(party6Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(infoLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(cancelButton)
                        .addComponent(saveButton))
                    .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void dayInputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dayInputFocusGained
        if (dayInput.getText().equals("DD")) {
            dayInput.setText("");
        }
    }//GEN-LAST:event_dayInputFocusGained

    private void dayInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dayInputFocusLost
        if (dayInput.getText().equals("")) {
            dayInput.setText("DD");
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
        }
    }//GEN-LAST:event_yearInputFocusLost

    private void sampleSizeInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_sampleSizeInputFocusLost
        checkNumericInput(sampleSizeInput);
    }//GEN-LAST:event_sampleSizeInputFocusLost

    private void regionDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_regionDropdownItemStateChanged
        updateResults();
    }//GEN-LAST:event_regionDropdownItemStateChanged

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        Region currentRegion = Region.values()[currentSelectedIndex];
        
        Poll pollToAdd = null;
        LocalDate date;
        Pollster pollster;
        int sampleSize;
        HashMap<Party, Double> regionalResults = new HashMap<>();
        
        try {
            // Read from input fields
            String day = String.format("%02d", Integer.valueOf(dayInput.getText()));
            String month = String.format("%02d", Integer.valueOf(monthInput.getText()));
            String year = String.format("%04d", Integer.valueOf(yearInput.getText()));
            String dateString = year + "-" + month + "-" + day;
            date = LocalDate.parse(dateString);
            
            pollster = Pollster.values()[pollsterDropdown.getSelectedIndex()];
            sampleSize = Integer.valueOf(sampleSizeInput.getText());
            
            int numZeros = 0;
            
            for (int i = 0; i < resultInputs.length; i++) {
                if (resultInputs[i].getText().equals("")) {
                    regionalResults.put(Party.values()[i], 0.0);
                    numZeros++;
                } else {
                    regionalResults.put(Party.values()[i], Double.valueOf(resultInputs[i].getText()));
                }
            }
            
            if (numZeros < resultInputs.length) {
                resultsToAdd.put(currentRegion, regionalResults);
            }
            
            if (editingPoll != null) {
                pollDatabase.remove(editingPoll.pollID);
            }

            // Add polls to database
            for (int i = 0; i < Region.values().length; i++) {
                Region region = Region.values()[i];
                
                if (resultsToAdd.get(region) != null) {
                    pollToAdd = new Poll(region, pollster, date, sampleSize, resultsToAdd.get(region), false);
                    if (pollDatabase.get(pollToAdd.getID()) != null) {
                        showError("Poll already added");
                    } else {
                        pollDatabase.add(pollToAdd);
                    }
                } else if (resultsToAdd.get(Region.CA) != null && editingPoll == null) {
                    HashMap<Party, Double> nationalProjection = getVoteProjection(pollDatabase, date, Region.CA);
                    HashMap<Party, Double> regionalProjection = getVoteProjection(pollDatabase, date, region);
                    HashMap<Party, Double> results = resultsToAdd.get(Region.CA);
                    HashMap<Party, Double> newRegionalResults = new HashMap<>();

                    for (int j = 0; j < Party.values().length; j++) { 
                        Party party = Party.values()[j];

                        Double swing = results.get(party) - nationalProjection.get(party);
                        Double newResult = Math.round((regionalProjection.get(party) + swing) * 10.0) / 10.0;

                        newRegionalResults.put(party, newResult);
                    }

                    pollToAdd = new Poll(region, pollster, date, sampleSize, newRegionalResults, true);

                    if (pollDatabase.get(pollToAdd.getID()) != null) {
                        showError("Poll already added");
                    } else {
                        pollDatabase.add(pollToAdd);
                    }
                }
            }
            
            showMessage("Successfully added " + pollToAdd.getID());
            
            this.dispose();
        } catch (Exception e) {
            showError("Error: Invalid Inputs");
        }        
    }//GEN-LAST:event_saveButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void party1ResultInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_party1ResultInputFocusLost
        checkNumericInput(party1ResultInput);
    }//GEN-LAST:event_party1ResultInputFocusLost

    private void party2ResultInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_party2ResultInputFocusLost
        checkNumericInput(party2ResultInput);
    }//GEN-LAST:event_party2ResultInputFocusLost

    private void party3ResultInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_party3ResultInputFocusLost
        checkNumericInput(party3ResultInput);
    }//GEN-LAST:event_party3ResultInputFocusLost

    private void party4ResultInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_party4ResultInputFocusLost
        checkNumericInput(party4ResultInput);
    }//GEN-LAST:event_party4ResultInputFocusLost

    private void party5ResultInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_party5ResultInputFocusLost
        checkNumericInput(party5ResultInput);
    }//GEN-LAST:event_party5ResultInputFocusLost

    private void party6ResultInputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_party6ResultInputFocusLost
        checkNumericInput(party6ResultInput);
    }//GEN-LAST:event_party6ResultInputFocusLost

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        // If window is opened to edit a poll
        System.out.println(editingPoll);
        
        if (editingPoll != null) {
            regionDropdown.setSelectedIndex(editingPoll.region.bucket);
            resultsLabel.setText("Results for " + regionDropdown.getSelectedItem());
            regionDropdown.setEnabled(false);
            
            for (int i = 0; i < resultInputs.length; i++) {
                resultInputs[i].setText(Double.toString(editingPoll.results.get(Party.values()[i])));
            }
            
            dayInput.setText(Integer.toString(editingPoll.date.getDayOfMonth()));
            monthInput.setText(Integer.toString(editingPoll.date.getMonthValue()));
            yearInput.setText(Integer.toString(editingPoll.date.getYear()));
            sampleSizeInput.setText(Integer.toString(editingPoll.getSampleSize()));
            
            for (int i = 0; i < Pollster.values().length; i++) {
                if (editingPoll.pollster.equals(Pollster.values()[i])) {
                    pollsterDropdown.setSelectedIndex(i);
                    break;
                }
            }
        }
    }//GEN-LAST:event_formWindowOpened

    private void partyResultInputKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_partyResultInputKeyPressed
        if (evt.getKeyCode() == 10) { // ENTER
            selectNextResult();
        }
    }//GEN-LAST:event_partyResultInputKeyPressed

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
            java.util.logging.Logger.getLogger(AddPoll_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AddPoll_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AddPoll_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AddPoll_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AddPoll_jFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField dayInput;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField monthInput;
    private javax.swing.JLabel party1Label;
    private javax.swing.JTextField party1ResultInput;
    private javax.swing.JLabel party2Label;
    private javax.swing.JTextField party2ResultInput;
    private javax.swing.JLabel party3Label;
    private javax.swing.JTextField party3ResultInput;
    private javax.swing.JLabel party4Label;
    private javax.swing.JTextField party4ResultInput;
    private javax.swing.JLabel party5Label;
    private javax.swing.JTextField party5ResultInput;
    private javax.swing.JLabel party6Label;
    private javax.swing.JTextField party6ResultInput;
    private javax.swing.JComboBox<String> pollsterDropdown;
    private javax.swing.JComboBox<String> regionDropdown;
    private javax.swing.JLabel resultsLabel;
    private javax.swing.JTextField sampleSizeInput;
    private javax.swing.JButton saveButton;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JTextField yearInput;
    // End of variables declaration//GEN-END:variables
}
