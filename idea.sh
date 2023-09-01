#!/usr/bin/env bash

idea authorization-server/build.gradle &
idea gateway/build.gradle &
idea api/build.gradle &
webstorm static/ &
cd static && run.sh