all: build
build:
	mvn package -Dmaven.test.skip=true
run:
	mvn package exec:java -Dmaven.test.skip=true
test:
	mvn package -Dmaven.test.skip=false

.PHONY: build run test