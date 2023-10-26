package eu.erasmuswithoutpaper.registry.configuration;

import java.util.ResourceBundle;

public abstract class Constans {

  public static final String REGISTRY_REPO_URL;

  static {
    ResourceBundle rb = ResourceBundle.getBundle("addresses");
    REGISTRY_REPO_URL = rb.getString("url.registry.repo");
  }

}
