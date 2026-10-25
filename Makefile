.PHONY: help dev-backend dev-frontend build-all clean docker-up docker-down docker-rebuild logs logs-backend logs-frontend db-reset install backend-install test

# 默认目标
help:
	@echo "SwiftAdmin Monorepo - 可用命令："
	@echo ""
	@echo "  dev-backend     启动后端开发服务器"
	@echo "  dev-frontend    启动前端开发服务器"
	@echo "  build-all       构建前后端"
	@echo "  clean           清理构建产物"
	@echo "  docker-up       启动 Docker 服务"
	@echo "  docker-down     停止 Docker 服务"
	@echo "  logs            查看 Docker 日志"
	@echo "  test            运行后端测试与前端类型检查（需先 docker up mysql/redis）"
	@echo ""

# 后端依赖安装（兄弟模块 SNAPSHOT 需装入本地仓库，spring-boot:run -pl 才能解析）
backend-install:
	cd backend && mvn -q install -DskipTests

# 后端开发
dev-backend: backend-install
	cd backend && mvn spring-boot:run -pl swiftadmin-boot

# 前端开发
dev-frontend:
	cd frontend && pnpm dev

# 构建全部
build-all:
	@echo "构建后端..."
	cd backend && mvn clean package -DskipTests
	@echo "构建前端..."
	cd frontend && pnpm build
	@echo "构建完成！"

# 清理
clean:
	@echo "清理后端..."
	cd backend && mvn clean
	@echo "清理前端..."
	cd frontend && pnpm clean
	@echo "清理完成！"

# Docker 相关（Docker Compose V2 子命令语法）
docker-up:
	cd deploy/docker && docker compose up -d mysql redis

docker-down:
	cd deploy/docker && docker compose down

docker-rebuild:
	cd deploy/docker && docker compose up -d --build

logs:
	cd deploy/docker && docker compose logs -f

logs-backend:
	cd deploy/docker && docker compose logs -f backend

logs-frontend:
	cd deploy/docker && docker compose logs -f frontend

# 数据库相关
db-reset:
	@echo "重置数据库（删除所有数据）..."
	cd deploy/docker && docker compose down -v
	@echo "数据库已重置！重新构建后请再次 docker compose up -d mysql"

# 安装依赖
install:
	cd frontend && pnpm install

# 运行测试
# 前置条件：docker compose -d mysql redis 已启动
# （compose 默认 root/root，与应用 dev 默认账号及 CI 一致，无需额外环境变量）
test-backend:
	cd backend && mvn -B -ntp test

test: test-backend
	@echo "前端类型检查..."
	cd frontend && pnpm type-check
