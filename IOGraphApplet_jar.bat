mkdir tmp\snapshot\IOGraphApplet_jar\edu\ncsa\polyglot
xcopy bin\edu\ncsa\polyglot\iograph\IOGraph*.class tmp\snapshot\IOGraphApplet_jar\edu\ncsa\polyglot\iograph /i /y
xcopy bin\edu\ncsa\polyglot\iograph\TextPanel.class tmp\snapshot\IOGraphApplet_jar\edu\ncsa\polyglot\iograph /i /y
xcopy bin\edu\ncsa\polyglot\iograph\Utils*.class tmp\snapshot\IOGraphApplet_jar\edu\ncsa\polyglot\iograph /i /y

jar cf tmp/snapshot/IOGraphApplet.jar -C tmp/snapshot/IOGraphApplet_jar .
REM jarsigner -signedjar tmp/snapshot/IOGraphApplet-signed.jar tmp/snapshot/IOGraphApplet.jar mykey
