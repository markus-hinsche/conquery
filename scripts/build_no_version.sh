#!/bin/bash

mvn -T 1C package -Dmaven.test.skip=true -DskipTests -pl executable -am
