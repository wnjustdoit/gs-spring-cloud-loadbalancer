#!/bin/bash

cd complete/say-hello

SERVER_PORT=9999 mvn spring-boot:run

SERVER_PORT=9092 mvn spring-boot:run

SERVER_PORT=8090 mvn spring-boot:run


cd ../user

mvn spring-boot:run

Access: http://localhost:8080/hi
