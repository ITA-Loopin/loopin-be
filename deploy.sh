#!/usr/bin/env bash
set -euo pipefail

# -----------------------------
# Config
# -----------------------------
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"

cd "$APP_DIR"

# docker compose / docker-compose 자동 감지
if command -v docker-compose >/dev/null 2>&1; then
  DC="docker-compose"
else
  DC="docker compose"
fi

echo "[deploy] dir=$APP_DIR"
echo "[deploy] compose=$COMPOSE_FILE"
echo "[deploy] dc='$DC'"

# -----------------------------
# Pre-checks
# -----------------------------
if ! command -v docker >/dev/null 2>&1; then
  echo "[deploy] ERROR: docker not found" >&2
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "[deploy] ERROR: $COMPOSE_FILE not found in $APP_DIR" >&2
  exit 1
fi

# FCM 서비스 계정 파일 체크 (비어있으면 바로 실패)
if [[ ! -s "firebase-adminsdk.json" ]]; then
  echo "[deploy] ERROR: firebase-adminsdk.json is missing or empty" >&2
  echo "[deploy] Hint: FIREBASE_ADMIN_SDK_B64 주입/출력 부분 확인 필요" >&2
  exit 1
fi

# jq가 있으면 JSON 유효성 검사까지
if command -v jq >/dev/null 2>&1; then
  if ! jq -e . < firebase-adminsdk.json >/dev/null 2>&1; then
    echo "[deploy] ERROR: firebase-adminsdk.json is not valid JSON" >&2
    exit 1
  fi
fi

# -----------------------------
# Deploy
# -----------------------------
echo "[deploy] pulling images..."
$DC -f "$COMPOSE_FILE" pull

echo "[deploy] starting/updating containers..."
# --remove-orphans: compose 파일에서 사라진 컨테이너 정리
$DC -f "$COMPOSE_FILE" up -d --remove-orphans

echo "[deploy] current status:"
$DC -f "$COMPOSE_FILE" ps

# app 컨테이너 로그 약간 보여주기(디버깅 편의)
APP_CID="$($DC -f "$COMPOSE_FILE" ps -q app || true)"
if [[ -n "${APP_CID:-}" ]]; then
  echo "[deploy] tail app logs:"
  docker logs --tail 120 "$APP_CID" || true
fi

# 찌꺼기 이미지 정리(실패해도 배포 성공에는 영향 없게)
echo "[deploy] pruning dangling images..."
docker image prune -f >/dev/null 2>&1 || true

echo "[deploy] done"
