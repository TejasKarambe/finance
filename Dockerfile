# Stage 1: Build the Spring Boot application
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy maven wrapper and pom.xml
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Fix line endings and permissions for wrapper
RUN chmod +x ./mvnw

# Copy source and package jar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Lightweight JRE 21 Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as non-root user for security
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copy built artifact
COPY --from=build /app/target/*.jar app.jar

# Dynamic Port Binding (Render injects $PORT)
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
