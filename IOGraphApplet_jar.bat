mkdir tmp\snapshot\IOGraphApplet_jar\edu\ncsa\icr\polyglot
xcopy bin\edu\ncsa\icr\polyglot\IOGraph*.class tmp\snapshot\IOGraphApplet_jar\edu\ncsa\icr\polyglot /i /y
xcopy bin\edu\ncsa\icr\polyglot\Conversion*.class tmp\snapshot\IOGraphApplet_jar\edu\ncsa\icr\polyglot /i /y
xcopy bin\edu\ncsa\icr\polyglot\Point2D*.class tmp\snapshot\IOGraphApplet_jar\edu\ncsa\icr\polyglot /i /y

cd tmp/snapshot/IOGraphApplet_jar
jar xf ../../../lib/ncsa/Utilities.jar
REM jar xf ../../../lib/mysql-connector-java-5.1.10/mysql-connector-java-5.1.10-bin.jar
cd ../../..

jar cf tmp/snapshot/IOGraphApplet.jar -C tmp/snapshot/IOGraphApplet_jar .
jarsigner -signedjar tmp/snapshot/IOGraphApplet-signed.jar tmp/snapshot/IOGraphApplet.jar mykey