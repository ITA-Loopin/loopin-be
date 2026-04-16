#!/bin/sh
set -e

# openssl이 없으면 설치 (nginx:alpine 기본 이미지에 미포함)
if ! command -v openssl > /dev/null 2>&1; then
  apk add --no-cache openssl
fi

DOMAIN="api.loopin.co.kr"
CERT_DIR="/etc/letsencrypt/live/$DOMAIN"
DUMMY_MARKER="$CERT_DIR/.dummy"

# 인증서가 없으면 더미 자체 서명 인증서 생성 (nginx 시작용)
if [ ! -f "$CERT_DIR/fullchain.pem" ]; then
  echo "[init-ssl] No certificate found for $DOMAIN. Creating dummy self-signed cert..."
  mkdir -p "$CERT_DIR"
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout "$CERT_DIR/privkey.pem" \
    -out "$CERT_DIR/fullchain.pem" \
    -subj "/CN=$DOMAIN" 2>/dev/null
  touch "$DUMMY_MARKER"
  echo "[init-ssl] Dummy certificate created. Waiting for certbot to issue real cert..."
fi

# 더미 인증서인 경우: certbot이 실제 인증서를 발급할 때까지 대기 후 reload
if [ -f "$DUMMY_MARKER" ]; then
  (
    # certbot-init이 실제 인증서를 발급하면 .dummy 마커가 아닌 renewal 디렉토리가 생김
    TIMEOUT=300
    ELAPSED=0
    while [ ! -d /etc/letsencrypt/renewal ] && [ "$ELAPSED" -lt "$TIMEOUT" ]; do
      sleep 5
      ELAPSED=$((ELAPSED + 5))
    done

    if [ ! -d /etc/letsencrypt/renewal ]; then
      echo "[init-ssl] WARNING: Timed out waiting for real certificate. Still using dummy cert."
    else
      rm -f "$DUMMY_MARKER"
      sleep 2
      nginx -s reload
      echo "[init-ssl] Real certificate detected. Nginx reloaded."
    fi
  ) &
fi

# 백그라운드에서 12시간마다 nginx reload (certbot 갱신 반영)
(while :; do sleep 12h; nginx -s reload 2>/dev/null || true; echo "[nginx] Periodic config reload."; done) &

exec nginx -g "daemon off;"
