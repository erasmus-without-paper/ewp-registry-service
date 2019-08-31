package eu.erasmuswithoutpaper.registry.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final ObjectWriter writer = new ObjectMapper().writer();
    private static final Logger logger = LoggerFactory.getLogger(PebbleAsJsonFilter.class);

    @Override
    public List<String> getArgumentNames() {
      return Collections.emptyList();
    }

    @Override
    public Object apply(Object inputObject, Map<String, Object> args) {
      try {
        return writer.writeValueAsString(inputObject);
      } catch (JsonProcessingException e) {
        logger.error(
            String.format("Exception raised while serializing object to JSON: %s", inputObject),
            e);
        return null;
      }
    }
  }
}
