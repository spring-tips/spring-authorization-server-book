#!/usr/bin/env bash 
rm -rf buld 
./gradlew nativeCompile && ./build/native/nativeCompile/authorization-server