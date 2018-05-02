package fr.gouv.agriculture.ift.examples;

import fr.gouv.agriculture.ift.examples.dto.HelloDTO;
import okhttp3.*;
import org.junit.BeforeClass;
import org.junit.Test;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.cert.CertificateException;

public class IftApiTestSuite {

    public static final String IFT_API_SERVER_URL = "https://alim-pprd.agriculture.gouv.fr/ift-api";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    static OkHttpClient client;

    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<Hello> helloDTOJsonAdapter = moshi.adapter(Hello.class);


    @BeforeClass
    public static void setup() throws IOException, KeyManagementException {
        client = getUnsafeOkHttpClient();
    }


    @Test
    public void testSayHello() throws IOException {

        Response response = run(IFT_API_SERVER_URL + "/api/hello");
        String jsonData = response.body().string();
        System.out.println(jsonData);

        if (!response.isSuccessful()){
            fail(response.body().string());
        }

        Hello hello = helloDTOJsonAdapter.fromJson(jsonData);

        assertThat(hello.message, notNullValue());
        assertThat(hello.message, is("Hello from IFT API"));

    }

    /**
     * DTO section
     */
    public class Hello {
        String message;
    }

    /**
     * Internal helper for running http request
     * @param url
     * @return
     * @throws IOException
     */
    private Response run(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        return client.newCall(request).execute();
    }

    /**
     * For testing purpose only : ignore SSL verification
     * In production mode, CA certificate should be added to java keystore
     * @return
     */
    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            OkHttpClient okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
