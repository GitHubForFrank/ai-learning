@REM #########################################################  
@REM  Name: 递归删除指定的目录，请把此文件放在你希望执行的那个目录  
@REM  Desciption:   
@REM  Author: Frank  
@REM  Date: 2021-04-27  
@REM  Version: 1.0  
@REM  Copyright: Up to you.  
@REM #########################################################  

@REM 引用内容 引用内容
@REM EQU - 等于
@REM NEQ - 不等于
@REM LSS - 小于
@REM LEQ - 小于或等于
@REM GTR - 大于
@REM GEQ - 大于或等于

@echo off
setlocal enabledelayedexpansion  
set CURRENT_FOLDER=%cd%


@REM 设置你想删除的目录  
set WHAT_SHOULD_BE_DELETED=target
set WHAT_SHOULD_NOT_BE_DELETED=%CURRENT_FOLDER%\%WHAT_SHOULD_BE_DELETED%
ECHO.
echo will not remove folder : %WHAT_SHOULD_NOT_BE_DELETED%
for /r . %%a in (!WHAT_SHOULD_BE_DELETED!) do ( 
	if exist %%a (  
		if "%WHAT_SHOULD_NOT_BE_DELETED%" NEQ "%%a" ( 
			echo begin remove folder : %%a
			rd /s /q "%%a"  
		)
	)
)

pause