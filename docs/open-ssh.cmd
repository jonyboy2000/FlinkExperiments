
set USERNAME=chgeuer
set SSHPRIV=%USERPROFILE%\Java\keys\dcos.putty.ppk
set IP=13.73.154.72

pageant.exe %SSHPRIV%
putty.exe -ssh -2 -l %USERNAME% -L 8081:localhost:8081 -P 22 -agent -A -i %SSHPRIV% %IP%



REM "C:\Users\chgeuer\bin\WinSCP.exe" /newinstance scp://%USERNAME%@%IP%/home/%USERNAME%/
