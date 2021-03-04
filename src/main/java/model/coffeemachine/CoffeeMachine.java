package model.coffeemachine;

import exceptions.coffeemachine.BeverageMissingException;
import exceptions.coffeemachine.InsufficientRawitemException;
import exceptions.coffeemachine.MachineBusyException;
import exceptions.coffeemachine.RawItemMissingException;
import lombok.Getter;
import lombok.ToString;
import model.beverage.Beverage;
import model.ingredient.Ingredient;
import model.ingredient.RawItem;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@ToString
public class CoffeeMachine {
    private final int N;
    // Semaphore to allow only fixed number of concurrent access to machine
    private final Semaphore concurrentSem;
    private final Map<String, RawItem> rawItems;
    private final Map<String, Beverage> beverages;

    Lock rawItemLock = new ReentrantLock();

    public CoffeeMachine(int N) {
        this.N = N;
        concurrentSem = new Semaphore(N);
        this.rawItems = new HashMap<>();
        this.beverages = new HashMap<>();
    }

    // Adds raw items to machine
    public void addRawItems(final JSONObject rawItems) {
        for (Object rawItemKey: rawItems.keySet()) {
            RawItem rawItem = new RawItem() {
                @Override
                public boolean isLow() {
                    return this.getQuantity() < 100;
                }

                @Override
                public String getName() {
                    return (String) rawItemKey;
                }
            };
            rawItem.addQuantity(Math.toIntExact((Long) rawItems.get(rawItemKey)));
            this.addRawItem(rawItem);
        }
    }

    private void addRawItem(final RawItem rawItem) {
        rawItemLock.lock();
        try {
            this.rawItems.put(rawItem.getName(), rawItem);
        } finally {
            rawItemLock.unlock();
        }

    }

    // Adds supported beverages by machine along with ingredient of them
    public void addBeverages(final JSONObject beverages) {
        for (Object beverageKey: beverages.keySet()) {
            Beverage beverage = new Beverage() {
                @Override
                public String getName() {
                    return (String) beverageKey;
                }
            };
            JSONObject ingredients = (JSONObject) beverages.get(beverageKey);
            for (Object ingredientKey: ingredients.keySet()) {
                Ingredient ingredient = new Ingredient() {
                    @Override
                    public String getName() {
                        return (String) ingredientKey;
                    }
                };
                ingredient.addQuantity(Math.toIntExact((Long) ingredients.get(ingredientKey)));
                beverage.addIngredient(ingredient);
            }
            this.addBeverage(beverage);
        }
    }

    private void addBeverage(final Beverage beverage) {
        if (this.beverages.containsKey(beverage.getName())) {
            return;
        }
        this.beverages.put(beverage.getName(), beverage);
    }

    // Method to refill all low items. Can be made more intelligent by refilling a particular item
    public void refillLowRawItems(int refillQuantity) {
        rawItemLock.lock();
        try {
            this.rawItems.values().stream().filter(RawItem::isLow).forEach(rawItem -> rawItem.addQuantity(refillQuantity));
        } finally {
            rawItemLock.unlock();
        }
    }

    // Method to determine whether enough raw items are available for the beverage
    private void areRawItemsSufficient(final Beverage beverage) throws InsufficientRawitemException {
        for (Map.Entry<String, Ingredient> entry: beverage.getIngredients().entrySet()) {
            if (this.rawItems.get(entry.getKey()).getQuantity() < entry.getValue().getQuantity()) {
                throw new InsufficientRawitemException("Item " + entry.getKey() + " is insufficient for beverage " + beverage.getName());
            }
        }
    }

    // Method to check whether machine could dispense the beverage
    private void checkBeverageAvailable(final String beverageName) throws BeverageMissingException {
        Beverage beverage = this.beverages.get(beverageName);
        if (beverage == null) {
            throw new BeverageMissingException("Beverage " + beverageName + " is not available");
        }
    }

    // Method to check all raw items are available in machine for a beverage
    private void checkRawItemsExist(Beverage beverage) throws RawItemMissingException {
        for(String ingredientName: beverage.getIngredients().keySet()) {
            if(!this.rawItems.containsKey(ingredientName)) {
                throw new RawItemMissingException("Raw Item is missing " + ingredientName);
            }
        }
    }

    /**
     * Fetch all raw items from machine for a beverage. It does following steps:
     * 1. Checks raw items exist for the beverage
     * 2. Checks enough raw items are available
     * 3. If 1 & 2 satisfies, reduces quantity of raw items from machine
     */
    private void fetchRawItems(final Beverage beverage) throws RawItemMissingException, InsufficientRawitemException {
        rawItemLock.lock();
        try {
            checkRawItemsExist(beverage);
            areRawItemsSufficient(beverage);
            for (Map.Entry<String, Ingredient> entry: beverage.getIngredients().entrySet()) {
                this.rawItems.compute(entry.getKey(), (rawItemName, rawItem) -> {
                    // Reduce raw item quantity
                    rawItem.removeQuantity(entry.getValue().getQuantity());
                    return rawItem;
                });
            }
        } finally {
            rawItemLock.unlock();
        }
    }

    /**
     * Method to dispense beverage from the machine. It does following steps:
     * 1. Checks whether beverage could be served by machine
     * 2. Fetches raw item
     */
    public void dispenseBeverage(final String beverageName) throws MachineBusyException, BeverageMissingException, RawItemMissingException, InsufficientRawitemException {
        boolean acquired = concurrentSem.tryAcquire();
        if (!acquired) {
            throw new MachineBusyException("Machine is busy");
        }
        try {
            checkBeverageAvailable(beverageName);
            fetchRawItems(this.beverages.get(beverageName));
            System.out.println("Here is your drink " + beverageName);
        } finally {
            concurrentSem.release();
        }
    }
}
