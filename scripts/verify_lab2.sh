#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REQUIRED_NODE_VERSION="$(cat "${ROOT_DIR}/.nvmrc")"
CURRENT_NODE_VERSION="$(node -p "process.versions.node" 2>/dev/null || true)"

if [[ -z "${CURRENT_NODE_VERSION}" ]]; then
  echo "Node.js 未安装，无法执行前端评测链。"
  exit 1
fi

if [[ "${CURRENT_NODE_VERSION%%.*}" != "${REQUIRED_NODE_VERSION%%.*}" ]]; then
  echo "当前 Node 版本为 ${CURRENT_NODE_VERSION}，项目要求 Node ${REQUIRED_NODE_VERSION}。"
  echo "请先执行 nvm use 或切换到 Node ${REQUIRED_NODE_VERSION} 后重试。"
  exit 1
fi

echo "[1/3] 运行后端测试"
(
  cd "${ROOT_DIR}/backend"
  ./mvnw clean test
)

echo "[2/3] 安装前端依赖"
(
  cd "${ROOT_DIR}/frontend"
  npm ci
)

echo "[3/3] 运行前端静态检查与构建"
(
  cd "${ROOT_DIR}/frontend"
  npm run lint
  npm run build
)

echo "Lab2 本地评测链执行完成。"
