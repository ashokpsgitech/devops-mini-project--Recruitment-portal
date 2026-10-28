# ==============================================================================
# STAGE 1: Build & Package
# ==============================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Cache maven wrapper and dependencies
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B

# Copy source code and package application (skipping tests since tests run in CI)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Extract Spring Boot layered jar for efficient Docker layer caching
WORKDIR /build/target/extracted
RUN java -Djarmode=layertools -jar ../recruitment-portal-1.0.0-SNAPSHOT.jar extract

# ==============================================================================
# STAGE 2: Minimal Distroless / Hardened Production Runtime
# ==============================================================================
FROM eclipse-temurin:21-jre-jammy AS runner

LABEL maintainer="TalentBridge Engineering <engineering@talentbridge.internal>"
LABEL description="Production container image for TalentBridge Enterprise ATS"

# Create a non-privileged application group and user (Principle of Least Privilege)
RUN groupadd -g 10001 appgroup && \
    useradd -u 10001 -g appgroup -s /bin/bash -m appuser

# Create directories for local resume uploads and logs with proper ownership
RUN mkdir -p /var/log/talentbridge /var/data/talentbridge/resumes && \
    chown -R appuser:appgroup /var/log/talentbridge /var/data/talentbridge

WORKDIR /app

# Copy layered Spring Boot components in order of change frequency (best caching)
COPY --from=builder --chown=appuser:appgroup /build/target/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/target/extracted/application/ ./

USER appuser:appgroup

ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom" \
    RESUME_STORAGE_PATH=/var/data/talentbridge/resumes

EXPOSE 8080

HEALTHCHECK --interval=20s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
