package com.softwareag.metering.contracts;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TMSAuth {
    private static final Logger LOG = LoggerFactory.getLogger(TMSAuth.class);
    public static String getTokenFromKeycloak() throws IOException {
        String authServerUrl = "https://idm-us-west-2.softwareag.cloud/auth/realms/scimaster/protocol/openid-connect/token";
        String client = "***";
        String secret = "***";

        String base64Credentials = Base64.getEncoder()
                .encodeToString((client + ":" + secret).getBytes(StandardCharsets.UTF_8));
        System.out.println(base64Credentials);

        HttpPost httpPost = new HttpPost(authServerUrl);
        httpPost.setHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, AuthSchemes.BASIC + " " + base64Credentials));
        httpPost.setHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType()));
        httpPost.setEntity(new StringEntity("grant_type=client_credentials", StandardCharsets.UTF_8));

        String responseBody;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpResponse response = httpClient.execute(httpPost);
            int responseStatus = response.getStatusLine().getStatusCode();

            if (responseStatus != HttpStatus.SC_OK) {
                LOG.error("The response code: '{}' is different than the expected one: {}", responseStatus , HttpStatus.SC_OK);
                return null;
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                responseBody = EntityUtils.toString(entity);
                LOG.info("Received body: \n{}", responseBody);
            } else {
                LOG.error("Unexpected null response from the server");
                return null;
            }
        }

        JSONObject body = new JSONObject(responseBody);

        String keycloakToken;
        try {
            keycloakToken = body.getString("access_token");
        } catch (JSONException e) {
            LOG.error("The response does not include an '{}' element", "access_token", e);
            return null;
        }
        LOG.info("Retrieved token: \n{}", keycloakToken);

        return keycloakToken;
    }
}
