#!/bin/bash

# Konfiguration
EC2_USER="ubuntu"
EC2_HOST="16.170.220.163"
EC2_KEY_PATH="/Users/raphael.ruoff/Desktop/ec2 instanz Dodge Drive/ec2-key-raphi.pem"

echo " Erstelle sauberes deploy-Verzeichnis lokal..."
rm -rf deploy
mkdir -p deploy

# docker-compose.yml und Init-Skripte kopieren
cp docker-compose.yml deploy/
cp -r postgres-init deploy/

# Services iterieren und build + Dockerfile kopieren
SERVICES=(
  "session-service"
  "game-service"
  "leaderboard-service"
  "profile-service"
  "auth-service"
  "friends-service"
  "log-service"
  "websocket-service"
)

for SERVICE in "${SERVICES[@]}"; do
  echo " Kopiere Build & Dockerfile von $SERVICE..."
  mkdir -p deploy/services/$SERVICE/build/libs
  cp services/$SERVICE/build/libs/*.jar deploy/services/$SERVICE/build/libs/
  cp services/$SERVICE/Dockerfile deploy/services/$SERVICE/
done

# Archiv erzeugen
echo " Erstelle deploy.tar.gz..."
tar -czf deploy.tar.gz deploy

# Hochladen per SCP
echo " Lade deploy.tar.gz auf EC2 hoch..."
scp -i "$EC2_KEY_PATH" deploy.tar.gz $EC2_USER@$EC2_HOST:/home/ubuntu/

# Cleanup und Vorbereitungen remote auf EC2
echo " Bereinige alte Container und Verzeichnisse auf EC2..."

ssh -i "$EC2_KEY_PATH" $EC2_USER@$EC2_HOST << EOF
  echo " Stoppe und lösche alte Container..."
  docker ps -aq | xargs -r docker rm -f

  echo " Entferne altes deploy-Verzeichnis..."
  rm -rf ~/deploy

  echo " Entpacke neues deploy.tar.gz..."
  tar -xzf deploy.tar.gz

  echo " Bereit! Starte nun manuell mit:"
  echo "    cd ~/deploy && docker-compose up -d --build"
EOF

echo " Upload & Cleanup abgeschlossen."
