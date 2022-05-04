package nessusTools.client;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

import org.apache.logging.log4j.*;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.util.*;


public abstract class GenericClient {
    private static final Logger logger = LogManager.getLogger(GenericClient.class);

    private final Client client;

    private String response;

    /**
     * Loads a properties file into a Properties instance and returns it.
     *
     * @param propertiesFilePath a path to a file on the java classpath list
     * @return a populated Properties instance or an empty Properties instance if
     * the file path was not found.
     */
    public static Properties loadProperties(String propertiesFilePath) {
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(propertiesFilePath));

        } catch (Exception e) {
            logger.error("Cannot load the properties file " + propertiesFilePath, e);
        }

        return properties;
    }

    public GenericClient(boolean acceptAnySSL) {
        if (acceptAnySSL) {
            this.client = AcceptAnySSL.makeClient();

        } else {
            this.client = ClientBuilder.newClient();
        }
    }

    public String getResponse() {
        return this.response;
    }

    public String fetch(String URL) {
        return fetch(URL, null, null, String.class);
    }

    public String fetch(String URL,
                         Map<String, String> headers,
                         MediaType mediaType) {
        return fetch(URL, headers, mediaType, String.class);
    }

    public <R> R fetch(String URL, Class<R> responseClass) {
        return fetch(URL, null, null, responseClass);
    }

    public <R> R fetch(String URL,
                        Map<String, String> headers,
                        MediaType mediaType,
                        Class<R> responseClass) {

        WebTarget target = client.target(URL);

        Invocation.Builder builder;
        if (mediaType != null) {
            builder = target.request(mediaType);

        } else {
            builder = target.request();
        }

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }
        }

        return builder.get(responseClass);
    }

    public JsonNode fetchJson(String URL)
            throws JsonProcessingException {

        return fetchJson(URL,  null, JsonNode.class, null);
    }

    public JsonNode fetchJson(String URL,
                              Map<String, String> headers)
            throws JsonProcessingException {

        return fetchJson(URL, headers, JsonNode.class,null);
    }

    public <T> T fetchJson(String URL, Class<T> mapToType)
            throws JsonProcessingException {

        return fetchJson(URL, null, mapToType, null);
    }

    public <T> T fetchJson(String URL,
                              Map<String, String> headers,
                              Class<T> mapToType)
            throws JsonProcessingException {

        return fetchJson(URL, headers, mapToType, null);
    }

    public <R> R fetchJson(String URL,
                           Map<String, String> headers,
                           Class<R> mapToType,
                           ObjectMapper mapper)
            throws JsonProcessingException {

        String response = fetch(URL, headers, MediaType.APPLICATION_JSON_TYPE);
        this.response = response;

        if (response == null || response.length() < 2) {
            //JSON needs at least two characters to be valid
            // e.g. {} or [] or ""
            return null;
        }

        if (mapper == null) {
            mapper = new ObjectMapper();
        }

        return mapper.readValue(response, mapToType);
    }
}
