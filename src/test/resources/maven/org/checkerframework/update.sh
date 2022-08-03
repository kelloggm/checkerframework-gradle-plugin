#!/bin/sh

# This script updates the version of the Checker Framework used in the tests to the version
# listed in the first argument.

NEW_CF_VERSION=$1

rm -rf checker/*
rm -rf checker-qual/*
rm -rf checker-util/*

mkdir -p "checker/${NEW_CF_VERSION}"
mkdir -p "checker-qual/${NEW_CF_VERSION}"
mkdir -p "checker-util/${NEW_CF_VERSION}"

cd "checker/${NEW_CF_VERSION}"
wget -O "checker-${NEW_CF_VERSION}.jar" "https://repo1.maven.org/maven2/org/checkerframework/checker/${NEW_CF_VERSION}/checker-${NEW_CF_VERSION}.jar"
wget -O "checker-${NEW_CF_VERSION}.pom" "https://repo1.maven.org/maven2/org/checkerframework/checker/${NEW_CF_VERSION}/checker-${NEW_CF_VERSION}.pom"
cd ../../

cd "checker-qual/${NEW_CF_VERSION}"
wget -O "checker-qual-${NEW_CF_VERSION}.jar" "https://repo1.maven.org/maven2/org/checkerframework/checker-qual/${NEW_CF_VERSION}/checker-qual-${NEW_CF_VERSION}.jar"
wget -O "checker-qual-${NEW_CF_VERSION}.pom" "https://repo1.maven.org/maven2/org/checkerframework/checker-qual/${NEW_CF_VERSION}/checker-qual-${NEW_CF_VERSION}.pom"
cd ../../

cd "checker-util/${NEW_CF_VERSION}"
wget -O "checker-util-${NEW_CF_VERSION}.jar" "https://repo1.maven.org/maven2/org/checkerframework/checker-util/${NEW_CF_VERSION}/checker-util-${NEW_CF_VERSION}.jar"
wget -O "checker-util-${NEW_CF_VERSION}.pom" "https://repo1.maven.org/maven2/org/checkerframework/checker-util/${NEW_CF_VERSION}/checker-util-${NEW_CF_VERSION}.pom"
cd ../../

