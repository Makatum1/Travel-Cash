# Travel-Cash — Reglas del Proyecto

## Flujo de Trabajo Automático tras Modificaciones

- **Subida a GitHub y Generación de APK**: Cada vez que se realice cualquier cambio o modificación en la aplicación, se debe automáticamente:
  1. Realizar commit con un mensaje descriptivo y hacer push a la rama principal en GitHub (`git push origin main`).
  2. Generar el nuevo APK firmado en Release (`./gradlew assembleRelease copyTravelCashReleaseApk`), asegurando que siempre quede como `TravelCash.apk`.

## Generación de APK

- **Nombre y Ubicación fija del APK**: Cada vez que se genere un APK, el archivo final generado debe llamarse siempre `TravelCash.apk` y guardarse (sobreescribiendo si ya existe) por defecto en:
  `C:\Users\perez\Desktop\basura\Aplicaciones\TravelCash\TravelCash.apk`
  (además de en `app/build/outputs/apk/release/TravelCash.apk`, mediante la tarea `copyTravelCashReleaseApk` configurada en `app/build.gradle.kts`).
- **Versión fija hasta nuevo aviso**: Siempre que se genere una APK, mantener exactamente `versionCode = 1` y `versionName = "1.0"` en `app/build.gradle.kts`. No incrementar, autogenerar ni modificar la versión salvo que el usuario lo indique explícitamente.
- **Firma de Release**: Las credenciales del keystore (`TRAVEL_CASH_STORE_FILE`, `TRAVEL_CASH_STORE_PASSWORD`, `TRAVEL_CASH_KEY_ALIAS`, `TRAVEL_CASH_KEY_PASSWORD`) se leen desde el `gradle.properties` del usuario (fuera del repositorio) y nunca deben escribirse ni subirse a archivos rastreados por Git.

## Navegación de pestañas

- **Sin desplazamiento lateral**: Mantener desactivado el desplazamiento lateral tipo swipe entre pestañas principales en `MainActivity.kt` (no usar `HorizontalPager`), navegando únicamente al pulsar las pestañas, salvo petición explícita del usuario.
