;OpenOffice (v3.1.0)
;document
;doc, odt, rtf, txt
;doc, odt, pdf, rtf, txt

;Run program
Run, "C:\Program Files\OpenOffice.org 3\program\soffice.exe" -headless -norestore "-accept=socket`,host=localhost`,port=8100;urp;StarOffice.ServiceManager"
RunWait, "C:\Program Files\OpenOffice.org 3\program\python.exe" "C:\Converters\DocumentConverter.py" "%1%" "%2%"