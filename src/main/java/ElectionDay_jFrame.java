import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.DefaultComboBoxModel;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ElectionDay_jFrame extends javax.swing.JFrame {
    private PollHashTable pollDatabase;
    private ArrayList<Seat> seatDatabase;
    private ArrayList<Seat> seatProjection;

    public ArrayList<SeatResult> liveResults = new ArrayList<>();
    public ArrayList<String> resultIds = new ArrayList<>();

    public ElectionDay_jFrame() {
        initComponents();
        loadResultIds();
        this.setTitle("Election Day Projections");
    }

    public void setDatabases(PollHashTable pollDatabase, ArrayList<Seat> seatDatabase, ArrayList<Seat> seatProjection) {
        this.pollDatabase = pollDatabase;
        this.seatDatabase = seatDatabase;
        this.seatProjection = seatProjection;
    }

    public void loadResultIds() {
        FileReader file = null;
        try {
            file = new FileReader("src/main/java/resultLinks.txt");
            BufferedReader input = new BufferedReader(file);
            String line = input.readLine();

            while (line != null) {
                resultIds.add(line);
                line = input.readLine();
            }
            input.close();
        } catch (Exception e) {
            e.getStackTrace();
        }
    }
    
    public HashMap CalculateAdjustmentFactor(ArrayList<Seat> seatResults, ArrayList<Seat> pollResults) {
        HashMap<Party, Double> averageDifference = new HashMap<>();
        for (int i = 0; i < Party.values().length; i++){
            averageDifference.put(Party.values()[i], 0.0);
        }
        // Go through 
        for (int i = 0; i < seatResults.size(); i++) {
            Seat curSeat = seatResults.get(i);
            Seat polledSeat = null;
            for (int j = 0; j < pollResults.size(); j++){
                if (pollResults.get(j).name.equals(curSeat.name)){
                    polledSeat = pollResults.get(j);
                } 
            }
            // Go through each party and map difference    
            for (int k = 0; k < Party.values().length; k++){
                Party curParty = Party.values()[k];
                double curDiff = curSeat.getPartyResult(curParty) / 100 - polledSeat.getPartyResult(curParty) / 100;
                averageDifference.put(curParty, averageDifference.get(curParty) + curDiff);
            } 
        }
        for (int i = 0; i < seatResults.size(); i++){
            Party partyToAdjust= Party.values()[i];
            averageDifference.put(partyToAdjust, averageDifference.get(partyToAdjust) / seatResults.size());
        }
        return averageDifference;
    }

    private ArrayList<SeatResult> getResults(ArrayList<String> ids) {
        ArrayList<SeatResult> resultsList = new ArrayList<>();

        String idList = String.join(",", ids);
        String resultTypes = String.join(",", Collections.nCopies(ids.size(), "1"));
        String cookieValue = "ItemList=" + idList + "&ResultTypeList=" + resultTypes;

        try {
            Connection.Response response = Jsoup.connect("https://enr.elections.ca/ElectoralDistricts.aspx?lang=e")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://www.elections.ca/")
                    .cookie("EDResults2", cookieValue)
                    .method(Connection.Method.GET)
                    .followRedirects(true)
                    .timeout(10000)
                    .execute();

            Document doc = response.parse();

            for (String id : ids) {

                HashMap<Party, Double> results = new HashMap<>();
                HashMap<Party, Double> resultsVotes = new HashMap<>();
                String riding = null;
                Province province = null;
                Region region = null;
                Double pollsReporting = null;

                for (Party party : Party.values()) {
                    resultsVotes.put(party, 0.0);
                }


                Element ridingElement = doc.selectFirst("#grdResultsucElectoralDistrictResult" + id + " caption");
                Element pollsReportingElement = doc.selectFirst("#litPollsReportingValueucElectoralDistrictResult" + id);
                Elements rows = doc.select("#grdResultsucElectoralDistrictResult" + id + " tr");

                if (ridingElement == null) continue;  // skip if not found
                riding = ridingElement.ownText();

                if (pollsReportingElement != null) {
                    Matcher matcher = Pattern.compile("\\(([^)]+%)\\)").matcher(pollsReportingElement.text());
                    if (matcher.find()) {
                        pollsReporting = Double.parseDouble(matcher.group(1).replace("%", "").trim());
                    }
                } else {
                    pollsReporting = 100.0;
                }

                for (Seat seat : seatDatabase) {
                    if (seat.name.equals(riding)) {
                        province = seat.province;
                        region = seat.region;
                    }
                }

                for (Element row : rows) {
                    if (row.select("th").size() > 0 || row.hasClass("tr_totals")) continue;

                    Elements tds = row.select("td");
                    if (tds.size() < 3) continue;

                    String partyValue = tds.get(0).text();
                    Party party = null;
                    Double votes = Double.valueOf(tds.get(2).text().replace(",", ""));

                    if (partyValue.contains("Conservative")) {
                        party = Party.CPC;
                    } else if (partyValue.contains("Liberal")) {
                        party = Party.LPC;
                    } else if (partyValue.contains("New Democratic")) {
                        party = Party.NDP;
                    } else if (partyValue.contains("Green")) {
                        party = Party.GPC;
                    } else if (partyValue.contains("People's")) {
                        party = Party.PPC;
                    } else if (partyValue.contains("Bloc")) {
                        party = Party.BQ;
                    } else {
                        continue;
                    }

                    resultsVotes.put(party, votes);
                }

                double totalVotes = resultsVotes.values().stream().mapToDouble(Double::doubleValue).sum();
                for (Party party : Party.values()) {
                    double percent = totalVotes > 0 ? (resultsVotes.get(party) / totalVotes) * 100 : 0;
                    results.put(party, percent);
                }

                SeatResult seatResult = new SeatResult(riding, region, province, results, resultsVotes, pollsReporting);

                if (seatResult.getSeatName().equals(testSeatResult.getSeatName())) {
                    seatResult = testSeatResult;
                }

                resultsList.add(seatResult);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resultsList;
    }

    private HashMap<Party, Double> getProjectionDifference(ArrayList<Seat> projection, ArrayList<SeatResult> results) {
        HashMap<Party, Double> resultsAverage = new HashMap<>();

        HashMap<Party, Double> numResults = new HashMap<>();

        for (Party party : Party.values()) {
            resultsAverage.put(party, 0.0);
            numResults.put(party, 0.0);
        }

        for (SeatResult seat : results) {
            HashMap<Party, Double> projectionAverage = calcVotePercentages(projection, seat.region);
            for (Party party : Party.values()) {
                if (projectionAverage.get(party) != 0.0 && seat.getPartyResult(party) != 0.0) {
                    resultsAverage.put(party, Math.pow(seat.getPollsReporting(), 2) * (seat.getPartyResult(party) / projectionAverage.get(party)) + resultsAverage.get(party));
                    numResults.put(party, numResults.get(party) + Math.pow(seat.getPollsReporting(), 2));
                } else {
                    resultsAverage.put(party, (seat.getPartyResult(party)) + resultsAverage.get(party));
                }
            }
        }

        for (Party party : Party.values()) {
            if (numResults.get(party) != 0) {
                resultsAverage.put(party, resultsAverage.get(party) / numResults.get(party));
            }
        }

        return resultsAverage;
    }

    public Seat applySwingToSeat(Seat seat, HashMap<Party, Double> swings) {
        HashMap<Party, Double> newResults = new HashMap<>();
        for (Party party : Party.values()) {
            double newPercentage = 0;
            if (swings.get(party) == 0) {
                newPercentage = seat.getPartyResult(party);
            } else {
                newPercentage = seat.getPartyResult(party) * swings.get(party);
            }

            if (newPercentage < 0) {
                newPercentage = 0;
            }

            newResults.put(party, newPercentage);
        }

        Seat newSeat = new Seat(seat.getSeatName(), seat.region, seat.province, newResults, seat.isIncumbent());

        return newSeat;
    }

    public ArrayList<Seat> getSeatProjection(ArrayList<Seat> oldSeatProjection, ArrayList<SeatResult> newResults, Region region) {
        ArrayList<Seat> newSeatList = new ArrayList<>();
        // If province is everything
        if (region == Region.CA) {
            // Iterate through every seat
            for (Seat seat : oldSeatProjection) {
                // Compare old results with polling average to determine swing (regional)
                HashMap<Party, Double> swing = getProjectionDifference(oldSeatProjection, newResults);

                // Apply the swing to the new seat and add it to the output
                Seat newSeat = applySwingToSeat(seat, swing);
                newSeatList.add(newSeat);

            }
            // If province is specific
        } else {
            for (Seat seat : oldSeatProjection) {
                Region seatRegion = seat.region;

                // Only add seats in the region inputted
                if (region == seatRegion) {
                    HashMap<Party, Double> swing = getProjectionDifference(oldSeatProjection, newResults);

                    Seat newSeat = applySwingToSeat(seat, swing);
                    newSeatList.add(newSeat);
                }
            }
        }
        return newSeatList;
    }

    public HashMap countSeatsWon(ArrayList<Seat> seatList) {
        HashMap<Party, Integer> results = new HashMap<>();

        for (int i = 0; i < Party.values().length; i++) {
            results.put(Party.values()[i], 0);
        }

        for (int i = 0; i < seatList.size(); i++) {
            Seat seat = seatList.get(i);
            Party winner = seat.getWinner();
            results.put(winner, results.get(winner) + 1);
        }

        return results;
    }

    public HashMap calcVotePercentages (ArrayList<Seat> seatList, Region region) {
        int totalSeats = 0;
        HashMap<Party, Double> votePercentages = new HashMap<>();

        // Fill the results with 0s to add to later
        for (int i = 0; i < Party.values().length; i++) {
            votePercentages.put(Party.values()[i], 0.0);
        }

        for (int i = 0; i < seatList.size(); i++) {
            Seat seat = seatList.get(i);

            if (region == Region.CA || region == seat.region) {
                for (int j = 0; j < Party.values().length; j++) {
                    Party currentParty = Party.values()[j];
                    votePercentages.put(currentParty, votePercentages.get(currentParty) + seat.getPartyResult(currentParty));
                }
                totalSeats++;
            }
        }

        for (int i = 0; i < Party.values().length; i++) {
            Double result = votePercentages.get(Party.values()[i]) / totalSeats;
            votePercentages.put(Party.values()[i], Math.round(result * 10.0) / 10.0);
        }

        return votePercentages;
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

        ElectionDay_jFrame.CustomRenderer renderer = new ElectionDay_jFrame.CustomRenderer(partyColours);

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

        ElectionDay_jFrame.CustomRenderer renderer = new ElectionDay_jFrame.CustomRenderer(partyColours);

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

    public void updateGraphs() {
        Region projectionRegion = Region.values()[regionDropdown.getSelectedIndex()];

        ArrayList<Seat> newSeatProjection = getSeatProjection(seatProjection, liveResults, projectionRegion);

        createSeatGraph(newSeatProjection);
        createVoteGraph(newSeatProjection);
    }

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

    public ArrayList<HashMap<Party, Double>> createTestObject() {
        HashMap<Party, Double> fakeResults = new HashMap<>();
        HashMap<Party, Double> fakeResultVotes = new HashMap<>();
        double[] results = {30.0, 60.0, 10.0, 0.0, 0.0, 0.0};
        for (Party party : Party.values()) {;
            fakeResults.put(party, results[party.ordinal()]);
            fakeResultVotes.put(party, results[party.ordinal()]);
        }
        ArrayList<HashMap<Party, Double>> ret = new ArrayList<>();
        ret.add(fakeResults);
        ret.add(fakeResultVotes);
        return ret;
    }

    SeatResult testSeatResult = new SeatResult("Ajax", Region.ON, Province.ON, createTestObject().get(0), createTestObject().get(1), 100);

    public HashMap<Party, Double> bayesianWinProbabilities(SeatResult seatResult) {
        HashMap<Party, Double> posteriorProbabilities = new HashMap<>();
        double pollsReporting = seatResult.getPollsReporting() / 100;
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
        System.out.println("priorProbabilities " + priorProbabilities) ;

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
    @SuppressWarnings("unchecked")
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        refreshButton = new javax.swing.JButton();
        regionDropdown = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        seatGraphPanel = new javax.swing.JPanel();
        voteGraphPanel = new javax.swing.JPanel();
        copyToClipboardButton = new javax.swing.JButton();
        displayDetailedButton = new javax.swing.JButton();
        battleboardButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        refreshButton.setText("Refresh Results");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
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
            .addGap(0, 260, Short.MAX_VALUE)
        );

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

        copyToClipboardButton.setText("Copy MapChart Data");
        copyToClipboardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyToClipboardButtonActionPerformed(evt);
            }
        });

        displayDetailedButton.setText("Display Detailed Projection");
        displayDetailedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayDetailedButtonActionPerformed(evt);
            }
        });

        battleboardButton.setText("Open Battleboard");
        battleboardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                battleboardButtonActionPerformed(evt);
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
                    .addComponent(refreshButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addGap(9, 9, 9)
                        .addComponent(regionDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(copyToClipboardButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(displayDetailedButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(battleboardButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(voteGraphPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(seatGraphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(regionDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(refreshButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(displayDetailedButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(battleboardButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(copyToClipboardButton)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void regionDropdownItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_regionDropdownItemStateChanged
        
    }//GEN-LAST:event_regionDropdownItemStateChanged

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        liveResults.clear();

        for (int i = 0; i < resultIds.size(); i += 4) {
            ArrayList<String> batch = new ArrayList<>();
            for (int j = i; j < i + 4 && j < resultIds.size(); j++) {
                batch.add(resultIds.get(j));
            }

            ArrayList<SeatResult> batchResults = getResults(batch);

            if (batchResults != null) {
                liveResults.addAll(batchResults);
            }
        }

        System.out.println("Live Results: " + liveResults.size());

        updateGraphs();
    }//GEN-LAST:event_refreshButtonActionPerformed

    private void copyToClipboardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyToClipboardButtonActionPerformed
        Region projectionRegion = Region.values()[regionDropdown.getSelectedIndex()];

        String mapChartData = getMapChartImportText(getSeatProjection(seatProjection, liveResults, projectionRegion));
        StringSelection stringSelection = new StringSelection(mapChartData);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }//GEN-LAST:event_copyToClipboardButtonActionPerformed

    private void displayDetailedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayDetailedButtonActionPerformed
        DetailedResults_jFrame DetailedResults_jFrame = new DetailedResults_jFrame();
        DetailedResults_jFrame.setVisible(true);
        DetailedResults_jFrame.setDatabases(pollDatabase, seatDatabase, getSeatProjection(seatProjection, liveResults, Region.CA));
        if (!regionDropdown.getSelectedItem().toString().equals("National")) {
            DetailedResults_jFrame.setLocationFilterValueIndex(regionDropdown.getSelectedIndex() + 1);
        }
    }//GEN-LAST:event_displayDetailedButtonActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        updateGraphs();
        System.out.println(bayesianWinProbabilities(testSeatResult));

    }//GEN-LAST:event_formWindowOpened

    private void battleboardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_battleboardButtonActionPerformed
        Battleboard_jFrame Battleboard_jFrame = new Battleboard_jFrame();
        Battleboard_jFrame.setVisible(true);
        Battleboard_jFrame.setDatabases(seatDatabase, seatProjection, liveResults);
    }//GEN-LAST:event_battleboardButtonActionPerformed

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
            java.util.logging.Logger.getLogger(ElectionDay_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ElectionDay_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ElectionDay_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ElectionDay_jFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ElectionDay_jFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton battleboardButton;
    private javax.swing.JButton copyToClipboardButton;
    private javax.swing.JButton displayDetailedButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton refreshButton;
    private javax.swing.JComboBox<String> regionDropdown;
    private javax.swing.JPanel seatGraphPanel;
    private javax.swing.JPanel voteGraphPanel;
    // End of variables declaration//GEN-END:variables
}
