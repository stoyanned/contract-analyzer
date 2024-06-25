# Contract Analyzer

Contract Analyzer is a Java application designed to analyze contract data from TMS and generate various reports. It also sends these reports via email.

## Prerequisites

1. **Java Development Kit (JDK) 8 or later** installed on your machine.
2. **Apache HttpComponents** for HTTP requests.
3. **Google Gson** for JSON parsing.

## Setup and Installation

1. **Clone the Repository**:
   - ```sh
     git clone https://github.webmethods.io/stne/metering-contracts.git
     cd metering-contracts
     ```

2. **Download Dependencies**:
   - Download and add the following JAR files to your project:
     - Google Gson: [Download Gson](https://github.com/google/gson)
     - Apache HttpComponents: [Download HttpComponents](https://hc.apache.org/downloads.cgi)
   - Place these JAR files in a `libs` directory within your project.

3. **Create Configuration Files**:
   - Create an `email.properties` file in the `resources` directory of your project with the following content:
     ```properties
     email.host=daesmtp.eur.ad.sag
     email.port=587
     email.username=tech-plt@softwareag.com
     email.password=***
     ```

## Compilation and Execution

1. **Compilation**:
   - Compile the project using Gradle:
     ```sh
     gradle build
     ```

2. **Execution**:
   - Run the application using Gradle:
     ```sh
     gradle run
     ```

## Application Workflow

1. **Authentication**:
   - The application first authenticates with Keycloak to obtain an authentication token. Ensure your Keycloak server is running and configured correctly.

2. **Fetch Contract Data**:
   - Using the authentication token, the application retrieves contract IDs and detailed contract data from the TMS.

3. **Analyze Contracts**:
   - The application analyzes the contracts to check for specific product codes (`VIEW`) and environment configurations.

4. **Generate Reports**:
   - Several CSV reports are generated based on the analysis:
     - General Report (`generalReport.csv`)
     - On-Premises Contracts without VIEW Product Code (`onPremWithoutView.csv`)
     - Contracts Missing VIEW Product Code (`missingView.csv`)
     - Duplicate Environments (`duplicates.csv`)

5. **Send Reports via Email**:
   - The generated reports are sent as email attachments to a specified email address using the SMTP settings provided in the `email.properties` file.

## Reports

1. **General Report**:
   - This report includes all contracts and their environments, providing a comprehensive overview of the contract details.

2. **On-Premises Contracts without VIEW Product Code**:
   - This report lists contracts that are on-premises and do not have the `VIEW` product code in any environment.

3. **Contracts Missing VIEW Product Code**:
   - This report highlights contracts that have some environments with the `VIEW` product code and others without it.

4. **Duplicate Environments**:
   - This report identifies environments with duplicate names and product codes, indicating potential redundancies.

## Troubleshooting

1. **Common Issues**:
   - **Authentication Failure**: Ensure your Keycloak server is running and accessible. Verify the credentials and URLs used for authentication.
   - **Missing Dependencies**: Ensure all required JAR files are included in the `libs` directory and properly referenced in the classpath during compilation and execution.
   - **Email Sending Issues**: Verify the SMTP settings in the `email.properties` file. Check if your email provider allows sending emails via SMTP and that the credentials are correct.

2. **Logs**:
   - The application prints logs to the console to indicate the progress and any issues encountered during execution.

## License

- This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [Google Gson](https://github.com/google/gson)
- [Apache HttpComponents](https://hc.apache.org/)

## Contributing

- If you would like to contribute to this project, please fork the repository and create a pull request with your changes.

## Contact

- For any questions or issues, please contact the project maintainer at stoyan.nedelchev@softwareag.com.

