FROM eclipse-temurin:24-jre

ENV LANG=en_US.UTF-8

RUN mkdir -p \
    /opt/classpath \
    /opt/app \
    /data/bmd/jena \
    /data/bmd/log

# Third-party dependencies only (relatively stable layer)
COPY dependencies/ /opt/classpath/

# App artifacts (small, changes often)
COPY app/ /opt/app/

ENTRYPOINT ["java"]
CMD [ "-Dfile.encoding=UTF-8", \
      "-Dlogging.config=/logback-spring.xml", \
      "-cp", "/opt/classpath/*:/opt/app/*", \
      "eu.bmdproject.dataspace.BmdDataspaceApiApplication" ]
