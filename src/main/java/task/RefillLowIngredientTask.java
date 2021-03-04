package task;

import model.coffeemachine.CoffeeMachine;

public class RefillLowIngredientTask implements Runnable {
    private CoffeeMachine coffeeMachine;
    private int quantityToRefill;

    public RefillLowIngredientTask(final CoffeeMachine coffeeMachine, int quantityToRefill) {
        this.coffeeMachine = coffeeMachine;
        this.quantityToRefill = quantityToRefill;
    }

    @Override
    public void run() {
        this.coffeeMachine.refillLowRawItems(quantityToRefill);
    }
}
