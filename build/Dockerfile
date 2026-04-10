FROM eclipse-temurin:25-jre-noble

ENV LANG=en_US.UTF-8

RUN apt-get update \
 && apt-get upgrade -y \
 && apt-get install -y --no-install-recommends ca-certificates \
 && rm -rf /var/lib/apt/lists/* \
 && mkdir -p \
    /opt/classpath \
    /opt/app \
    /data/bmd/jena \
    /data/bmd/log

# Third-party dependencies only (relatively stable layer)
COPY dependencies/ /opt/classpath/

# Application artifacts, including app.jar itself (small, changes often)
COPY app/ /opt/app/

ENTRYPOINT ["java"]

# NOTE: We must add both /opt/app/ and /opt/app/* to the classpath! /opt/app/
#       ensures that version-info.json will be picked using Class::getResource.
#       /opt/app/* tells the JVM all jar files in there count as class path items.
CMD [ "-Dfile.encoding=UTF-8", \
      "-Dlogging.config=/logback-spring.xml", \
      "-cp", "/opt/classpath/*:/opt/app/:/opt/app/*", \
      "eu.bmdproject.dataspace.BmdDataspaceApiApplication" ]
