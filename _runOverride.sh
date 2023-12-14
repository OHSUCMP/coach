#!/bin/bash

mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.config.location=classpath:/application.properties,file:./override.properties"