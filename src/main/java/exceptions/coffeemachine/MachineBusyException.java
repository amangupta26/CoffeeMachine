package exceptions.coffeemachine;

public class MachineBusyException extends Exception {
    public MachineBusyException(String message) {
        super(message);
    }
}
