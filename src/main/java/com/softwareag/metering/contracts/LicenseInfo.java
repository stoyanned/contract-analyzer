package com.softwareag.metering.contracts;

import java.util.List;
import java.util.Map;

public class LicenseInfo {
    private int row;
    private String unit;
    private List<Map<String, List<String>>> productCodes;

    public LicenseInfo(int row, String unit, List<Map<String, List<String>>> productCodes) {
        this.row = row;
        this.unit = unit;
        this.productCodes = productCodes;
    }

    public List<Map<String, List<String>>> getProductCodes() {
        return productCodes;
    }

    @Override
    public String toString() {
        return "LicenseInfo{" +
                "row=" + row +
                ", unit='" + unit + '\'' +
                ", productCodes=" + productCodes +
                '}';
    }
}
