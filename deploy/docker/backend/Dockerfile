# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 pom.xml 文件
COPY pom.xml .
COPY swiftadmin-common/pom.xml swiftadmin-common/
COPY swiftadmin-auth/pom.xml swiftadmin-auth/
COPY swiftadmin-system/pom.xml swiftadmin-system/
COPY swiftadmin-boot/pom.xml swiftadmin-boot/

# 下载依赖（利用 Docker 缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY swiftadmin-common swiftadmin-common
COPY swiftadmin-auth swiftadmin-auth
COPY swiftadmin-system swiftadmin-system
COPY swiftadmin-boot swiftadmin-boot

# 构建项目
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建非 root 用户
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

# 复制构建产物
COPY --from=builder /app/swiftadmin-boot/target/*.jar app.jar

# 设置权限
RUN chown -R appuser:appgroup /app

USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
