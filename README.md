TPO – Programación III
Optimización de Portafolios Financieros con Heurísticas y Branch & Bound


Descripción del Proyecto:

Este trabajo práctico tiene como objetivo desarrollar un sistema capaz de optimizar una cartera de inversión, seleccionando una combinación de activos financieros que maximice el retorno esperado sin superar un nivel de riesgo máximo permitido y cumpliendo con diversas restricciones (presupuesto, montos mínimos, topes por activo, tipo y sector).

El proyecto se desarrolló en Java, aplicando técnicas de búsqueda y optimización combinatoria vistas en la materia Programación III, como:

Heurísticas Greedy

Branch & Bound (Ramificación y Poda)

Análisis de complejidad y restricciones

Objetivos del sistema:

Cargar la información del mercado financiero (activos y correlaciones).

Validar coherencia, límites y formato de datos.

Generar una cartera factible inicial.

Aplicar un método Greedy para obtener una buena solución inicial.

Aplicar Branch & Bound para encontrar la solución óptima.

Comparar resultados y generar un reporte final con métricas.

 Estructura del Proyecto
TPO-PROGRA/
│
├── data/                # Archivos de entrada (activos, correlaciones, mercado.json)
│   ├── activos_financieros_60.csv
│   ├── correlaciones_60.csv
│   └── mercado.json
│
├── lib/                 # Librerías externas
│   └── gson-2.10.1.jar
│
├── src/
│   ├── io/              # Entrada/Salida y Reportes
│   ├── model/           # Clases del modelo de dominio
│   ├── validacion/      # Validaciones del sistema
│   ├── heuristicas/     # Métodos Greedy y Semilla Factible
│   └── optimizacion/    # Algoritmo Branch & Bound
│
├── App.java             # Programa principal
├── README.md            # Este archivo
└── .gitignore

 Componentes principales:
Módulo	Descripción
I/O	Lectura y escritura de archivos JSON / CSV.
Modelo	Representa los activos, mercado, perfil del cliente y asignaciones.
Validaciones	Controla límites de presupuesto, riesgo, tipos y sectores.
Semilla Factible	Construye una primera cartera siempre válida.
Greedy Inicial	Selecciona activos con mayor relación retorno/riesgo.
Branch & Bound	Explora combinaciones posibles con poda por cota superior.
Reporte	Imprime y exporta el resumen de resultados.

Metodología aplicada:

Greedy Heuristic
Construye una solución rápida eligiendo activos con mejor ratio retorno/sigma (rentabilidad por unidad de riesgo).
Cumple todas las restricciones y sirve como cota inferior para el Branch & Bound.

Branch & Bound (Ramificación y Poda)
Explora sistemáticamente el espacio de soluciones:

Cada nodo representa una decisión de invertir o no en un activo (o en múltiplos del monto mínimo).

Se calcula una cota superior optimista del retorno para decidir si expandir o podar la rama.

Se actualiza la mejor solución conocida cuando se encuentra una cartera válida con mayor retorno.

Cota superior con riesgo residual
Una mejora adicional considera el riesgo parcial de la cartera y ajusta la cota optimista según el “presupuesto de riesgo” restante, logrando una poda más eficiente.

 Ejecución del proyecto
 Requisitos

Java JDK 11 o superior.

Librería GSON (lib/gson-2.10.1.jar).

 Compilación y ejecución

Desde la raíz del proyecto:

Remove-Item -Recurse -Force bin 2>$null; New-Item -ItemType Directory -Path bin 1>$null;
javac -cp "lib\gson-2.10.1.jar" -d bin src\App.java src\io\CargadorDatosJson.java src\io\Reporte.java src\io\dto\ActivoJson.java src\io\dto\MercadoJson.java src\model\*.java src\validacion\*.java src\heuristicas\*.java src\optimizacion\BBPortafolio.java;
java -cp "bin;lib\gson-2.10.1.jar" App

 Ejemplo de salida:
 
Activos: 60
Matriz rho: 60 x 60
OK: Mercado válido
OK: Perfil válido
Cliente: Cliente Demo

--- SEMILLA ---
Presupuesto: 100000.00
Invertido:   95000.00
Retorno esp: 0.081
Riesgo (σ): 0.108 (max 0.250)

--- GREEDY ---
Presupuesto: 100000.00
Invertido:   95000.00
Retorno esp: 0.110
Riesgo (σ): 0.023

--- BRANCH & BOUND ---
Presupuesto: 100000.00
Invertido:   95000.00
Retorno esp: 0.110
Riesgo (σ): 0.023
Nodos visitados: 214

 Conclusiones

El sistema permite analizar portafolios de inversión respetando límites reales de riesgo y diversificación.

La heurística Greedy proporciona una cota inicial y soluciones de calidad en poco tiempo.

El algoritmo Branch & Bound garantiza la óptima global, aunque con mayor costo computacional.

Al combinar ambos métodos, se logra un equilibrio entre eficiencia y precisión.

Integrantes
Mones Ruiz Ignacio 
Gomez Francisco
Materia: Programación III

Carrera: Ingeniería en Informática – UADE

Año: 2025
