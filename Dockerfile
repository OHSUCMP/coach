FROM eclipse-temurin:11

# Continue with your application deployment
RUN groupadd coach -g 9999 && \
    useradd coach -d / -g 9999 -u 9999 -M -s /sbin/nologin

RUN mkdir /opt/app && \
    mkdir /opt/app/defaults && \
    mkdir /opt/app/config && \
    mkdir /opt/app/logs && \
    chown -R coach:coach /opt/app
COPY --chown=coach:coach docker-image-files/logback.xml /opt/app/defaults/logback.xml
COPY --chown=coach:coach docker-image-files/logback.xml /opt/app/config/logback.xml
COPY --chown=coach:coach target/coach.jar /opt/app/coach.jar

COPY docker-image-files/entrypoint.sh /entrypoint.sh
RUN chmod 755 /entrypoint.sh

USER coach
WORKDIR /opt/app
EXPOSE 8082
ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-Dlogging.config=/opt/app/config/logback.xml", "-jar", "/opt/app/coach.jar"]
