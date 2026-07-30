@echo off
chcp 65001 >nul
echo ========================================
echo   咖啡店数据库完整导入脚本
echo ========================================
echo.

:: 尝试查找 MySQL 路径
set MYSQL_PATH=

:: 常见的 MySQL 安装路径
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe
    goto :found
)
if exist "C:\Program Files\MySQL\MySQL Server 8.1\bin\mysql.exe" (
    set MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.1\bin\mysql.exe
    goto :found
)
if exist "C:\Program Files\MySQL\MySQL Server 8.2\bin\mysql.exe" (
    set MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.2\bin\mysql.exe
    goto :found
)
if exist "C:\Program Files\MySQL\MySQL Server 8.3\bin\mysql.exe" (
    set MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.3\bin\mysql.exe
    goto :found
)
if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" (
    set MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe
    goto :found
)
if exist "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe" (
    set MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe
    goto :found
)
if exist "C:\xampp\mysql\bin\mysql.exe" (
    set MYSQL_PATH=C:\xampp\mysql\bin\mysql.exe
    goto :found
)
if exist "D:\xampp\mysql\bin\mysql.exe" (
    set MYSQL_PATH=D:\xampp\mysql\bin\mysql.exe
    goto :found
)
if exist "C:\wamp64\bin\mysql\mysql8.0.31\bin\mysql.exe" (
    set MYSQL_PATH=C:\wamp64\bin\mysql\mysql8.0.31\bin\mysql.exe
    goto :found
)
if exist "C:\laragon\bin\mysql\mysql-8.0.30-winx64\bin\mysql.exe" (
    set MYSQL_PATH=C:\laragon\bin\mysql\mysql-8.0.30-winx64\bin\mysql.exe
    goto :found
)

:: 如果没找到，让用户手动输入
echo 未找到 MySQL，请手动输入 mysql.exe 的完整路径
echo 例如: C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe
echo.
set /p MYSQL_PATH=请输入路径: 

if not exist "%MYSQL_PATH%" (
    echo.
    echo 错误: 找不到指定的 mysql.exe 文件
    echo.
    pause
    exit /b 1
)

:found
echo 找到 MySQL: %MYSQL_PATH%
echo.

set /p MYSQL_USER=请输入MySQL用户名 (默认root): 
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_PASS=请输入MySQL密码: 

echo.
echo 正在导入数据库...
echo.

"%MYSQL_PATH%" -u %MYSQL_USER% -p%MYSQL_PASS% < "%~dp0coffee_shop_complete.sql"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   导入成功！
    echo ========================================
) else (
    echo.
    echo ========================================
    echo   导入失败，请检查错误信息
    echo ========================================
)

echo.
pause
