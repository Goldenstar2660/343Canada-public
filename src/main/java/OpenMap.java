import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.*;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.Color;
import java.util.stream.Collectors;

public class OpenMap {
    private MapContent map;
    private JMapFrame frame;
    private ShapefileDataStore dataStore;
    private SimpleFeatureSource featureSource;

    // Hardcoded ridings for each party
    private final List<String> conservativeRidings = Arrays.asList(
        "Calgary Heritage",
        "Calgary Signal Hill",
        "Calgary Rocky Ridge",
        "Calgary Nose Hill",
        "Calgary Midnapore",
        "Calgary Forest Lawn",
        "Calgary Centre",
        "Calgary Confederation",
        "Calgary Shepard"
    );

    private final List<String> ndpRidings = Arrays.asList(
        "Edmonton Strathcona",
        "Winnipeg Centre",
        "Hamilton Centre",
        "Vancouver East",
        "Vancouver Kingsway"
    );

    private final List<String> blocRidings = Arrays.asList(
        "Beloeil-—Chambly",
        "Bécancour--Nicolet--Saurel",
        "Berthier--Maskinongé",
        "Brome--Missisquoi",
        "Brossard--Saint-Lambert"
    );

    private final List<String> greenRidings = Arrays.asList(
        "Saanich—Gulf Islands",
        "Fredericton",
        "Victoria"
    );

    public OpenMap() {
        // Set system property for GeoTools
        System.setProperty("org.geotools.referencing.forceXY", "true");
    }

    public void initializeMap() {
        try {
            // Get the shapefile from resources
            URL shapefileUrl = getClass().getClassLoader().getResource("FED_CA_2021_EN.shp");
            if (shapefileUrl == null) {
                System.out.println("Error: Could not find shapefile in resources");
                return;
            }

            // Open the shapefile
            dataStore = new ShapefileDataStore(shapefileUrl);
            dataStore.setCharset(StandardCharsets.UTF_8);
            
            // Get the feature source
            featureSource = dataStore.getFeatureSource();
            
            // Create a map content
            map = new MapContent();
            map.setTitle("Canadian Electoral Districts");
            
            // Create and configure the map frame
            frame = new JMapFrame(map);
            frame.setSize(800, 600);
            frame.enableToolBar(true);
            frame.enableStatusBar(true);
            
            // Get the bounds and set the display area
            ReferencedEnvelope bounds = featureSource.getBounds();
            if (bounds != null) {
                bounds.expandBy(bounds.getWidth() * 0.1, bounds.getHeight() * 0.1);
                frame.getMapPane().setDisplayArea(bounds);
            }
            
        } catch (IOException e) {
            System.err.println("Error initializing map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateMapWithResults() {
        try {
            // Get all regions from the shapefile
            List<String> allRegions = new ArrayList<>();
            try (SimpleFeatureIterator features = featureSource.getFeatures().features()) {
                while (features.hasNext()) {
                    var feature = features.next();
                    String edName = (String) feature.getAttribute("ED_NAMEE");
                    allRegions.add(edName);
                }
            }

            // Create style factory and filter factory
            StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
            FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2();
            
            // Create the style
            Style style = styleFactory.createStyle();
            FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
            
            // Add rules for each party
            addPartyRule(fts, styleFactory, filterFactory, conservativeRidings, Color.BLUE);
            addPartyRule(fts, styleFactory, filterFactory, ndpRidings, Color.ORANGE);
            addPartyRule(fts, styleFactory, filterFactory, blocRidings, new Color(64, 224, 208)); // Turquoise
            addPartyRule(fts, styleFactory, filterFactory, greenRidings, Color.GREEN);

            // Create a list of all ridings that have been assigned to parties
            List<String> assignedRidings = new ArrayList<>();
            assignedRidings.addAll(conservativeRidings);
            assignedRidings.addAll(ndpRidings);
            assignedRidings.addAll(blocRidings);
            assignedRidings.addAll(greenRidings);

            // Add default red rule for all other ridings
            Rule defaultRule = styleFactory.createRule();
            PolygonSymbolizer defaultSymbolizer = styleFactory.createPolygonSymbolizer();
            defaultSymbolizer.setFill(styleFactory.createFill(filterFactory.literal(Color.RED)));
            defaultSymbolizer.setStroke(styleFactory.createStroke(filterFactory.literal(Color.BLACK), filterFactory.literal(1.0)));
            defaultRule.symbolizers().add(defaultSymbolizer);

            // Create a filter to exclude already assigned ridings
            List<Filter> excludeFilters = assignedRidings.stream()
                .map(riding -> filterFactory.not(
                    filterFactory.equals(
                        filterFactory.property("ED_NAMEE"),
                        filterFactory.literal(riding)
                    )
                ))
                .collect(Collectors.toList());

            // Combine all exclude filters with AND
            Filter finalFilter = filterFactory.and(excludeFilters);
            defaultRule.setFilter(finalFilter);
            fts.rules().add(defaultRule);

            // Add the feature type style to the main style
            style.featureTypeStyles().add(fts);
            
            // Create a layer with the feature source and style
            Layer layer = new FeatureLayer(featureSource, style);
            
            // Clear existing layers and add the new one
            map.layers().clear();
            map.addLayer(layer);
            
            // Refresh the display
            frame.getMapPane().repaint();
            
        } catch (IOException e) {
            System.err.println("Error updating map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addPartyRule(FeatureTypeStyle fts, StyleFactory styleFactory, 
                            FilterFactory2 filterFactory, List<String> regions, Color color) {
        if (!regions.isEmpty()) {
            Rule rule = styleFactory.createRule();
            PolygonSymbolizer symbolizer = styleFactory.createPolygonSymbolizer();
            symbolizer.setFill(styleFactory.createFill(filterFactory.literal(color)));
            symbolizer.setStroke(styleFactory.createStroke(filterFactory.literal(Color.BLACK), filterFactory.literal(1.0)));
            rule.symbolizers().add(symbolizer);
            
            List<Filter> filters = regions.stream()
                .map(region -> filterFactory.equals(
                    filterFactory.property("ED_NAMEE"),
                    filterFactory.literal(region)
                ))
                .collect(Collectors.toList());
            
            Filter combinedFilter = filterFactory.or(filters);
            rule.setFilter(combinedFilter);
            fts.rules().add(rule);
        }
    }

    public void show() {
        if (frame != null) {
            frame.setVisible(true);
        }
    }

    public void dispose() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        if (dataStore != null) {
            dataStore.dispose();
            dataStore = null;
        }
        if (map != null) {
            map.dispose();
            map = null;
        }
    }
}
