#!/bin/sh

# This script updates the version of the Checker Framework used in the tests to the version
# listed in the first argument.

NEW_CF_VERSION=$1

rm -rf checker/*
rm -rf checker-qual/*
rm -rf jdk8/*

mkdir "checker/${NEW_CF_VERSION}"
mkdir "checker-qual/${NEW_CF_VERSION}"
mkdir "jdk8/${NEW_CF_VERSION}"

cd "checker/${NEW_CF_VERSION}"
wget -O "checker-${NEW_CF_VERSION}.jar" "https://search.maven.org/remotecontent?filepath=org/checkerframework/checker/${NEW_CF_VERSION}/checker-${NEW_CF_VERSION}.jar"
wget -O "checker-${NEW_CF_VERSION}.pom" "https://search.maven.org/remotecontent?filepath=org/checkerframework/checker/${NEW_CF_VERSION}/checker-${NEW_CF_VERSION}.pom"
cd ../../

cd "checker-qual/${NEW_CF_VERSION}"
wget -O "checker-qual-${NEW_CF_VERSION}.jar" "https://search.maven.org/remotecontent?filepath=org/checkerframework/checker-qual/${NEW_CF_VERSION}/checker-qual-${NEW_CF_VERSION}.jar"
wget -O "checker-qual-${NEW_CF_VERSION}.pom" "https://search.maven.org/remotecontent?filepath=org/checkerframework/checker-qual/${NEW_CF_VERSION}/checker-qual-${NEW_CF_VERSION}.pom"
cd ../../

cd "jdk8/${NEW_CF_VERSION}"
wget -O "jdk8-${NEW_CF_VERSION}.jar" "https://search.maven.org/remotecontent?filepath=org/checkerframework/jdk8/${NEW_CF_VERSION}/jdk8-${NEW_CF_VERSION}.jar"
wget -O "jdk8-${NEW_CF_VERSION}.pom" "https://search.maven.org/remotecontent?filepath=org/checkerframework/jdk8/${NEW_CF_VERSION}/jdk8-${NEW_CF_VERSION}.pom"
cd ../../
