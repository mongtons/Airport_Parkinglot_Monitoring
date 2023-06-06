@echo on
:_loop
set /a temp=%random% %%30+1
mosquitto_pub -t tmp -m "{\"tmp\" : %temp%}"
timeout /t 2 /nobreak > nul
goto _loop