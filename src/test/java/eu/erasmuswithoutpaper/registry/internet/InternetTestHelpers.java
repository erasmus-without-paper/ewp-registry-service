package eu.erasmuswithoutpaper.registry.internet;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;

public class InternetTestHelpers {

  public static List<String> extractParams(Request request, String paramName) {
    try {
      String query;
      if (request.getMethod().equals("GET")) {
        URL url;
        url = new URL(request.getUrl());
        query = url.getQuery();
      } else if (request.getMethod().equals("POST")) {
        if (request.getBody().isPresent()) {
          query = new String(request.getBody().get(), StandardCharsets.UTF_8);
        } else {
          query = null;
        }
      } else {
        throw new RuntimeException("Unsupported method - cannot extract params");
      }
      return extractParamsFromQueryString(query, paramName);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<String> extractParamsFromQueryString(String query, String paramName) {
    List<String> result = new ArrayList<>();
    try {
      if (query != null) {
        for (String pair : query.split("&")) {
          int index = pair.indexOf("=");
          if (index > 0) {
            String key = URLDecoder.decode(pair.substring(0, index), "UTF-8");
            if (!key.equals(paramName)) {
              continue;
            }
            String value = URLDecoder.decode(pair.substring(index + 1), "UTF-8");
            result.add(value);
          }
        }
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return result;
  }
}
