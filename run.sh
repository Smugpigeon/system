#!/bin/bash

# 任务管理系统启动脚本
# 用法: ./run.sh [backend|frontend|all|stop]

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
FRONTEND_DIR="$PROJECT_DIR/frontend"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

start_backend() {
    echo -e "${YELLOW}启动后端服务...${NC}"
    cd "$BACKEND_DIR" || exit
    if [ -f "./mvnw" ]; then
        nohup ./mvnw spring-boot:run > "$PROJECT_DIR/backend.log" 2>&1 &
    else
        nohup mvn spring-boot:run > "$PROJECT_DIR/backend.log" 2>&1 &
    fi
    echo $! > "$PROJECT_DIR/backend.pid"
    echo -e "${GREEN}后端已启动 (PID: $(cat "$PROJECT_DIR/backend.pid"))${NC}"
    echo "日志: $PROJECT_DIR/backend.log"
    sleep 3
}

start_frontend() {
    echo -e "${YELLOW}启动前端服务...${NC}"
    cd "$FRONTEND_DIR" || exit
    
    if [ ! -d "node_modules" ]; then
        echo "安装前端依赖..."
        npm install
    fi
    
    nohup npm run dev > "$PROJECT_DIR/frontend.log" 2>&1 &
    echo $! > "$PROJECT_DIR/frontend.pid"
    echo -e "${GREEN}前端已启动 (PID: $(cat "$PROJECT_DIR/frontend.pid"))${NC}"
    echo "日志: $PROJECT_DIR/frontend.log"
}

stop_all() {
    echo -e "${YELLOW}停止所有服务...${NC}"
    if [ -f "$PROJECT_DIR/backend.pid" ]; then
        kill "$(cat "$PROJECT_DIR/backend.pid")" 2>/dev/null && echo "后端已停止"
        rm -f "$PROJECT_DIR/backend.pid"
    fi
    if [ -f "$PROJECT_DIR/frontend.pid" ]; then
        kill "$(cat "$PROJECT_DIR/frontend.pid")" 2>/dev/null && echo "前端已停止"
        rm -f "$PROJECT_DIR/frontend.pid"
    fi
    echo -e "${GREEN}所有服务已停止${NC}"
}

show_status() {
    echo "服务状态:"
    if [ -f "$PROJECT_DIR/backend.pid" ] && kill -0 "$(cat "$PROJECT_DIR/backend.pid")" 2>/dev/null; then
        echo -e "  后端: ${GREEN}运行中${NC} (PID: $(cat "$PROJECT_DIR/backend.pid"))"
    else
        echo -e "  后端: ${RED}未运行${NC}"
    fi
    
    if [ -f "$PROJECT_DIR/frontend.pid" ] && kill -0 "$(cat "$PROJECT_DIR/frontend.pid")" 2>/dev/null; then
        echo -e "  前端: ${GREEN}运行中${NC} (PID: $(cat "$PROJECT_DIR/frontend.pid"))"
    else
        echo -e "  前端: ${RED}未运行${NC}"
    fi
}

case "$1" in
    backend)
        start_backend
        ;;
    frontend)
        start_frontend
        ;;
    stop)
        stop_all
        ;;
    status)
        show_status
        ;;
    all|"")
        stop_all 2>/dev/null
        start_backend
        start_frontend
        echo ""
        echo -e "${GREEN}=================================${NC}"
        echo -e "${GREEN}  任务管理系统已启动!${NC}"
        echo -e "${GREEN}=================================${NC}"
        echo "访问地址:"
        echo "  前端页面: http://localhost:5173"
        echo "  后端API:  http://localhost:8080"
        echo "  H2控制台: http://localhost:8080/h2-console"
        echo ""
        echo "停止服务: ./run.sh stop"
        echo "查看状态: ./run.sh status"
        ;;
    *)
        echo "用法: ./run.sh [backend|frontend|all|stop|status]"
        exit 1
        ;;
esac