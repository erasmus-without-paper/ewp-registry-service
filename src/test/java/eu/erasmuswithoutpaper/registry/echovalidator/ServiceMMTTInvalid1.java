package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it doesn't include the Date nor Original-Date headers.
 */
public class ServiceMMTTInvalid1 extends ServiceMMTTValid {

  public ServiceMMTTInvalid1(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  protected void includeProperHeaders(Request request, Response response) {
    this.includeDigestHeader(response);
    this.includeXRequestIdHeader(request, response);
    this.includeXRequestSignature(request, response);
    this.includeSignatureHeader(response);
  }
}
