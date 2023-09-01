#!/bin/bash
mvn -q compile && mvn exec:java -e -q -Dexec.mainClass="com.aytao.rubiks.client.CommSheetValidator" -Dexec.args="$*"