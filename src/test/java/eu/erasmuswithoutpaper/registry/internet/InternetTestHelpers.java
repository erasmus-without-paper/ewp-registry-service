package eu.erasmuswithoutpaper.registry.internet;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

public class InternetTestHelpers {

  private static String getQuery(Request request) {
    try {
      String query;
      if (request.getMethod().equals("GET")) {
        URL url;
        url = new URL(request.getUrl());
        query = url.getQuery();
      } else {
        if (request.getBody().isPresent()) {
          query = new String(request.getBody().get(), StandardCharsets.UTF_8);
        } else {
          query = null;
        }
      }
      return query;
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<String, List<String>> extractAllParams(Request request) {
    List<NameValuePair> params = URLEncodedUtils.parse(getQuery(request), StandardCharsets.UTF_8);
    Map<String, List<String>> result = new HashMap<>();
    for (NameValuePair nvp : params) {
      if (!result.containsKey(nvp.getName())) {
        result.put(nvp.getName(), new ArrayList<>());
      }
      result.get(nvp.getName()).add(nvp.getValue());
    }
    return result;
  }

  public static List<String> extractParams(Request request, String paramName) {
    String query = getQuery(request);
    return extractParamsFromQueryString(query, paramName);
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
