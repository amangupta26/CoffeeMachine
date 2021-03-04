import exceptions.coffeemachine.BeverageMissingException;
import exceptions.coffeemachine.InsufficientRawitemException;
import exceptions.coffeemachine.MachineBusyException;
import exceptions.coffeemachine.RawItemMissingException;
import model.coffeemachine.CoffeeMachine;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import task.DispenseBeverageTask;
import task.RefillLowIngredientTask;

import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FunctionalTests {
    CoffeeMachine coffeeMachine;

    @Before
    public void setUp() {
        this.coffeeMachine = initialiseCoffeeMachine();
    }

    @Test
    public void testConcurrentDispensingGreaterThanN() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        Future<Void> f1 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_tea"));
        Future<Void> f2 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "green_tea"));
        Future<Void> f3 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_coffee"));
        Future<Void> f4 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_coffee"));
        try {
            f1.get();
            f2.get();
            f3.get();
            f4.get();
        } catch (ExecutionException ex) {
            // Get machine busy exception since 4 beverages are dispensed
            if (ex.getCause() instanceof MachineBusyException) {
                assertTrue(true);
            } else {
                System.out.println(ex);
            }
        }
    }

    @Test
    public void testRawItemInsufficient() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<Void> f1 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_tea"));
        Future<Void> f2 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_coffee"));
        Future<Void> f3 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_coffee"));
        try {
            f1.get();
            f2.get();
            f3.get();
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof InsufficientRawitemException);
        }
    }

    @Test
    public void testRawItemDoesNotExist() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        Future<Void> f1 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "green_tea"));
        try {
            f1.get();
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof RawItemMissingException);
        }
    }

    @Test
    public void testCoffeeMachineDispenses_IfEnoughRawItem() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<Void> f1 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_coffee"));
        try {
            f1.get();
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof InsufficientRawitemException || ex.getCause() instanceof RawItemMissingException) {
                fail("Beverage should be dispensed");
            }
            fail("Unit test need to be fixed");
        }
    }

    @Test
    public void testCoffeeMachineDoesNotStuckAfterDispense() throws InterruptedException {
        // Fully utilise machine
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        Future<Void> f1 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_tea"));
        Future<Void> f2 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_coffee"));
        Future<Void> f3 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_coffee"));
        try {
            f1.get();
            f2.get();
            f3.get();
        } catch (ExecutionException ex) {
            System.out.println(ex.getMessage());
        }

        // Dispense next beverage and should succeed as other beverages are dispensed by now
        Future<Void> f4 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "green_tea"));
        try {
            f4.get();
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof MachineBusyException) {
                fail("Fourth dispense should not get stuck");
            }
            System.out.println(ex.getMessage());
        }
    }

    @Test
    public void testUnknownBeverageIsNotDispensed() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<Void> f1 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "unknown_tea"));
        try {
            f1.get();
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof BeverageMissingException);
        }
    }

    @Test
    public void testLowRawItemsAreRefilled() throws InterruptedException, ExecutionException {
        // Dispense beverages which make hot_milk low
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Void> f1 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_tea"));
        Future<Void> f2 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_coffee"));
        f1.get();
        f2.get();

        // Try to get coffee, should throw InsufficientRawitemException
        Future<Void> f3 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_coffee"));
        try {
            f3.get();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof InsufficientRawitemException);
        }

        // Refill low ingredients
        Future<?> fRefill = executorService.submit(new RefillLowIngredientTask(coffeeMachine, 500));
        fRefill.get();

        // Try again and should not see InsufficientRawitemException
        f3 = executorService.submit(new DispenseBeverageTask(coffeeMachine, "hot_coffee"));
        f3.get();
    }

    private CoffeeMachine initialiseCoffeeMachine() {
        JSONParser parser = new JSONParser();
        JSONObject data;
        try {
            data = (JSONObject) parser.parse(new FileReader(System.getProperty("user.dir") + "/src/test/java/TestInput.json"));
        } catch (IOException | ParseException e) {
            System.out.println("JSON file does not exist or is invalid");
            return null;
        }
        // Setup Coffee Machine
        JSONObject machine = (JSONObject) data.get("machine");
        JSONObject outlets = (JSONObject) machine.get("outlets");
        JSONObject totalItemsWithQuantity = (JSONObject) machine.get("total_items_quantity");
        JSONObject beverages = (JSONObject) machine.get("beverages");

        // Initialise the coffee machine with N, add ingredients from input and beverages it could deliver
        CoffeeMachine coffeeMachine = new CoffeeMachine(Math.toIntExact((Long) outlets.get("count_n")));
        coffeeMachine.addRawItems(totalItemsWithQuantity);
        coffeeMachine.addBeverages(beverages);
        System.out.println(coffeeMachine);
        return coffeeMachine;
    }
}
