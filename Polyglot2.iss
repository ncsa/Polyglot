[Setup]
AppName=Polyglot2
AppVerName=Polyglot 2.1
AppPublisher=NCSA
AppPublisherURL=http://isda.ncsa.illinois.edu
AppSupportURL=http://isda.ncsa.illinois.edu
AppUpdatesURL=http://isda.ncsa.illinois.edu
UsePreviousAppDir=no
DefaultDirName={userdesktop}\Polyglot2
DefaultGroupName=Polyglot2
Compression=lzma
SolidCompression=yes
OutputDir=build
OutputBaseFilename=Polyglot2

[Files]
Source: "lib\maven\*"; DestDir: "{app}\lib"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "scripts\*"; DestDir: "{app}\scripts"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "target\polyglot-2.1.0-SNAPSHOT.jar"; DestDir: "{app}"; Flags: ignoreversion; AfterInstall: BuildDefaultConfiguration

[Dirs]
Name: "{app}/tmp"

[Code]
procedure BuildDefaultConfiguration;
var
  output: string;
begin
  output := ExpandConstant('{app}') + '\SoftwareServer.bat'
  SaveStringToFile(output, 'java -cp "%~dp0polyglot-2.1.0-SNAPSHOT.jar;%~dp0lib/*" -Xmx1g edu.ncsa.softwareserver.SoftwareServer %*', false);

  output := ExpandConstant('{app}') + '\SoftwareServer.conf'
  SaveStringToFile(output, 'RootPath=tmp' + #10, false);
  SaveStringToFile(output, 'Port=30' + #10, true);
  SaveStringToFile(output, 'MaxOperationTime=30000' + #10, true);
  SaveStringToFile(output, 'MaxOperationAttempts=2' + #10, true);
  SaveStringToFile(output, 'EnableMonitors=true' + #10, true);
  SaveStringToFile(output, 'AHKScripts=scripts/ahk', true);
end;
