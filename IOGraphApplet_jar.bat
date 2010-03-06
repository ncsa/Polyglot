mkdir build\IOGraphApplet\edu\ncsa\icr\polyglot
xcopy bin\edu\ncsa\icr\polyglot\IOGraph*.class build\IOGraphApplet\edu\ncsa\icr\polyglot /i /y
xcopy bin\edu\ncsa\icr\polyglot\Conversion*.class build\IOGraphApplet\edu\ncsa\icr\polyglot /i /y
xcopy bin\edu\ncsa\icr\polyglot\Point2D*.class build\IOGraphApplet\edu\ncsa\icr\polyglot /i /y
xcopy bin\edu\ncsa\icr\polyglot\FadedScrollBarUI*.class build\IOGraphApplet\edu\ncsa\icr\polyglot /i /y

cd build/IOGraphApplet
jar xf ../../lib/ncsa/Utilities.jar
cd ../..

jar cf build/IOGraphApplet.jar -C build/IOGraphApplet .
jarsigner -signedjar build/IOGraphApplet-signed.jar build/IOGraphApplet.jar mykey