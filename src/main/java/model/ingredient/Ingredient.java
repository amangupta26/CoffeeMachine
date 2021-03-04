package model.ingredient;

public abstract class Ingredient {
    private int quantity = 0;

    // Have to be overriden by each ingredient
    public abstract String getName();

    public int getQuantity() {
        return quantity;
    }

    public void addQuantity(int addedQuantity) {
        this.quantity += addedQuantity;
    }

    public void removeQuantity(int removedQuantity) {
        this.quantity -= removedQuantity;
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
        final Ingredient other = (Ingredient) obj;
        return this.getName().equals(other.getName());
    }

    @Override
    public String toString() {
        return this.getName() + "(" + this.quantity + ")";
    }
}
