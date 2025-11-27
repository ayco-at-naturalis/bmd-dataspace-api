# BMD Dataspace API

The BMD Dataspace API allows VREs to interact with the Cubing Engine and with the RO-Crate
storage and query service.

(More info coming soon)

## API Documentation (OpenAPI / Swagger UI)

The project integrates **Springdoc OpenAPI 3**, which automatically generates 
interactive API documentation for all REST controllers.

Once the application is running, you can access:

- **Swagger UI (interactive docs)**  
  👉 [http://localhost:8080/scalar](http://localhost:8080/scalar)

- **OpenAPI JSON specification**  
  👉 [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

These endpoints are available by default in all environments.

### 🔧 Configuration

The OpenAPI metadata (title, version, description) is defined in  
[`eu.bmdproject.dataspace.config.OpenApiConfig`](src/main/java/eu/bmdproject/dataspace/OpenApiConfig.java).

If you need to customize groupings, include/exclude endpoints, or add authentication 
details to the docs, refer to the [Springdoc OpenAPI documentation](https://springdoc.org/).

## Javadocs

Javadocs are generated as part of the GitLab pipeline. They can be viewed at
[https://naturalis.gitlab.io/bii/bmd/bmd-dataspace-api](https://naturalis.gitlab.io/bii/bmd/bmd-dataspace-api)