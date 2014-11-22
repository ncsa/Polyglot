#!/bin/bash
SCRIPT_DIR="$(dirname $(readlink -f ${BASH_SOURCE[0]}))"
PROJECT_DIR=$SCRIPT_DIR/../../../../daffodil
MAINCLASS=edu.illinois.ncsa.daffodil.Main


exec java $JAVA_OPTS -cp "$PROJECT_DIR/daffodil-cli/target/scala-2.10/daffodil-cli_2.10-0.13.0.jar:$PROJECT_DIR/daffodil-tdml/target/scala-2.10/daffodil-tdml_2.10-0.13.0.jar:$PROJECT_DIR/daffodil-runtime1/target/scala-2.10/daffodil-runtime1_2.10-0.13.0.jar:$PROJECT_DIR/daffodil-core/target/scala-2.10/daffodil-core_2.10-0.13.0.jar:$PROJECT_DIR/daffodil-io/target/scala-2.10/daffodil-io_2.10-0.13.0.jar:$PROJECT_DIR/daffodil-lib/target/scala-2.10/daffodil-lib_2.10-0.13.0.jar:$PROJECT_DIR/daffodil-lib/lib/icu4j-localespi-51_1.jar:$PROJECT_DIR/daffodil-lib/lib/icu4j-charset-51_1.jar:$PROJECT_DIR/.ivy2/cache/org.scala-lang/scala-library/jars/scala-library-2.10.4.jar:$PROJECT_DIR/.ivy2/cache/junit/junit/jars/junit-4.11.jar:$PROJECT_DIR/.ivy2/cache/org.hamcrest/hamcrest-core/jars/hamcrest-core-1.3.jar:$PROJECT_DIR/.ivy2/cache/com.novocode/junit-interface/jars/junit-interface-0.10-M4.jar:$PROJECT_DIR/.ivy2/cache/junit/junit-dep/jars/junit-dep-4.10.jar:$PROJECT_DIR/.ivy2/cache/org.scala-tools.testing/test-interface/jars/test-interface-0.5.jar:$PROJECT_DIR/.ivy2/cache/org.jdom/jdom2/jars/jdom2-2.0.5.jar:$PROJECT_DIR/.ivy2/cache/net.sf.saxon/Saxon-HE/jars/Saxon-HE-9.5.1-1.jar:$PROJECT_DIR/.ivy2/cache/net.sf.saxon/Saxon-HE-jdom2/jars/Saxon-HE-jdom2-9.5.1-1.jar:$PROJECT_DIR/.ivy2/cache/com.ibm.icu/icu4j/jars/icu4j-51.1.jar:$PROJECT_DIR/.ivy2/cache/xerces/xercesImpl/jars/xercesImpl-2.10.0.jar:$PROJECT_DIR/.ivy2/cache/xml-apis/xml-apis/jars/xml-apis-1.4.01.jar:$PROJECT_DIR/.ivy2/cache/xml-resolver/xml-resolver/jars/xml-resolver-1.2.jar:$PROJECT_DIR/.ivy2/cache/jline/jline/jars/jline-2.9.jar:$PROJECT_DIR/.ivy2/cache/org.rogach/scallop_2.10/jars/scallop_2.10-0.9.5.jar:$PROJECT_DIR/.sbt/boot/scala-2.10.3/lib/scala-reflect.jar:$PROJECT_DIR/.ivy2/cache/net.sourceforge.expectj/expectj/jars/expectj-2.0.7.jar:$PROJECT_DIR/.ivy2/cache/commons-logging/commons-logging/jars/commons-logging-1.1.1.jar:$PROJECT_DIR/.ivy2/cache/com.jcraft/jsch/jars/jsch-0.1.42.jar:$PROJECT_DIR/.ivy2/cache/commons-io/commons-io/jars/commons-io-2.4.jar" "$MAINCLASS" "$@"

