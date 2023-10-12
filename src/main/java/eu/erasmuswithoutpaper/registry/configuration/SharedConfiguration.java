package eu.erasmuswithoutpaper.registry.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import jakarta.servlet.Filter;

/**
 * Spring beans to be used when running both the actual application server AND unit tests.
 */
@Configuration
public class SharedConfiguration {

  /**
   * This enables Spring's default conversion filters to be applied to Spring's {@link Value}
   * annotations, e.g. comma-separated list of property values will be properly mapped to
   * {@link List Lists}.
   *
   * @return {@link DefaultConversionService}
   */
  @Bean
  public ConversionService conversionService() {
    return new DefaultConversionService();
  }

  /**
   * Adds "lazy" HTTP ETag support to all our responses.
   *
   * @return {@link ShallowEtagHeaderFilter}
   */
  @Bean
  public Filter shallowETagHeaderFilter() {
    return new ShallowEtagHeaderFilter();
  }
}
