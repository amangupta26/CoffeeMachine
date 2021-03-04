package task;

import exceptions.coffeemachine.BeverageMissingException;
import exceptions.coffeemachine.InsufficientRawitemException;
import exceptions.coffeemachine.MachineBusyException;
import exceptions.coffeemachine.RawItemMissingException;
import model.coffeemachine.CoffeeMachine;

import java.util.concurrent.Callable;

public class DispenseBeverageTask implements Callable<Void> {
    private final CoffeeMachine coffeeMachine;
    private final String beverage;

    public DispenseBeverageTask(final CoffeeMachine coffeeMachine, final String beverage) {
        this.coffeeMachine = coffeeMachine;
        this.beverage = beverage;
    }

    @Override
    public Void call() throws MachineBusyException, RawItemMissingException, InsufficientRawitemException, BeverageMissingException {
        this.coffeeMachine.dispenseBeverage(this.beverage);
        return null;
    }
}
