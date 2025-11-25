@echo off
REM Script de compilación y ejecución para el Simulador con GUI
REM Asegúrate de configurar la ruta de JavaFX SDK

REM ===== CONFIGURACIÓN =====
REM Edita esta línea con la ruta a tu JavaFX SDK
SET JAVAFX_PATH=C:\Program Files\Java\javafx-sdk-17.0.17\lib

REM Verifica que la ruta existe
IF NOT EXIST "%JAVAFX_PATH%" (
    echo [ERROR] No se encontró JavaFX SDK en: %JAVAFX_PATH%
    echo.
    echo Por favor:
    echo 1. Descarga JavaFX SDK de https://gluonhq.com/products/javafx/
    echo 2. Extrae el archivo
    echo 3. Edita este script y actualiza la variable JAVAFX_PATH
    echo.
    pause
    exit /b 1
)

echo ========================================
echo   Simulador de Sistema Operativo
echo   Compilación y Ejecución
echo ========================================
echo.

cd /d "%~dp0sistemaoperativo"

REM Crear directorio bin si no existe
IF NOT EXIST "bin" mkdir bin

echo [1/3] Compilando código fuente...
echo.

REM Compilar todos los archivos Java
javac --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -d bin -sourcepath src\main src\main\ui\SimulatorGUI.java src\main\main\GUILauncher.java src\main\main\OSSimulator.java 2>compile_errors.txt

IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] La compilación falló. Revisa compile_errors.txt
    type compile_errors.txt
    pause
    exit /b 1
)

echo [OK] Compilación exitosa
echo.

echo [2/3] Copiando recursos...
IF EXIST "src\main\resources" (
    xcopy /Y /Q "src\main\resources\*" "bin\" >nul 2>&1
    echo [OK] Recursos copiados
) ELSE (
    echo [INFO] No hay recursos para copiar
)
echo.

echo [3/3] Iniciando la interfaz gráfica...
echo.
echo ========================================
echo   Cerrando esta ventana detendrá la GUI
echo ========================================
echo.

java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -cp bin main.GUILauncher

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Error al ejecutar la aplicación
    pause
    exit /b 1
)

echo.
echo Aplicación cerrada correctamente.
pause
