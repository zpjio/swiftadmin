#!/bin/bash

# =============================================================================
# SwiftAdmin 开发环境启动脚本
# 功能：加载环境变量、编译、启动后端服务
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
FRONTEND_DIR="$SCRIPT_DIR/frontend/apps/admin-web"
ENV_FILE="$BACKEND_DIR/.env.local"

# 端口配置 (从环境变量读取，或使用默认值)
BACKEND_PORT=${BACKEND_PORT:-8080}
FRONTEND_PORT=${FRONTEND_PORT:-5173}

# 解析命令行参数
KILL_PROCESS=true
START_FRONTEND=false
SKIP_COMPILE=false

for arg in "$@"; do
    case $arg in
        -k|--kill)
            KILL_PROCESS=true
            shift
            ;;
        --no-kill)
            KILL_PROCESS=false
            shift
            ;;
        -f|--frontend)
            START_FRONTEND=true
            shift
            ;;
        -s|--skip-compile)
            SKIP_COMPILE=true
            shift
            ;;
        -h|--help)
            echo "用法: ./run-dev.sh [选项]"
            echo ""
            echo "选项:"
            echo "  -k, --kill          启动前终止已运行的进程 (默认开启)"
            echo "  --no-kill           不终止已运行的进程"
            echo "  -f, --frontend      同时启动前端开发服务"
            echo "  -s, --skip-compile  跳过编译步骤"
            echo "  -h, --help          显示帮助信息"
            echo ""
            echo "示例:"
            echo "  ./run-dev.sh              # 终止后端进程后启动"
            echo "  ./run-dev.sh --no-kill    # 不终止进程直接启动"
            echo "  ./run-dev.sh -f           # 同时启动前端和后端"
            exit 0
            ;;
        *)
            ;;
    esac
done

# 终止占用端口的进程
kill_port_process() {
    local port=$1
    local name=$2

    local pid=$(lsof -ti:$port 2>/dev/null || true)

    if [ -n "$pid" ]; then
        echo -e "${YELLOW}⚠️  发现 $name 占用端口 $port (PID: $pid)${NC}"
        if [ "$KILL_PROCESS" = true ]; then
            echo -e "${BLUE}   正在终止进程...${NC}"
            kill -9 $pid 2>/dev/null || true
            sleep 1
            echo -e "${GREEN}✓ 已终止 $name 进程${NC}"
        else
            echo -e "${YELLOW}   跳过终止 (--no-kill)${NC}"
        fi
    fi
}

# 检查并终止已运行的进程
if [ "$KILL_PROCESS" = true ]; then
    echo -e "${BLUE}🔍 检查已运行的进程...${NC}"
    kill_port_process $BACKEND_PORT "后端服务"
    if [ "$START_FRONTEND" = true ]; then
        kill_port_process $FRONTEND_PORT "前端服务"
    fi
fi

# 加载环境变量
if [ -f "$ENV_FILE" ]; then
    echo -e "${GREEN}📋 加载环境变量: .env.local${NC}"
    set -a
    source "$ENV_FILE"
    set +a
else
    echo -e "${YELLOW}⚠️  未找到 .env.local，使用默认配置${NC}"
    echo -e "${YELLOW}   可复制 .env.local.example 创建自己的配置${NC}"
fi

# 编译后端
if [ "$SKIP_COMPILE" = false ]; then
    echo -e "${BLUE}🔧 编译后端...${NC}"
    cd "$BACKEND_DIR"
    mvn compile -q -DskipTests
    echo -e "${GREEN}✓ 后端编译完成${NC}"
fi

# 启动前端 (后台运行)
if [ "$START_FRONTEND" = true ]; then
    echo -e "${BLUE}🚀 启动前端开发服务...${NC}"
    cd "$FRONTEND_DIR"

    # 检查 node_modules
    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}   安装前端依赖...${NC}"
        pnpm install
    fi

    # 后台启动前端
    pnpm dev > /tmp/swiftadmin-frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo -e "${GREEN}✓ 前端已启动 (PID: $FRONTEND_PID, 端口: $FRONTEND_PORT)${NC}"
    echo -e "${GREEN}   访问地址: http://localhost:$FRONTEND_PORT${NC}"
fi

# 启动后端
echo -e "${BLUE}🚀 启动后端服务...${NC}"
cd "$BACKEND_DIR"
echo -e "${GREEN}   端口: $BACKEND_PORT${NC}"
echo -e "${GREEN}   API 文档: http://localhost:$BACKEND_PORT/swagger-ui.html${NC}"
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  按 Ctrl+C 停止服务${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

mvn spring-boot:run -pl swiftadmin-boot