package model.beverage;

import model.ingredient.Ingredient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class Beverage {
    Map<String, Ingredient> ingredients = new HashMap<>();
    public abstract String getName();

    public void addIngredient(Ingredient ingredient) {
        this.ingredients.put(ingredient.getName(), ingredient);
    }

    public Map<String, Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Beverage other = (Beverage) obj;
        return this.getName().equals(other.getName());
    }

    @Override
    public String toString() {
        return this.getName() + "(" + this.ingredients + ")";
    }
}
