package eu.bmdproject.dataspace.config;

import org.apache.jena.query.Dataset;
import org.apache.jena.tdb2.TDB2Factory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JenaConfig {

  @Bean
  public Dataset dataset(GraphProperties graphProperties) {
    return TDB2Factory.connectDataset(graphProperties.jenaDataDir().toString());
  }

}
