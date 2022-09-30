FROM eclipse-temurin:11

# Continue with your application deployment
RUN groupadd coach -g 9999 && \
    useradd coach -d / -g 9999 -u 9999 -M -s /sbin/nologin
RUN mkdir /opt/app && chown coach:coach /opt/app
COPY target/coach.jar /opt/app/coach.jar
USER coach
WORKDIR /opt/app
EXPOSE 8082
CMD ["java", "-jar", "/opt/app/coach.jar"]