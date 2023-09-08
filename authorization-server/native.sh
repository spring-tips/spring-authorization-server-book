#!/usr/bin/env bash 
rm -rf build
./gradlew nativeCompile && ./build/native/nativeCompile/authorization-server
