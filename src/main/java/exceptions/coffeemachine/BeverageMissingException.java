package exceptions.coffeemachine;

public class BeverageMissingException extends Exception {
    public BeverageMissingException(String message) {
        super(message);
    }
}
