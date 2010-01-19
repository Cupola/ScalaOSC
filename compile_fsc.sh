#!/bin/sh
echo "Compiling..."
fsc -deprecation -cp libraries/scalacheck_2.8.0.Beta1-RC1-1.7-SNAPSHOT.jar -d build/classes/ -sourcepath src/ src/de/sciss/scalaosc/*.scala
echo "Archiving..."
jar cf build/ScalaOSC.jar -C build/classes/ .
jar uf build/ScalaOSC.jar -C resources/ .
echo "Done."