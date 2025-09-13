public enum Province {
    // Provinces
    CA("None"),
    NL("Newfoundland & Labrador"),
    NS("Nova Scotia"),
    PE("Prince Edward Island"),
    NB("New Brunswick"),
    QC("Quebec"),
    ON("Ontario"),
    MB("Manitoba"),
    SK("Saskatchewan"),
    AB("Alberta"),
    BC("British Columbia");
    
    // Attrnibutes
    public String fullName;
    
    // Constructor
    Province(String fullName) {
        this.fullName = fullName;
    }
    
    // Getter Methods
    public String getFullName() {
        return fullName;
    }
}