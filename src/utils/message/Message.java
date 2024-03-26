package utils.message;

public class Message {
    private String variableId;

    public Message(String variableId) {
        this.variableId = variableId;
    }

    @Override
    public String toString() {
        return "Variable ID : " + variableId + "\n";
    }

    public String getVariableId() {
        return variableId;
    }
}
