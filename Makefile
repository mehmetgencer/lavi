CLASSPATH=-classpath ./lib/gs-core-1.1.2.jar:./lib/super-csv-2.0.1.jar
SCALAROOT=~/scala-2.10.0-RC3/

default: scalacompile scalaruntest
	echo "DONE"

scalaclear:
	rm -fr ./dist/*

scalacompile: scalaclear
	~/scala-2.10.0-RC3/bin/scalac -feature -language:higherKinds -d dist -deprecation $(CLASSPATH) src/*.scala

scalaruntest:
	~/scala-2.10.0-RC3/bin/scala $(CLASSPATH):./dist lavicore.LaviShell test

scalarunsinternals:
	~/scala-2.10.0-RC3/bin/scala $(CLASSPATH):./dist lavicore.LaviShell testdata/scalainternals.lax

docs:
	$(SCALAROOT)/bin/scaladoc $(CLASSPATH) -d doc src/*.scala
