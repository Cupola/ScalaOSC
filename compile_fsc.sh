#!/bin/sh
echo "Compiling..."
fsc -deprecation -d out/production/ScalaOSC/ -sourcepath src/ src/de/sciss/scalaosc/*.scala
echo "Archiving..."
jar cf ScalaOSC.jar -C out/production/ScalaOSC/ .
jar uf ScalaOSC.jar -C resources/ .
echo "Done."