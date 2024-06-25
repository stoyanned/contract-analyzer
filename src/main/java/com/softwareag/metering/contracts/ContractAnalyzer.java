package com.softwareag.metering.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.reflect.Type;
import java.util.stream.Collectors;

public class ContractAnalyzer {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        String token = TMSAuth.getTokenFromKeycloak();
        if (token == null) {
            System.out.println("Failed to obtain authentication token. Exiting...");
            return;
        }

        List<String> contractIds = getContractIds(token);
        if (contractIds == null) {
            System.out.println("Failed to retrieve contract IDs. Exiting...");
            return;
        }
        System.out.println(contractIds);

        List<Contract> contracts = getContractDetails(token, contractIds);
//        System.out.println(contracts);

        analyzeContracts(contracts);
        generalReport(contracts, "generalReport.csv");
        analyzeContractsAndExport(contracts, "onPremWithoutView.csv");
        exportMissingView(contracts, "missingView.csv");
        exportDuplicateEnvironments(contracts, "duplicates.csv");

        Properties emailProps = new Properties();

        try (InputStream input = ContractAnalyzer.class.getClassLoader().getResourceAsStream("email.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find email.properties");
                return;
            }
            emailProps.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        String host = emailProps.getProperty("email.host");
        String port = emailProps.getProperty("email.port");
        String username = emailProps.getProperty("email.username");
        String password = emailProps.getProperty("email.password");
        String toAddress = "stne@softwareag.com";
        String subject = "TMS Contracts Reports";
        String message = "Please find the attached reports.";

        List<File> attachments = Arrays.asList(
                new File("generalReport.csv"),
                new File("onPremWithoutView.csv"),
                new File("missingView.csv"),
                new File("duplicates.csv")
        );

