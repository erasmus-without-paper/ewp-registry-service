package eu.erasmuswithoutpaper.registry.validators;


import org.joox.Match;

public interface CatalogueMatcherProvider {
  Match getMatcher();
}
