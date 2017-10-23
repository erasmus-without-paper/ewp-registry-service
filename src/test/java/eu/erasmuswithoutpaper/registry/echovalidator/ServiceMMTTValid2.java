package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one includes "original-date" instead of "date". This is valid.
 */
public class ServiceMMTTValid2 extends ServiceMMTTValid {

  public ServiceMMTTValid2(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  protected void includeProperHeaders(Request request, Response response) {
    this.includeDateHeaders(response, false, true);
    this.includeDigestHeader(response);
    this.includeXRequestIdHeader(request, response);
    this.includeXRequestSignature(request, response);
    this.includeSignatureHeader(response);
  }
}
