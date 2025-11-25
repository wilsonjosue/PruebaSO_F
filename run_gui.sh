#!/bin/bash
# Script de compilación y ejecución para el Simulador con GUI (Linux/Mac)

# ===== CONFIGURACIÓN =====
# Edita esta línea con la ruta a tu JavaFX SDK
JAVAFX_PATH="/path/to/javafx-sdk-21/lib"

# Verifica que la ruta existe
if [ ! -d "$JAVAFX_PATH" ]; then
    echo "[ERROR] No se encontró JavaFX SDK en: $JAVAFX_PATH"
    echo ""
    echo "Por favor:"
    echo "1. Descarga JavaFX SDK de https://gluonhq.com/products/javafx/"
    echo "2. Extrae el archivo"
    echo "3. Edita este script y actualiza la variable JAVAFX_PATH"
    echo ""
    exit 1
fi

echo "========================================"
echo "  Simulador de Sistema Operativo"
echo "  Compilación y Ejecución"
echo "========================================"
echo ""

cd "$(dirname "$0")/sistemaoperativo"

# Crear directorio bin si no existe
mkdir -p bin

echo "[1/3] Compilando código fuente..."
echo ""

# Compilar todos los archivos Java
find src/main -name "*.java" > sources.txt

javac --module-path "$JAVAFX_PATH" \
      --add-modules javafx.controls,javafx.fxml \
      -d bin \
      @sources.txt 2> compile_errors.txt

if [ $? -ne 0 ]; then
    echo "[ERROR] La compilación falló. Revisa compile_errors.txt"
    cat compile_errors.txt
    rm sources.txt
    exit 1
fi

rm sources.txt
echo "[OK] Compilación exitosa"
echo ""

echo "[2/3] Copiando recursos..."
if [ -d "src/main/resources" ]; then
    cp -r src/main/resources/* bin/ 2>/dev/null
    echo "[OK] Recursos copiados"
else
    echo "[INFO] No hay recursos para copiar"
fi
echo ""

echo "[3/3] Iniciando la interfaz gráfica..."
echo ""
echo "========================================"
echo "  Cerrando esta ventana detendrá la GUI"
echo "========================================"
echo ""

java --module-path "$JAVAFX_PATH" \
     --add-modules javafx.controls,javafx.fxml \
     -cp bin \
     main.GUILauncher

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Error al ejecutar la aplicación"
    exit 1
fi

echo ""
echo "Aplicación cerrada correctamente."
