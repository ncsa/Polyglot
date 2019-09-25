#!/bin/bash
java -cp target/classes:target/polyglot-2.2.0-SNAPSHOT.jar:lib/kgm/* -Xmx1g edu.illinois.ncsa.isda.softwareserver.SoftwareServerRestlet
#java -cp target/original-polyglot-2.1.0-SNAPSHOT.jar:lib/maven/* -Xmx1g edu.illinois.ncsa.isda.softwareserver.SoftwareServerRestlet
