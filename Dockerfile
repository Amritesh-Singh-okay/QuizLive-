# ==============================================================================
# QuizLive Production Dockerfile
# Multi-stage build for lightweight, high-performance container deployment
# Compatible with: Docker, Railway, Render, Fly.io, AWS ECS, Google Cloud Run
# ==============================================================================

# Stage 1: Build application WAR
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom and sources
COPY pom.xml .
COPY src ./src

# Compile and assemble production WAR
RUN mvn clean package -DskipTests

# Stage 2: Production Apache Tomcat 10.1 Runtime (Jakarta EE 10)
FROM tomcat:10.1-jdk21-temurin
WORKDIR /usr/local/tomcat

# Remove default Tomcat demo webapps
RUN rm -rf webapps/*

# Deploy WAR as both ROOT.war (serves at /) and quizlive.war (serves at /quizlive)
COPY --from=build /app/target/quizlive.war webapps/ROOT.war
COPY --from=build /app/target/quizlive.war webapps/quizlive.war

# Copy dynamic port entrypoint script (handles cloud platform $PORT injection)
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Default HTTP port
EXPOSE 8080

ENTRYPOINT ["/docker-entrypoint.sh"]
