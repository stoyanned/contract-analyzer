package com.softwareag.metering.contracts;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Environment {
    private String name;
    private String licenseInfo;
    private String environmentType;
    private String usageType;
    private boolean swAGCloud;
    
    public List<LicenseInfo> getLicenseInfo() {
        Gson gson = new Gson();
        try {
            JsonElement jsonElement = gson.fromJson(licenseInfo, JsonElement.class);
            if (jsonElement.isJsonArray()) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                List<LicenseInfo> licenseInfoList = new ArrayList<>();
                for (JsonElement element : jsonArray) {
                    if (element.isJsonObject()) {
                        JsonObject jsonObject = element.getAsJsonObject();
                        int row = jsonObject.has("row") ? jsonObject.get("row").getAsInt() : 0;
                        String unit = jsonObject.has("unit") ? jsonObject.get("unit").getAsString() : "";
                        JsonArray productCodesArray = jsonObject.getAsJsonArray("productCodes");
                        List<Map<String, List<String>>> productCodesList = parseProductCodes(productCodesArray);
                        LicenseInfo licenseInfo = new LicenseInfo(row, unit, productCodesList);
                        licenseInfoList.add(licenseInfo);
                    }
                }
                return licenseInfoList;
            } else if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                int row = jsonObject.has("row") ? jsonObject.get("row").getAsInt() : 0;
                String unit = jsonObject.has("unit") ? jsonObject.get("unit").getAsString() : "";
                JsonArray productCodesArray = jsonObject.getAsJsonArray("productCodes");
                List<Map<String, List<String>>> productCodesList = parseProductCodes(productCodesArray);
                LicenseInfo licenseInfo = new LicenseInfo(row, unit, productCodesList);
                return List.of(licenseInfo);
            } else {
                return null;
            }
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    private List<Map<String, List<String>>> parseProductCodes(JsonElement productCodesElement) {
        List<Map<String, List<String>>> productCodesList = new ArrayList<>();

        if (productCodesElement == null) {
            return productCodesList;
        }

        if (productCodesElement.isJsonArray()) {
            JsonArray productCodesArray = productCodesElement.getAsJsonArray();
            for (JsonElement codeElement : productCodesArray) {
                if (codeElement.isJsonObject()) {
                    Map<String, List<String>> productCodeMap = getStringListMap(codeElement);
                    productCodesList.add(productCodeMap);
                } else if (codeElement.isJsonPrimitive()) {
                    String code = codeElement.getAsString();
                    Map<String, List<String>> productCodeMap = new HashMap<>();
                    List<String> codesList = new ArrayList<>();
                    codesList.add(code);
                    productCodeMap.put("default", codesList);
                    productCodesList.add(productCodeMap);
                }
            }
        } else if (productCodesElement.isJsonPrimitive()) {
            String code = productCodesElement.getAsString();
            Map<String, List<String>> productCodeMap = new HashMap<>();
            List<String> codesList = new ArrayList<>();
            codesList.add(code);
            productCodeMap.put("default", codesList);
            productCodesList.add(productCodeMap);
        }

        return productCodesList;
    }

    @NotNull
    private static Map<String, List<String>> getStringListMap(JsonElement codeElement) {
        JsonObject productCodeObject = codeElement.getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entrySet = productCodeObject.entrySet();
        Map<String, List<String>> productCodeMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            String type = entry.getKey();
            JsonElement codesElement = entry.getValue();
            if (codesElement.isJsonArray()) {
                List<String> codesList = new ArrayList<>();
                JsonArray codesArray = codesElement.getAsJsonArray();
                for (JsonElement code : codesArray) {
                    codesList.add(code.getAsString());
                }
                productCodeMap.put(type, codesList);
            } else if (codesElement.isJsonPrimitive()) {
                String code = codesElement.getAsString();
                List<String> codesList = new ArrayList<>();
                codesList.add(code);
                productCodeMap.put(type, codesList);
            }
        }
        return productCodeMap;
    }

    public String getName() {
        return name;
    }

    public boolean isSwAGCloud() {
        return swAGCloud;
    }


    @Override
    public String toString() {
        return "Environment{" +
                "name='" + name + '\'' +
                ", licenseInfo='" + licenseInfo + '\'' +
                ", environmentType='" + environmentType + '\'' +
                '}';
    }

    public String getEnvironmentType() {
        return environmentType;
    }
}
