FROM eclipse-temurin:24-jre

ENV LANG=en_US.UTF-8

RUN mkdir -p \
    /opt/classpath \
    /data/bmd/jena \
    /data/bmd/log

COPY dependencies /opt/classpath

ENTRYPOINT ["java"]
CMD [ "-Dfile.encoding=UTF-8", \
      "-Dlogging.config=/logback-spring.xml", \
      "-cp", "/opt/classpath/*", \
      "eu.bmdproject.dataspace.BmdDataspaceApiApplication" ]
