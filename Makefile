.PHONY: help dev-backend dev-frontend build-all clean docker-up docker-down logs

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
	@echo ""

# 后端开发
dev-backend:
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

# Docker 相关
docker-up:
	cd deploy/docker && docker-compose up -d

docker-down:
	cd deploy/docker && docker-compose down

docker-rebuild:
	cd deploy/docker && docker-compose up -d --build

logs:
	cd deploy/docker && docker-compose logs -f

logs-backend:
	cd deploy/docker && docker-compose logs -f backend

logs-frontend:
	cd deploy/docker && docker-compose logs -f frontend

# 数据库相关
db-reset:
	@echo "重置数据库（删除所有数据）..."
	cd deploy/docker && docker-compose down -v
	cd deploy/docker && docker-compose up -d mysql
	@echo "数据库已重置！"

# 安装依赖
install:
	cd frontend && pnpm install

# 运行测试
test:
	cd backend && mvn test
	cd frontend && pnpm --filter admin-web test
