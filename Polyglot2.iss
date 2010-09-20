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
Source: "lib\*"; DestDir: "{app}\lib"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "scripts\*"; DestDir: "{app}\scripts"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "build\Polyglot2.jar"; DestDir: "{app}"; Flags: ignoreversion; AfterInstall: BuildDefaultConfiguration

[Dirs]
Name: "{app}/tmp"

[Code]
procedure BuildDefaultConfiguration;
var
  output: string;
begin
  output := ExpandConstant('{app}') + '\SoftwareReuseServer.bat'
  SaveStringToFile(output, 'java -cp "%~dp0lib/ncsa/Utilities.jar;%~dp0Polyglot2.jar" -Xmx1024m edu.ncsa.icr.SoftwareReuseServer %1 %2', false);

  output := ExpandConstant('{app}') + '\SoftwareReuseServer.ini'
  SaveStringToFile(output, 'RootPath=tmp' + #10, false);
  SaveStringToFile(output, 'Port=30' + #10, true);
  SaveStringToFile(output, 'MaxOperationTime=30000' + #10, true);
  SaveStringToFile(output, 'MaxOperationAttempts=2' + #10, true);
  SaveStringToFile(output, 'EnableMonitors=true' + #10, true);
  SaveStringToFile(output, 'AHKScripts=scripts/ahk', true);
end;
