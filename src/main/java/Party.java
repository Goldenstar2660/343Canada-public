import java.awt.Color;

public enum Party {
    // Parties
    CPC("Conservative Party", new Color(15, 46, 82, 255), new Color(73, 98, 135, 128)),
    LPC("Liberal Party", new Color(215, 24, 33), new Color(230, 92, 99, 128)),
    NDP("New Democratic Party", new Color(228, 111, 11), new Color(238, 153, 82, 128)),
    BQ("Bloc Quebecois", new Color(55, 151, 240), new Color(112, 181, 244, 128)),
    GPC("Green Party", new Color(62, 155, 53), new Color(114, 184, 109, 128)),
    PPC("People's Party", new Color(74, 51, 137), new Color(122, 105, 169, 128));
    
    // Attributes
    public String fullName;
    public Color colour;
    public Color colourLeaning;
    
    // Constructor
    Party(String fullName, Color colour, Color colourLeaning) {
        this.fullName = fullName;
        this.colour = colour;
        this.colourLeaning = colourLeaning;
    }
    
    // Getter Methods
    public String getFullName() {
        return fullName;
    }
    
    public Color getColour() {
        return colour;
    }

    public Color getLeaningColour() {
        return colourLeaning;
    }
    
    public static String[] getFullNameList() {
        String[] fullNameList = new String[Party.values().length];
        for (int i = 0; i < Party.values().length; i++) {
            fullNameList[i] = Party.values()[i].fullName;
        }
        return fullNameList;
    }
}
