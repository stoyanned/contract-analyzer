package com.softwareag.metering.contracts;

import java.util.List;

public class Contract {
    private String contractNumber;
    private long startDate;
    private long endDate;
    private String customerId;
    private String customerName;
    private String billingSystemId;
    private List<Environment> environments;
    private List<String> relatedContracts;

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getBillingSystemId() {
        return billingSystemId;
    }

    public void setBillingSystemId(String billingSystemId) {
        this.billingSystemId = billingSystemId;
    }

    public List<Environment> getEnvironments() {
        return  environments;
    }

    public void setEnvironments(List<Environment> environments) {
        this.environments = environments;
    }

    public List<String> getRelatedContracts() {
        return relatedContracts;
    }

    public void setRelatedContracts(List<String> relatedContracts) {
        this.relatedContracts = relatedContracts;
    }

    public Contract(String customerId, List<Environment> environments) {
        this.customerId = customerId;
        this.environments = environments;
    }
    @Override
    public String toString() {
        return "Contract {" +
                "contractNumber='" + contractNumber + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", customerId='" + customerId + '\'' +
                ", customerName='" + customerName + '\'' +
                ", environments=" + environments +
                '}';
    }
}
