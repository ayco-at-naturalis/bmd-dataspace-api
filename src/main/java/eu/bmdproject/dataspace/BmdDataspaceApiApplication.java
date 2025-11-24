package eu.bmdproject.dataspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BmdDataspaceApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(BmdDataspaceApiApplication.class, args);
  }
}
