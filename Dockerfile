# 1단계: 빌드 환경 (Maven + Java 11)
FROM maven:3.8.6-eclipse-temurin-11 AS build
COPY . .
RUN mvn clean package -DskipTests

# 2단계: 실행 환경 (Java 11 런타임)
FROM eclipse-temurin:11-jre-jammy
# 빌드된 결과물인 .war 파일을 app.war로 복사
COPY --from=build /target/*.war app.war
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.war"]