FROM eclipse-temurin:24-jre

ENV LANG=en_US.UTF-8

RUN mkdir -p /opt/classpath /data/bmd/log/

COPY dependencies /opt/classpath
COPY logback-spring.xml /opt/classpath

ENTRYPOINT ["java"]
CMD [ "-Xms2048m" \
    , "-Xmx2048m" \
    , "-Dfile.encoding=UTF-8" \
    , "-Dlogging.config=/opt/classpath/logback-spring.xml" \
    , "-cp" \
    , "/opt/classpath/*" \
    , "eu.bmdproject.dataspace.BmdDataspaceApiApplication" ]
