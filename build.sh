#!/bin/bash

PROGRESS_FILE=./Listeners/PathCountEstimator.java
PRINTER_FILE=./Listeners/ProgressPrinter.java
BUILD_CLASS_PATH=./build/jpf.jar:./Listeners
EXAMPLE_CLASS=ProgressExample

javac -cp $BUILD_CLASS_PATH $PRINTER_FILE && \
javac -cp $BUILD_CLASS_PATH $PROGRESS_FILE
#./jpf-core/bin/jpf +native_classpath=./Listeners +classpath=test +listener=PathCountEstimator $EXAMPLE_CLASS
