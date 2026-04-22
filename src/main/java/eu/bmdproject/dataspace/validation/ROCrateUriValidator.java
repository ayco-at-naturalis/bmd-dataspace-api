package eu.bmdproject.dataspace.validation;

import eu.bmdproject.dataspace.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class ROCrateUriValidator {

  private final IPValidator ipValidator;

  public ROCrateUriValidator(IPValidator ipValidator) {
    this.ipValidator = ipValidator;
  }

  public void validate(URI uri) {
    if (!"https".equalsIgnoreCase(uri.getScheme())) {
      throw new BadRequestException("Only https URIs are allowed, got: " + uri.getScheme());
    }
    // More validations
    ipValidator.validate(uri);
  }
}