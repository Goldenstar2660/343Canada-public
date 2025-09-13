public enum Pollster {
    // Pollsters
    LEGER("Leger", 5.0),
    NANOS("Nanos Research", 4.0),
    IPSOS("Ipsos", 4.0),
    EKOS("EKOS", 2.0),
    ABACUS("Abacus Data", 3.0),
    MAINSTREET("Mainstreet Research", 3.0),
    ANGUS("Angus Reid Institute", 2.0),
    PALLAS("Pallas Data", 2.0),
    RESEARCH("Research Co.", 2.0),
    LIAISON("Liaison Strategies", 2.0),
    INNOVATIVE("Innovative Research", 1.0),
    MQO("MQO Research", 1.0),
    POLLARA("Pollara", 1.0),
    CARDINAL("Cardinal Research", 1.0),
    RELAY("Relay Strategies", 1.0);
    
    // Attributes
    public String fullName;
    public Double weight;
    
    // Constructor 
    Pollster(String fullName, Double weight) {
        this.fullName = fullName;
        this.weight = weight;
    }
    
    // Getter Methods
    public String getFullName() {
        return fullName;
    }
    
    public static String[] getFullNameList() {
        String[] fullNameList = new String[Pollster.values().length];
        for (int i = 0; i < Pollster.values().length; i++) {
            fullNameList[i] = Pollster.values()[i].fullName;
        }
        return fullNameList;
    }
    
    public Double getWeight() {
        return weight;
    }
}
