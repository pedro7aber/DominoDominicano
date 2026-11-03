# Dominó Dominicano

Juego de Dominó Dominicano para Android 13+ (minSdk 33, targetSdk 35).

## Características

- **Reglas v3** estrictas (parejas, 6-6 de salida, Capicúa, Pase de Salida, Paso Redondo, Chuchazo, Tranque)
- **IA** con conteo, memoria de pases, protección de pareja y bloqueo
- **Configuración de partida**: meta, bonos, tipo de tranque, Chuchazo
- **Layout de mesa** con ficha central y dos ramas
- UI en Jetpack Compose

## Cómo obtener el APK

1. Ve a [Actions](https://github.com/pedro7aber/DominoDominicano/actions)
2. Abre el último run exitoso de **Build Dominó Dominicano APK**
3. Descarga el artifact `DominoDominicano-debug`
4. Instala en tu dispositivo (permite orígenes desconocidos)

## Estructura

```
app/src/main/java/com/lamesa/dominodominicano/
  domain/     → reglas y estado (fuente de verdad)
  ai/         → DominoAI
  ui/         → Compose screens + BoardLayout + DominoTile
  data/       → settings (idioma)
```

## Build local

```bash
./gradlew assembleDebug
```

## Verificación

Ver `verification.txt` para el checklist de cumplimiento del prompt original.
