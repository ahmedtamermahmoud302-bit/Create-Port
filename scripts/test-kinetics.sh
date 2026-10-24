#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
compiler="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
runtime="${JAVA_HOME:+$JAVA_HOME/bin/}java"
mkdir -p build/kinetic-checks
"$compiler" -source 8 -target 8 -d build/kinetic-checks \
  src/main/java/dev/createport/kinetics/*.java \
  src/test/java/dev/createport/kinetics/*.java
"$runtime" -cp build/kinetic-checks dev.createport.kinetics.KineticGraphTest
"$runtime" -cp build/kinetic-checks dev.createport.kinetics.RotationRulesTest
"$runtime" -cp build/kinetic-checks dev.createport.kinetics.EnergyTest
