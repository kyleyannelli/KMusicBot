package Exceptions;

public class EmptyParameterException extends Exception {
    private final String causalParameterName;
    public EmptyParameterException(String paramName) {
        super("Parameter \"" + paramName + "\" is not in the interaction!");
        this.causalParameterName = paramName;
    }

    public String getCausalParameterName() {
        return causalParameterName;
    }
}
