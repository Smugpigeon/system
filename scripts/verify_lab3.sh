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
  echo "当前 Node 版本为 ${CURRENT_NODE_VERSION}，推荐 Node ${REQUIRED_NODE_VERSION}。"
  echo "继续执行评测链；如果前端依赖安装失败，请先执行 nvm use。"
fi

echo "[1/4] 运行后端全量测试"
(
  cd "${ROOT_DIR}/backend"
  ./mvnw clean test
)

echo "[2/4] 安装前端依赖"
(
  cd "${ROOT_DIR}/frontend"
  npm ci
)

echo "[3/4] 运行前端 lint"
(
  cd "${ROOT_DIR}/frontend"
  npm run lint
)

echo "[4/4] 运行前端生产构建"
(
  cd "${ROOT_DIR}/frontend"
  npm run build
)

echo "Lab3 本地评测链执行完成。"
