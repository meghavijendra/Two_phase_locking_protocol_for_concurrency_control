package rigorous2pl_cautious;

public enum Constants {

	B("b"),C("c"),R("r"),W("w"),E("e"),
	READ("Read"),WRITE("Write"),
	ACTIVE("Active"),BLOCK("Block"),COMMIT("Commit"),ABORT("Abort"), OPENBRACKET("(");
	
	private String value;
	
	Constants(String value){
		this.value = value;
	}
	
	public String getValue() {
        return this.value;
    }
	
	public char getCharValue() {
		return value.charAt(0);
	}
}
