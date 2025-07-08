#!/bin/bash

# Start script for item-handler

PORT=8080
exec java -jar -Dserver.port="${PORT}" -XX:MaxRAMPercentage=80 "item-handler.jar"
