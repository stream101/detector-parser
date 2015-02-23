#!/bin/bash

JAVA_CLASSPATH="\
lib/gson-2.3.1.jar:\
../detector/build/jar/soot-infoflow-android.jar:\
build/jar/detector-parser.jar:\
"
ant jar
java -cp $JAVA_CLASSPATH Parser $1
