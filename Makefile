all: build
build:
	mvn package -Dmaven.test.skip=true
run:
	mvn package exec:java -Dmaven.test.skip=true
test:
	mvn test

.PHONY: build run test
