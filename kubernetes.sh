#!/bin/bash

SERVICES=(
  "auth-service"
  "friends-service"
  "game-service"
  "leaderboard-service"
  "log-service"
  "profile-service"
  "session-service"
  "websocket-service"
)

for SERVICE in "${SERVICES[@]}"; do
  echo " Baue und pushe $SERVICE ..."
  docker build -t raphaelruoff/$SERVICE:1.0 ./services/$SERVICE
  docker push raphaelruoff/$SERVICE:1.0
done
