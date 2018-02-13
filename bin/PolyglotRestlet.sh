

PolyglotJar=$( /bin/ls -1rt target/polyglot-*.jar 2>/dev/null | tail -1 )

echo "run polyglot:" $PolyglotJar

java -cp target/classes:$PolyglotJar:lib/kgm/* -Xmx1g edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotRestlet
