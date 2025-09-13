public enum Region {
    // Regions
    CA("National", 0),
    ATL("Atlantic Canada", 1),
    QC("Quebec", 2),
    ON("Ontario", 3),
    MBSK("Manitoba/Saskatchewan", 4),
    AB("Alberta", 5),
    BC("British Columbia", 6);
    
    // Attributes
    public String fullName;
    public int bucket;
    
    // Constructor
    Region(String fullName, int bucket) {
        this.fullName = fullName;
        this.bucket = bucket;
    }
    
    // Getter Methods
    public String getFullName() {
        return fullName;
    }
    
    public static String[] getFullNameList() {
        String[] fullNameList = new String[Region.values().length];
        for (int i = 0; i < Region.values().length; i++) {
            fullNameList[i] = Region.values()[i].fullName;
        }
        return fullNameList;
    }
}
