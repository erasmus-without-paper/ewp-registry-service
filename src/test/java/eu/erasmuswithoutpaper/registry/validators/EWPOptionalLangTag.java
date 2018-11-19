package eu.erasmuswithoutpaper.registry.validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EWPOptionalLangTag extends EWPAbstractTag {
  private String lang;

  public EWPOptionalLangTag(String _lang) {
    lang = _lang;
  }

  @Override
  public List<String> getTags() {
    if (lang != null) {
      return Arrays.asList(" xml:lang=\"" + lang + "\"");
    }
    return new ArrayList<>();
  }
}
