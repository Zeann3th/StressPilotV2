APP_NAME=stresspilot
JAR_FILE=$(shell ls target/*.jar | head -n 1)
IMAGE_NAME=stresspilot:latest

.PHONY: all build-docker clean

all: build-docker

build-docker:
	mvn clean package -DskipTests
	docker build -t $(IMAGE_NAME) .

clean:
	mvn clean
	docker rmi -f $(IMAGE_NAME) || true