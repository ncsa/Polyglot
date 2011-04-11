taskkill /f /im java.exe
cd "%~dp0"
call ScriptInstaller -shortcut \"%1\"
SoftwareServerRestlet