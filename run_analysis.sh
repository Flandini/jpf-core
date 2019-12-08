#!/bin/bash

MEMOIZATION_FILE=./Listeners/Memoization.java
CODECOVERAGE_FILE=./Listeners/CodeCoverage.java

javac -cp ./build/jpf.jar $MEMOIZATION_FILE $CODECOVERAGE_FILE && \
javac -cp ./build/jpf.jar test/*.java && \
  ./jpf-core/bin/jpf \
    +native_classpath=./Listeners \
    +classpath=test \
    +report.console.file=CodeCoverageReport.txt \
    +listener=CodeCoverage,Memoization \
    $1
