# 공용 멀티스테이지 Dockerfile — SERVICE build-arg 하나로 5개 서비스 이미지를 모두 굽는다.
# 사용: docker build --build-arg SERVICE=pointbank-auth-service -t auth .
# ===== 1) 빌드 스테이지: JDK + Gradle 로 해당 모듈만 bootJar =====
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
ARG SERVICE
COPY . .
# 해당 모듈의 실행가능 jar(bootJar)만 생성 → plain jar 안 생겨 혼동 없음
RUN chmod +x gradlew && ./gradlew :services:${SERVICE}:bootJar --no-daemon

# ===== 2) 런타임 스테이지: JRE + jar 하나 (작고 안전) =====
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
ARG SERVICE
# 비루트 유저로 실행 (보안)
RUN useradd -r -u 1001 appuser
# 빌드 스테이지 산출물만 가져온다 (ARG 는 스테이지 넘으면 사라지므로 위에서 재선언함)
COPY --from=build /app/services/${SERVICE}/build/libs/*.jar app.jar
ENV JAVA_OPTS=""
USER appuser
# JAVA_OPTS 환경변수를 풀어쓰기 위해 셸 형태 사용
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
