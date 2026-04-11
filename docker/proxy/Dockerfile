FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY build/libs/proxy.jar app.jar

# 8080 = TCP data plane (client requests)
# 9000/udp = UDP control plane (worker heartbeats)
EXPOSE 8080
EXPOSE 9000/udp

ENTRYPOINT ["java", "-jar", "app.jar"]
