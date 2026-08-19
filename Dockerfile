FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app
RUN apk add --no-cache maven

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
ENV TZ=Asia/Tashkent
COPY --from=build /app/target/*.jar app.jar

EXPOSE 5050
EXPOSE 8888
EXPOSE 8889

ENTRYPOINT ["java", "-jar", "app.jar"]