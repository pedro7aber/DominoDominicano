# Dominó Dominicano de Élite

**APK Android 13+ (minSdk 33 / targetSdk 35)**  
Juego completo de Dominó Dominicano con reglas definitivas v3, IA de alto nivel y UI procedural Canvas de alta calidad.

## Cómo obtener el APK (1 clic)

1. Ve a la pestaña **Actions**
2. Selecciona el workflow **Build Dominó Dominicano APK**
3. Pulsa **Run workflow** → **Run workflow**
4. Espera ~3-6 minutos
5. En el run terminado, baja el artefacto **DominoDominicano-debug**  
   (o descarga el APK desde el Release que se crea automáticamente)

El APK viene firmado con keystore de debug → **listo para instalar** en cualquier dispositivo Android 13+.

```bash
adb install DominoDominicano-debug.apk
```

## Características

- **Reglas v3 estrictas e inquebrantables** (archivo de reglas oficiales)
- 4 jugadores, 2 parejas, sentido antihorario, sale el 6-6
- Obligación de jugar, dobles transversales, conteo configurable
- Bonos: Capicúa, Paso Redondo, Pase de Salida, Chuchazo (opcional)
- Tranque individual o por parejas
- Meta configurable (100–400)
- **IA de élite** (3 instancias con personalidades distintas)
- UI 100 % procedural (Compose Canvas) siguiendo la captura de referencia
- Español (default) + Inglés

## Estructura

```
app/src/main/java/com/lamesa/dominodominicano/
├── domain/          # Motor de reglas (fuente de verdad)
├── ai/              # IA heurística de alto nivel
├── ui/game/         # Pantalla de juego + ViewModel
├── ui/components/   # Fichas procedurales
└── ...
```

## Build local

```bash
./gradlew assembleDebug
```

---

Hecho con rigor máximo para la variante dominicana.
