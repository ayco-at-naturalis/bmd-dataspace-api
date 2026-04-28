package eu.bmdproject.dataspace.component;

import eu.bmdproject.dataspace.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Component
public class ROCrateUriValidator {

  public ROCrateUriValidator() {
  }

  public void validate(URI uri) {
    if (!"https".equalsIgnoreCase(uri.getScheme())) {
      throw new BadRequestException("Only https URIs are allowed, got: " + uri.getScheme());
    }
    String host = uri.getHost();
    InetAddress address;
    try {
      address = InetAddress.getByName(uri.getHost());
    } catch (UnknownHostException e) {
      throw new BadRequestException("Cannot resolve host: " + host);
    }
    if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
      throw new BadRequestException("Requests to private or loopback address not allowed: " + host);
    }
  }
}