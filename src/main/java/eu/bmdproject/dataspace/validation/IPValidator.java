package eu.bmdproject.dataspace.validation;

import eu.bmdproject.dataspace.exception.BadRequestException;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Component
public class IPValidator {

  public void validate(@NotBlank URI uri) {
    String host = uri.getHost();
    InetAddress address;
    try {
      address = InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      throw new BadRequestException("Cannot resolve host: " + host);
    }
    if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
      throw new BadRequestException("Requests to private or loopback address not allowed: " + host);
    }
  }
}