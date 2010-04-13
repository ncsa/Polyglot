[Setup]
AppName=Polyglot2
AppVerName=Polyglot 2.0
AppPublisher=NCSA
AppPublisherURL=http://isda.ncsa.uiuc.edu
AppSupportURL=http://isda.ncsa.uiuc.edu
AppUpdatesURL=http://isda.ncsa.uiuc.edu
UsePreviousAppDir=no
DefaultDirName={userdesktop}\Polyglot2
DefaultGroupName=Polyglot2
Compression=lzma
SolidCompression=yes
OutputDir=build
OutputBaseFilename=Polyglot2

[Files]
Source: "lib\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "scripts\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "build\Polyglot2.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "ICRServer.ini"; DestDir: "{app}"; Flags: ignoreversion
Source: "ICRServer_cwd.bat.txt"; DestDir: "{app}"; DestName: "PolyglotDaemon.bat"; Flags: ignoreversion

[Dirs]
Name: "{app}/tmp"