        try {
            EmailSender.sendEmailWithAttachments(host, port, username, password, toAddress, subject, message, attachments);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static List<String> getContractIds(String token) throws IOException {
        String url = "https://myconsole.softwareag.cloud/tms/contracts/";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", "Bearer " + token);

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                Type listType = new TypeToken<List<String>>() {}.getType();
                return gson.fromJson(reader, listType);
            }
        }
        return null;
    }

    private static List<Contract> getContractDetails(String token, List<String> contractIds) throws IOException {
        String url = "https://myconsole.softwareag.cloud/tms/contracts/ids/";
        List<Contract> allContracts = new ArrayList<>();
        int batchSize = 200;

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(30000)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
            for (int i = 0; i < contractIds.size(); i += batchSize) {
                int end = Math.min(i + batchSize, contractIds.size());
                List<String> batch = contractIds.subList(i, end);
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Authorization", "Bearer " + token);
                httpPost.setHeader("Content-Type", "application/json");

                String requestBody = gson.toJson(batch);
                httpPost.setEntity(new StringEntity(requestBody));

                System.out.println("Sending request with batch: " + batch);

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                            StringBuilder jsonResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                jsonResponse.append(line);
                            }

                            // Log the raw JSON response
                            System.out.println("JSON Response: " + jsonResponse);

                            try {
                                // Assuming the response could be an object containing a list of contracts or directly a list of contracts
                                JsonElement jsonElement = JsonParser.parseString(jsonResponse.toString());
                                System.out.println("Parsed JSON element: " + jsonElement.toString());
                                if (jsonElement.isJsonArray()) {
                                    // The response is a JSON array of Contract objects
                                    Type listType = new TypeToken<List<Contract>>() {}.getType();
                                    List<Contract> contracts = gson.fromJson(jsonElement, listType);
                                    if (contracts != null) {
                                        allContracts.addAll(contracts);
                                    }
                                } else if (jsonElement.isJsonObject()) {
                                    // The response is a JSON object that contains a list of Contract objects
                                    Type responseType = new TypeToken<Map<String, List<Contract>>>() {}.getType();
                                    Map<String, List<Contract>> responseMap = gson.fromJson(jsonElement, responseType);
                                    List<Contract> contracts = responseMap.get("contracts"); // Adjust the key based on the actual JSON structure
                                    if (contracts != null) {
                                        allContracts.addAll(contracts);
                                    }
                                } else {
                                    System.err.println("Unexpected JSON structure: " + jsonElement);
                                }
                            } catch (JsonSyntaxException e) {
                                System.err.println("Failed to parse JSON response: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    } else {
                        System.err.println("Response entity is null");
                    }
                } catch (SocketTimeoutException e) {
                    System.err.println("Request timed out: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Total contracts retrieved: " + allContracts.size());
        return allContracts;
    }

    public static void analyzeContracts(List<Contract> contracts) {

        for (Contract contract : contracts) {
            if (contract.getEnvironments() == null || contract.getEnvironments().isEmpty()) {
                continue;
            }

            String customerId = contract.getCustomerId();
            String contractId = contract.getContractNumber();

            boolean hasViewProductCode = false;
            for (Environment environment : contract.getEnvironments()) {
                List<LicenseInfo> licenseInfoList = environment.getLicenseInfo();
                if (hasViewProductCode(licenseInfoList)) {
                    hasViewProductCode = true;
                    break;
                }
            }

            if (hasViewProductCode) {
                boolean allEnvironmentsHaveView = true;
                for (Environment environment : contract.getEnvironments()) {
                    List<LicenseInfo> licenseInfoList = environment.getLicenseInfo();
                    if (!hasViewProductCode(licenseInfoList)) {
                        allEnvironmentsHaveView = false;
                        break;
                    }
                }

                if (allEnvironmentsHaveView) {
                    System.out.println("Customer " + customerId + ", Contract " + contractId + ": All environments have VIEW product code");
                } else {
                    System.out.println("Customer " + customerId + ", Contract " + contractId + ": Some environments do not have VIEW product code");
                }
            }
        }
    }

    private static boolean hasViewProductCode(List<LicenseInfo> licenseInfoList) {
        if (licenseInfoList == null) {
            return false;
        }

        for (LicenseInfo licenseInfo : licenseInfoList) {
            List<Map<String, List<String>>> productCodes = licenseInfo.getProductCodes();
            if (productCodes != null) {
                for (Map<String, List<String>> productCodeMap : productCodes) {
                    for (List<String> codes : productCodeMap.values()) {
                        if (codes != null && codes.contains("VIEW")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void generalReport(List<Contract> contracts, String filePath) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Customer ID,Customer Name,Contract Number,Start Date,End Date,Environment,Product Code");

            for (Contract contract : contracts) {
                String customerId = contract.getCustomerId();
                String customerName = contract.getCustomerName();
                String contractNumber = contract.getContractNumber();
                Date startDate = new Date(contract.getStartDate());
                Date endDate = new Date(contract.getEndDate());

                List<Environment> environments = contract.getEnvironments();
                if (environments == null || environments.isEmpty()) {
                    String line = String.join(",", customerId, "\"" + customerName + "\"", contractNumber,
                            dateFormat.format(startDate), dateFormat.format(endDate), "none", "none");
                    writer.println(line);
                } else {
                    for (Environment environment : environments) {
                        String environmentName = environment.getName();
                        List<LicenseInfo> licenseInfoList = environment.getLicenseInfo();
                        if (licenseInfoList != null) {
                            for (LicenseInfo licenseInfo : licenseInfoList) {
                                List<Map<String, List<String>>> productCodes = licenseInfo.getProductCodes();
                                if (productCodes != null) {
                                    for (Map<String, List<String>> productCodeMap : productCodes) {
                                        for (List<String> codes : productCodeMap.values()) {
                                            if (codes != null) {
                                                for (String productCode : codes) {
                                                    String line = String.join(",", customerId, "\"" + customerName + "\"", contractNumber,
                                                            dateFormat.format(startDate), dateFormat.format(endDate), environmentName, productCode);
                                                    writer.println(line);
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    System.out.println("Product codes list is null");
                                }
                            }
                        } else {
                            System.out.println("LicenseInfoList is null");
                        }
                    }
                }
            }
            System.out.println("Exported data to: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void analyzeContractsAndExport(List<Contract> contracts, String filePath) {
        List<Contract> contractsWithoutSwAGCloudAndView = new ArrayList<>();

        for (Contract contract : contracts) {
            if (contract.getEnvironments() == null || contract.getEnvironments().isEmpty()) {
                continue;
            }

            boolean hasViewProductCode = false;
            boolean hasSwAGCloud = false;

            for (Environment environment : contract.getEnvironments()) {
                List<LicenseInfo> licenseInfoList = environment.getLicenseInfo();
                if (hasViewProductCode(licenseInfoList)) {
                    hasViewProductCode = true;
                }
                if (environment.isSwAGCloud()) {
                    hasSwAGCloud = true;
                }
            }

            if (!hasViewProductCode && !hasSwAGCloud) {
                contractsWithoutSwAGCloudAndView.add(contract);
            }
        }
        exportOnPremWithoutView(contractsWithoutSwAGCloudAndView, filePath);
    }

    public static void exportOnPremWithoutView(List<Contract> contracts, String filePath) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Customer ID,Customer Name,Contract Number,Start Date,End Date");

            for (Contract contract : contracts) {
                String customerId = contract.getCustomerId();
                String customerName = contract.getCustomerName();
                String contractNumber = contract.getContractNumber();
                Date startDate = new Date(contract.getStartDate());
                Date endDate = new Date(contract.getEndDate());

                String line = String.join(",", customerId, "\"" + customerName + "\"", contractNumber,
                        dateFormat.format(startDate), dateFormat.format(endDate));
                writer.println(line);
            }

            System.out.println("Exported data to: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void exportMissingView(List<Contract> contracts, String filePath) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("customerId,CustomerName,contractNumber,startDate,endDate,environmentName");

            Map<String, List<Contract>> contractsByCustomer = contracts.stream()
                    .collect(Collectors.groupingBy(Contract::getCustomerId));

            for (String customerId : contractsByCustomer.keySet()) {
                List<Contract> customerContracts = contractsByCustomer.get(customerId);

                if (customerContracts.size() <= 1) {
                    continue;
                }

                boolean anyContractHasView = false;
                for (Contract contract : customerContracts) {
                    if (contractHasViewProductCode(contract)) {
                        anyContractHasView = true;
                        break;
                    }
                }

                if (!anyContractHasView) {
                    continue;
                }

                for (Contract contract : customerContracts) {
                    String customerName = contract.getCustomerName();
                    String contractNumber = contract.getContractNumber();
                    Date startDate = new Date(contract.getStartDate());
                    Date endDate = new Date(contract.getEndDate());

                    for (Environment environment : contract.getEnvironments()) {
                        List<LicenseInfo> licenseInfoList = environment.getLicenseInfo();
                        if (!hasViewProductCode(licenseInfoList)) {
                            String environmentName = environment.getName();
                            String line = String.join(",", customerId, "\"" + customerName + "\"",
                                    contractNumber, dateFormat.format(startDate), dateFormat.format(endDate), environmentName);
                            writer.println(line);
                        }
                    }
                }
            }

            System.out.println("Exported data to: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean contractHasViewProductCode(Contract contract) {
        for (Environment environment : contract.getEnvironments()) {
            List<LicenseInfo> licenseInfoList = environment.getLicenseInfo();
            if (hasViewProductCode(licenseInfoList)) {
                return true;
            }
        }
        return false;
    }

    public static void exportDuplicateEnvironments(List<Contract> contracts, String filePath) {
        Map<String, List<Contract>> environmentMap = new HashMap<>();

        for (Contract contract : contracts) {
            if (contract.getEnvironments() == null || contract.getEnvironments().isEmpty()) {
                continue;
            }

            for (Environment environment : contract.getEnvironments()) {
                String environmentName = environment.getName();
                List<String> productCodes = getProductCodes(environment);
                String environmentType = environment.getEnvironmentType();

                if (environmentName == null || hasViewProductCodeAndIsSaas(productCodes, environmentType)) {
                    continue;
                }

                String environmentKey = environmentName + "-" + productCodes;
                environmentMap.computeIfAbsent(environmentKey, k -> new ArrayList<>()).add(contract);
            }
        }

        exportDuplicateEnvironmentsToCSV(environmentMap, filePath);
    }

    public static void exportDuplicateEnvironmentsToCSV(Map<String, List<Contract>> environmentMap, String filePath) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Environment Name,Customer ID,Customer Name,Contract Number,Start Date,End Date,Product Codes");

            for (Map.Entry<String, List<Contract>> entry : environmentMap.entrySet()) {
                String environmentKey = entry.getKey();
                List<Contract> contracts = entry.getValue();

                if (contracts.size() > 1) {
                    String[] parts = environmentKey.split("-");
                    String environmentName = parts[0];
                    String productCodes = parts[1];

                    for (Contract contract : contracts) {
                        String customerId = contract.getCustomerId();
                        String customerName = contract.getCustomerName();
                        String contractNumber = contract.getContractNumber();
                        Date startDate = new Date(contract.getStartDate());
                        Date endDate = new Date(contract.getEndDate());

                        String line = String.join(",", environmentName, customerId, "\"" + customerName + "\"", contractNumber,
                                dateFormat.format(startDate), dateFormat.format(endDate), "\"" + productCodes + "\"");
                        writer.println(line);
                    }
                }
            }

            System.out.println("Exported data to: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isEnvironmentTypeSaas(String environmentType) {
        return environmentType != null && environmentType.equalsIgnoreCase("SAAS");
    }

    private static boolean hasViewProductCodeAndIsSaas(List<String> productCodes, String environmentType) {
        return !isEnvironmentTypeSaas(environmentType) || (productCodes != null && productCodes.contains("VIEW"));
    }

    private static List<String> getProductCodes(Environment environment) {
        List<String> productCodes = new ArrayList<>();
        List<LicenseInfo> licenseInfoList = environment.getLicenseInfo();
        if (licenseInfoList != null) {
            for (LicenseInfo licenseInfo : licenseInfoList) {
                List<Map<String, List<String>>> productCodesList = licenseInfo.getProductCodes();
                if (productCodesList != null) {
                    for (Map<String, List<String>> productCodeMap : productCodesList) {
                        for (List<String> codes : productCodeMap.values()) {
                            if (codes != null) {
                                productCodes.addAll(codes);
                            }
                        }
                    }
                }
            }
        }
        return productCodes;
    }
}
