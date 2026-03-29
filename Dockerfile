# For building, we need a JDK as well as Maven.
FROM maven:3-eclipse-temurin-17 AS build
RUN mkdir /opt/app
WORKDIR /opt/app

# Install dependencies first to cache the build layer until they change.
COPY pom.xml .
RUN mvn verify -DskipTests --fail-never

# Now compile the application.
COPY . .
RUN mvn package -DskipTests

# For running, we need a JRE and various project files.
FROM eclipse-temurin:17 AS run

RUN apt update && apt install -y wait-for-it
 # netcat-traditional

# Continue with your application deployment
RUN groupadd coach -g 9999 && \
    useradd coach -d / -g 9999 -u 9999 -M -s /sbin/nologin

RUN mkdir -p /opt/app/config
RUN mkdir -p /opt/app/logs
WORKDIR /opt/app/
# COPY --chown=coach:coach docker-image-files/logback.xml defaults/logback.xml
COPY --chown=coach:coach docker-image-files/logback-console-only.xml config/logback.xml

# Copy in the .jar file from the build stage.
COPY --from=build /opt/app/target/coach.jar /opt/app/coach.jar

RUN chown -R coach:coach /opt/app

# Run it!
USER coach
EXPOSE 8082
CMD ["java", "-Dlogging.config=/opt/app/config/logback.xml", "-jar", "/opt/app/coach.jar"]
