package eu.erasmuswithoutpaper.registry.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

/**
 * Custom filter to Pebble templating engine that converts passed object or array to its JSON
 * representation.
 * Usage: {{ variable | asJson }}
 */
@Component
public class PebbleAsJsonFilter extends AbstractExtension {
  @Override
  public Map<String, Filter> getFilters() {
    HashMap<String, Filter> functions = new HashMap<>();
    functions.put("asJson", new JsonWriterPebbleFunction());
    return functions;
  }

  public static class JsonWriterPebbleFunction implements Filter {
    private static final ObjectWriter WRITER = new ObjectMapper().writer();
    private static final Logger LOGGER = LoggerFactory.getLogger(PebbleAsJsonFilter.class);

    @Override
    public List<String> getArgumentNames() {
      return Collections.emptyList();
    }

    @Override
    public Object apply(Object inputObject, Map<String, Object> args, PebbleTemplate self,
        EvaluationContext context, int lineNumber) throws PebbleException {
      try {
        return WRITER.writeValueAsString(inputObject);
      } catch (JacksonException e) {
        LOGGER.error("Exception raised while serializing object to JSON: " + inputObject, e);
        return null;
      }
    }
  }
}
