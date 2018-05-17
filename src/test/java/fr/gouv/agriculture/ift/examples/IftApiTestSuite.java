package fr.gouv.agriculture.ift.examples;

import com.auth0.jwk.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IftApiTestSuite {

    public static final String IFT_API_SERVER_URL = "https://alim-pprd.agriculture.gouv.fr/ift-api";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    static OkHttpClient client = new OkHttpClient.Builder().build();;

    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<Hello> helloJsonAdapter = moshi.adapter(Hello.class);
    private final JsonAdapter<Campagne[]> campagneJsonAdapter = moshi.adapter(Campagne[].class);
    private final JsonAdapter<ProduitDoseReference[]> produitDoseRefAdapter = moshi.adapter(ProduitDoseReference[].class);
    private final JsonAdapter<IftTraitement> iftTraitementJsonAdapter = moshi.adapter(IftTraitement.class);
    private final JsonAdapter<IftSigne> iftSigneJsonAdapter = moshi.adapter(IftSigne.class);


    @Test
    public void testSayHello() throws IOException {

        Response response = run(IFT_API_SERVER_URL + "/api/hello");
        String jsonData = response.body().string();
        System.out.println(jsonData);

        if (!response.isSuccessful()){
            fail(response.body().string());
        }

        Hello hello = helloJsonAdapter.fromJson(jsonData);

        assertThat(hello.message, notNullValue());
        assertThat(hello.message, is("Hello from IFT API"));

    }

    @Test
    public void testGetCampagnes() throws IOException {

        Response response = run(IFT_API_SERVER_URL + "/api/campagnes");
        String jsonData = response.body().string();
        System.out.println(jsonData);

        if (!response.isSuccessful()){
            fail(response.body().string());
        }

        Campagne[] campagnes = campagneJsonAdapter.fromJson(jsonData);

        assertThat(campagnes, notNullValue());
        assertThat(campagnes.length, greaterThan(0));

    }

    @Test
    public void testGetProduitDoseReference() throws IOException {

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(IFT_API_SERVER_URL ).append("/api/produits-doses-reference?")
                .append("campagneIdMetier=2018")
                .append("&cultureIdMetier=1161")
                .append("&produitLibelle=BOUILLIE%20CAZORLA%2020%20PM")
                .append(("&type=culture"));

        Response response = run(urlBuilder.toString());
        String jsonData = response.body().string();
        System.out.println(jsonData);

        if (!response.isSuccessful()){
            fail(response.body().string());
        }

        ProduitDoseReference[] produitDoseReferences = produitDoseRefAdapter.fromJson(jsonData);

        assertThat(produitDoseReferences, notNullValue());
        assertThat(produitDoseReferences.length, is(1));
        ProduitDoseReference res = produitDoseReferences[0];
        assertThat(res.campagne.idMetier, is("2018"));
        assertThat(res.culture.idMetier, is("1161"));
        assertThat(res.produit.libelle, is("BOUILLIE CAZORLA 20 PM"));
        assertThat(res.numeroAmm.idMetier, is("2100061"));
        assertThat(res.biocontrol, is(false));
        assertThat(res.dose, is(25.0));

    }

    @Test
    public void testComputeIft() throws IOException {

        Response response = getIftTraitement(false);

        String jsonData = response.body().string();
        System.out.println(jsonData);

        if (!response.isSuccessful()){
            fail(response.body().string());
        }

        IftTraitement iftTraitement = iftTraitementJsonAdapter.fromJson(jsonData);

        assertThat(iftTraitement.ift, is(0.3));
        assertThat(iftTraitement.doseReference, is(1.0));

    }

    @Test
    public void testSignatureIft() throws IOException {

        Response response = getIftTraitement(true);

        String jsonData = response.body().string();
        System.out.println(jsonData);

        if (!response.isSuccessful()){
            fail(response.body().string());
        }

        IftSigne iftSigne = iftSigneJsonAdapter.fromJson(jsonData);

        assertThat(iftSigne.iftTraitement.ift, is(0.3));
        assertThat(iftSigne.iftTraitement.doseReference, is(1.0));

        assertThat(iftSigne.signature, notNullValue());

        assertTrue("The signature is not valid", verifySignature(iftSigne.signature));
    }

    /**
     * DTO section
     *
     * The DTO definition is not exhaustive here
     * Only useful properties have been defined (for testing purpose)
     */
    static public class Hello {
        String message;
    }

    static public class Campagne {
        String idMetier;
        String libelle;
        boolean active;
    }

    static public class Culture {
        String idMetier;
        String libelle;
        //...
    }

    static public class Produit {
        String libelle;
    }

    static public class NumeroAmm {
        String idMetier;
    }

    static public class ProduitDoseReference {
        Campagne campagne;
        Culture culture;
        Produit produit;
        NumeroAmm numeroAmm;
        Double dose;
        boolean biocontrol;
        //...
    }

    static public class IftTraitement {
        Double ift;
        Double doseReference;
        //...
    }

    static public class IftSigne {
        IftTraitement iftTraitement;
        String signature;
    }

    /**
     * Allow to decode and verify signature with JWKS endpoint (/.well-known/jwks.json)
     * @param signature
     * @return
     */
    private boolean verifySignature(String signature){

        try {
            //Decode JWT header to retrieve the public key id property ("kid")
            DecodedJWT jwt = JWT.decode(signature);
            RSAKeyProvider keyProvider = getPublicKeyProvider();

            Algorithm algorithm = Algorithm.RSA256(keyProvider);
            algorithm.verify(jwt);
            return true;

        } catch (Exception exception){
            exception.printStackTrace();
        }

        return false;
    }

    /**
     * Public key provider
     * Use JWKS endpoint.
     * @see https://tools.ietf.org/html/rfc7517 for more details about jwks
     */
    private RSAKeyProvider getPublicKeyProvider() throws MalformedURLException {
        RSAKeyProvider keyProvider = new RSAKeyProvider() {
            JwkProvider provider = new UrlJwkProvider(new URL(IFT_API_SERVER_URL + "/.well-known/jwks.json"));

            public RSAPublicKey getPublicKeyById(String kid) {

                //Received 'kid' value might be null if it wasn't defined in the Token's header
                if (kid != null){

                    try {
                        Jwk jwk = provider.get(kid);

                        return (RSAPublicKey) jwk.getPublicKey();
                    } catch (InvalidPublicKeyException e) {
                        e.printStackTrace();
                    } catch (JwkException e) {
                        e.printStackTrace();
                    }
                }

                return null;
            }

            public RSAPrivateKey getPrivateKey() {
                throw new RuntimeException("Not implemented");
            }

            public String getPrivateKeyId() {
                throw new RuntimeException("Not implemented");
            }
        };

        return keyProvider;
    }

    /**
     * Helper to compute ift
     * @param signed
     * @return
     * @throws IOException
     */
    private Response getIftTraitement(Boolean signed) throws IOException {
        StringBuilder urlBuilder = new StringBuilder();

        urlBuilder.append(IFT_API_SERVER_URL);

        if (signed){
            urlBuilder.append("/api/ift/traitement/certifie?");
        } else {
            urlBuilder.append("/api/ift/traitement?");
        }

        urlBuilder.append("campagneIdMetier=2018")
                .append("&numeroAmmIdMetier=9900206")
                .append("&cultureIdMetier=1161")
                .append("&cibleIdMetier=82")
                .append("&typeTraitementIdMetier=T22")
                .append("&uniteIdMetier=U3")
                .append("&dose=0.5")
                .append("&facteurDeCorrection=60");

        return run(urlBuilder.toString());
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

}
