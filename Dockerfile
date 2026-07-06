# 1단계: 빌드 환경 (WAR 파일 생성)
FROM maven:3.8.5-openjdk-11 AS build
COPY . .
RUN mvn clean package -DskipTests

# 2단계: 실행 환경 (톰캣 환경에서 WAR 실행)
FROM openjdk:11-jdk-slim
# 빌드된 결과물인 .war 파일을 app.war로 복사
COPY --from=build /target/*.war app.war
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.war"]