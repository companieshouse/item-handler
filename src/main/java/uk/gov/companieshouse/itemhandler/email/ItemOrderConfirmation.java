package uk.gov.companieshouse.itemhandler.email;

import java.util.List;

public class ItemOrderConfirmation extends OrderConfirmation {
    private List<ItemDetails> itemDetails;
    private String totalFee;

    public List<ItemDetails> getItemDetails() {
        return itemDetails;
    }

    public void setItemDetails(List<ItemDetails> itemDetails) {
        this.itemDetails = itemDetails;
    }

    public String getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(String totalFee) {
        this.totalFee = totalFee;
    }
}
