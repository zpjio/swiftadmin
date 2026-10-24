# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 先复制所有 pom.xml，便于复用依赖下载缓存
COPY pom.xml .
COPY swiftadmin-common/pom.xml swiftadmin-common/
COPY swiftadmin-common/swiftadmin-common-core/pom.xml swiftadmin-common/swiftadmin-common-core/
COPY swiftadmin-common/swiftadmin-common-mybatis/pom.xml swiftadmin-common/swiftadmin-common-mybatis/
COPY swiftadmin-common/swiftadmin-common-security/pom.xml swiftadmin-common/swiftadmin-common-security/
COPY swiftadmin-system/pom.xml swiftadmin-system/
COPY swiftadmin-system/swiftadmin-system-api/pom.xml swiftadmin-system/swiftadmin-system-api/
COPY swiftadmin-system/swiftadmin-system-biz/pom.xml swiftadmin-system/swiftadmin-system-biz/
COPY swiftadmin-auth/pom.xml swiftadmin-auth/
COPY swiftadmin-auth/swiftadmin-auth-api/pom.xml swiftadmin-auth/swiftadmin-auth-api/
COPY swiftadmin-auth/swiftadmin-auth-biz/pom.xml swiftadmin-auth/swiftadmin-auth-biz/
COPY swiftadmin-message/pom.xml swiftadmin-message/
COPY swiftadmin-message/swiftadmin-message-api/pom.xml swiftadmin-message/swiftadmin-message-api/
COPY swiftadmin-message/swiftadmin-message-biz/pom.xml swiftadmin-message/swiftadmin-message-biz/
COPY swiftadmin-schedule/pom.xml swiftadmin-schedule/
COPY swiftadmin-schedule/swiftadmin-schedule-api/pom.xml swiftadmin-schedule/swiftadmin-schedule-api/
COPY swiftadmin-schedule/swiftadmin-schedule-biz/pom.xml swiftadmin-schedule/swiftadmin-schedule-biz/
COPY swiftadmin-boot/pom.xml swiftadmin-boot/

RUN mvn -B -q dependency:go-offline || true

# 复制源代码并构建
COPY . .
RUN mvn -B clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建非 root 用户
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

# 复制构建产物
COPY --from=builder /app/swiftadmin-boot/target/swiftadmin-boot-*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
