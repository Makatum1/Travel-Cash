# Travel-Cash — Reglas del Proyecto

## Flujo de Trabajo Automático tras Modificaciones

Cada vez que se realice cualquier cambio o modificación en la aplicación y se vaya a dar por finalizada la tarea, se debe ejecutar OBLIGATORIAMENTE la siguiente secuencia de 4 pasos:

1. **Revisión de las modificaciones realizadas**: Comprobar el diff de cambios, validar la compilación sin errores ni advertencias y ejecutar los tests unitarios (`./gradlew testDebugUnitTest`) asegurando que todo funcione limpiamente y sin regresiones.
2. **Creación del APK en el lugar y nombre correspondiente**: Generar el nuevo APK firmado en Release (`./gradlew assembleRelease copyTravelCashReleaseApk`), verificando que siempre se llame `TravelCash.apk`, mantenga `versionCode = 1` y `versionName = "1.0"`, y se guarde/sobreescriba por defecto en:
   `C:\Users\perez\Desktop\basura\Aplicaciones\TravelCash\TravelCash.apk`
   (además de en `app/build/outputs/apk/release/TravelCash.apk`).
3. **Subida en el apartado correspondiente en GitHub**: Añadir los cambios (`git add`), realizar commit con un mensaje descriptivo y hacer push a la rama principal en GitHub (`git push origin main`), asegurando que el estado remoto y local quede limpio y sincronizado.
4. **Resumen del día**: Mostrar en la respuesta un resumen claro y estructurado con todo lo que se ha modificado y realizado durante ese día.

## Generación de APK

- **Nombre y Ubicación fija del APK**: Cada vez que se genere un APK, el archivo final generado debe llamarse siempre `TravelCash.apk` y guardarse (sobreescribiendo si ya existe) por defecto en:
  `C:\Users\perez\Desktop\basura\Aplicaciones\TravelCash\TravelCash.apk`
  (además de en `app/build/outputs/apk/release/TravelCash.apk`, mediante la tarea `copyTravelCashReleaseApk` configurada en `app/build.gradle.kts`).
- **Versión fija hasta nuevo aviso**: Siempre que se genere una APK, mantener exactamente `versionCode = 1` y `versionName = "1.0"` en `app/build.gradle.kts`. No incrementar, autogenerar ni modificar la versión salvo que el usuario lo indique explícitamente.
- **Firma de Release**: Las credenciales del keystore (`TRAVEL_CASH_STORE_FILE`, `TRAVEL_CASH_STORE_PASSWORD`, `TRAVEL_CASH_KEY_ALIAS`, `TRAVEL_CASH_KEY_PASSWORD`) se leen desde el `gradle.properties` del usuario (fuera del repositorio) y nunca deben escribirse ni subirse a archivos rastreados por Git.

## Navegación de pestañas

- **Sin desplazamiento lateral**: Mantener desactivado el desplazamiento lateral tipo swipe entre pestañas principales en `MainActivity.kt` (no usar `HorizontalPager`), navegando únicamente al pulsar las pestañas, salvo petición explícita del usuario.

## Verificación de Contexto entre Proyectos

- **Detección de términos ajenos**: El usuario gestiona dos proyectos en paralelo y puede confundir términos (por ejemplo, hacer mención a "Camino", "Etapas", "Eventos" u otros conceptos propios de su otra app). Si el usuario solicita un cambio mencionando conceptos, pantallas o entidades que no pertenecen a esta compilación de Travel-Cash (cuyo modelo es Viajes, Gastos, Bote Común, Participantes), se debe **detener y advertirle explícitamente antes de modificar nada**, consultándole cómo adaptar la petición o si se refería a su otro proyecto.
