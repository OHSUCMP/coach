# Developing Flyway Migrations

Flyway is used to automate database migrations when you need to make schema changes. This directory contains folders to support two vendors - MySQL and SQL Server. These folders are named to match the vendor property used by the application.properties file in the definition:

```spring.flyway.locations=classpath:db/migration/{vendor}```

The folder names should not be changed. The list of vendors supported by Flyway/Spring Boot can be found here: https://github.com/spring-projects/spring-boot/blob/v3.1.4/spring-boot-project/spring-boot/src/main/java/org/springframework/boot/jdbc/DatabaseDriver.java

The list of migrations that have been run and their status are tracked in the database table flyway_schema_history. For a migration to occur on startup, the file must live in the vendor folder, and follow the naming convention:

```VYYYYMMDDHHMMSS__<anything>.sql```

Flyway runs migrations in date order and will fail if it finds a new script that is earlier than one it has already run. This configuration can be changed, but is not recommended.

# Best Practices

When the application starts and new scripts are detected, Flyway will try to run them. If there are errors in the script or if the database is changed in a way that does not match the domain defined by JPA, the application may fail to start. Here are a few practices you can try to employ to minimize these occurrences:

1. When developing the script locally, you may need to make incremental changes as you decide what the schema will be and test out additions. I often start by naming my script 'tempV....sql'. This prevents Flyway from picking up the change and trying to run it. Instead, I run this script manually until I'm satisfied it is correct. Then I back out the changes manually, rename the file so Flyway will see it, and restart.
2. Whenever possible, try to make the script idempotent. For instance, use conventions like 'drop table if exists' before creating a new table, so that running the script over and over again doesn't do any harm. If your script is idempotent, you don't have to manually back out any changes. Instead, you can simply delete the row in flyway_schema_history pertaining to the script so it has no memory of running it previously. On restart, it will run it again.
3. Always test the script thoroughly locally before pushing to production. Always consider the impacts a different instance of the database with different data could have. If in doubt, take a backup of the production database and restore it locally to test that the script works as expected.