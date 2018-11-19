package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Invalid, because it fetches GET-like parameters (from URL), even when POST request is received.
 */
public class ServiceHTTTInvalid12 extends ServiceHTTTValid {

  private static List<String> extractAllParams(Request request, String paramName) {
    List<String> result = new ArrayList<>();
    try {
      URL url;
      url = new URL(request.getUrl());
      String getQuery = url.getQuery();
      result.addAll(InternetTestHelpers.extractParamsFromQueryString(getQuery, paramName));
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    if (request.getBody().isPresent()) {
      String postQuery = new String(request.getBody().get(), StandardCharsets.UTF_8);
      result.addAll(InternetTestHelpers.extractParamsFromQueryString(postQuery, paramName));
    }
    return result;
  }

  public ServiceHTTTInvalid12(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected List<String> retrieveEchoValues(Request request) {
    return extractAllParams(request, "echo");
  }
}
