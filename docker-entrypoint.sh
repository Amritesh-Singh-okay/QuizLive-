#!/bin/sh
set -e

# Support dynamic port assignment for Render, Railway, Fly.io, Heroku, Cloud Run
TARGET_PORT="${PORT:-8080}"
echo "Configuring Tomcat port to ${TARGET_PORT}..."
sed -i "s/port=\"[0-9]*\" protocol=\"HTTP\/1.1\"/port=\"${TARGET_PORT}\" protocol=\"HTTP\/1.1\"/g" /usr/local/tomcat/conf/server.xml
sed -i "s/port=\"8080\"/port=\"${TARGET_PORT}\"/g" /usr/local/tomcat/conf/server.xml

echo "Starting QuizLive Tomcat server on port ${TARGET_PORT}..."
exec catalina.sh run
