#!/bin/bash
mvn compile -q && mvn exec:java -e -q -Dexec.mainClass="com.aytao.rubiks.client.CommSheetValidator" -Dexec.args="%*"