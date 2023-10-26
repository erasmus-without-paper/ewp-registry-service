package eu.erasmuswithoutpaper.registry.configuration;

import java.util.ResourceBundle;

public abstract class Constans {

  public static final String DOCUMENTATION_URL;
  public static final String REGISTRY_REPO_URL;
  public static final String SCHEMA_BASE_URL;

  static {
    ResourceBundle rb = ResourceBundle.getBundle("addresses");
    DOCUMENTATION_URL = rb.getString("url.ewp.documentation");
    REGISTRY_REPO_URL = rb.getString("url.registry.repo");
    SCHEMA_BASE_URL = rb.getString("url.ewp.schema");
  }

}
