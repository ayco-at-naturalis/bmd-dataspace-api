package eu.bmdproject.dataspace.bootstrap;

import eu.bmdproject.dataspace.controller.HomeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.TreeSet;

/**
 * Logs a compact list of HTTP endpoints discovered by Spring MVC at application startup.
 */
@Component
public class EndpointSummaryLogger implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(EndpointSummaryLogger.class);

  private static final String CONTROLLER_PACKAGE = HomeController.class.getPackage().getName();

  private final RequestMappingHandlerMapping handlerMapping;

  public EndpointSummaryLogger(RequestMappingHandlerMapping handlerMapping) {
    this.handlerMapping = handlerMapping;
  }

  @Override
  public void run(ApplicationArguments args) {
    Set<String> lines = new TreeSet<>();
    handlerMapping.getHandlerMethods().forEach((mappingInfo, handlerMethod) -> {
      if (!isInControllerPackage(handlerMethod)) {
        return;
      }
      collect(mappingInfo, lines);
    });
    if (!lines.isEmpty()) {
      LOG.info(
          "Discovered these HTTP endpoints ({}):\n{}",
          CONTROLLER_PACKAGE,
          String.join("\n", lines)
      );
    }
  }

  private boolean isInControllerPackage(HandlerMethod handlerMethod) {
    return handlerMethod.getBeanType()
        .getName()
        .startsWith(CONTROLLER_PACKAGE);
  }

  private void collect(RequestMappingInfo info, Set<String> lines) {
    var pathsCondition = info.getPathPatternsCondition();
    if (pathsCondition == null) {
      return;
    }
    var methods = info.getMethodsCondition().getMethods();
    if (methods.isEmpty()) {
      return;
    }
    pathsCondition.getPatterns().forEach(pattern -> {
      String path = pattern.getPatternString();
      for (RequestMethod method : methods) {
        lines.add(method.name() + " " + path);
      }
    });
  }

}
