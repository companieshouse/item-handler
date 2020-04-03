package uk.gov.companieshouse.itemhandler.model;

import java.util.Objects;

public class ItemCosts {

    private String discountApplied;

    private String itemCost;

    private String calculatedCost;

    private ProductType productType;

    public ItemCosts() {
    }

    public ItemCosts(String discountApplied, String itemCost, String calculatedCost, ProductType productType) {
        this.discountApplied = discountApplied;
        this.itemCost = itemCost;
        this.calculatedCost = calculatedCost;
        this.productType = productType;
    }

    public String getDiscountApplied() {
        return discountApplied;
    }

    public void setDiscountApplied(String discountApplied) {
        this.discountApplied = discountApplied;
    }

    public String getItemCost() {
        return itemCost;
    }

    public void setItemCost(String itemCost) {
        this.itemCost = itemCost;
    }

    public String getCalculatedCost() {
        return calculatedCost;
    }

    public void setCalculatedCost(String calculatedCost) {
        this.calculatedCost = calculatedCost;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemCosts)) return false;
        ItemCosts itemCosts = (ItemCosts) o;
        return Objects.equals(discountApplied, itemCosts.discountApplied) &&
                Objects.equals(itemCost, itemCosts.itemCost) &&
                Objects.equals(calculatedCost, itemCosts.calculatedCost) &&
                productType == itemCosts.productType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(discountApplied, itemCost, calculatedCost, productType);
    }
}
