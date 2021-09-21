package uk.gov.companieshouse.itemhandler.model;

import com.google.gson.Gson;

public class GeneralPartnerDetails implements BasicInformationIncludable {

    private Boolean includeBasicInformation;

    public Boolean getIncludeBasicInformation() {
        return includeBasicInformation;
    }

    public void setIncludeBasicInformation(Boolean includeBasicInformation) {
        this.includeBasicInformation = includeBasicInformation;
    }

    @Override
    public String toString() { return new Gson().toJson(this); }
}
