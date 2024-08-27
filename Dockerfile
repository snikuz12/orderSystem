# 첫 번째 단계: 애플리케이션 빌드
FROM openjdk:11 as stage1

# 작업 디렉터리 설정
WORKDIR /app

# Gradle Wrapper 파일과 필요한 폴더 복사
COPY gradlew .
COPY gradle gradle

# 소스 파일과 Gradle 설정 파일 복사
COPY src src
COPY build.gradle .
COPY settings.gradle .

# Gradle Wrapper에 실행 권한 부여
RUN chmod +x gradlew

# 애플리케이션 빌드
RUN ./gradlew bootJar

# 두 번째 단계: 실행 환경 설정
FROM openjdk:11

# 작업 디렉터리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=stage1 /app/build/libs/*.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
