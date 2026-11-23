# ReservaHub — Pruebas de aceptación (Serenity BDD + Screenplay)

Pruebas automatizadas de la aplicación web de ReservaHub con **Serenity BDD**, el patrón **Screenplay** y **Cucumber** (Gherkin en español). Mismo esquema que el proyecto `taller-calidad-screenplay`: `interactions`, `models`, `questions`, `tasks`, `userinterfaces`, `utils`, `runners`, `stepdefinitions` y `features/<área>`.

## Requisitos

- JDK 17 o superior (probado con JDK 21)
- Google Chrome
- La aplicación corriendo. Desde la raíz del repositorio:

  ```bash
  docker compose up -d --build
  ```

  Frontend en http://localhost:3001 (las pruebas apuntan ahí por defecto).

No hace falta instalar Gradle: se usa el wrapper (`gradlew` / `gradlew.bat`).

## Ejecutar

```bash
cd acceptance-tests

./gradlew clean test                                          # todo, con navegador visible
./gradlew clean test -Dheadless.mode=true                     # sin ventana
./gradlew clean test -Dcucumber.filter.tags="@cliente"        # solo un área
./gradlew clean test -Dcucumber.filter.tags="@e2e"            # solo los recorridos completos
./gradlew clean test -Dcucumber.filter.tags="@proveedor and @reportes"
./gradlew clean test -Dcucumber.filter.name="Reservar el primer horario libre"
./gradlew clean test -Dreservahub.url=http://localhost:5173   # contra `npm run dev`
```

En Windows (PowerShell/cmd) se usa `gradlew.bat` con los mismos argumentos.

`DEMO_DELAY` (milisegundos) controla las pausas de demostración entre acciones: `DEMO_DELAY=1500` para presentarlas despacio, `DEMO_DELAY=0` para correr lo más rápido posible.

### Reporte

Serenity genera el reporte HTML con capturas en `target/site/serenity/index.html`. El plugin lo agrega al terminar `test`; si no aparece, se genera con `./gradlew aggregate`.

## Estructura

```text
acceptance-tests/
├── build.gradle · settings.gradle · gradle.properties · serenity.properties · gradlew
└── src/
    ├── main/java/co/edu/udea/calidad/reservahub/
    │   ├── interactions/     # OpenCleanSession, OpenPage, Pause
    │   ├── models/           # ClientAccount, ProviderAccount, ServiceInfo, ResourceInfo, Reservation
    │   ├── questions/        # TextOf, TextsOf, CurrentPage, ToastMessage, ... (lo que el actor observa)
    │   ├── tasks/            # LoginAs, RegisterAsClient, NavigateTo, ... (lo que el actor hace en la UI)
    │   │   └── api/          # preparación de datos por API: cuentas, servicios, horarios, reservas
    │   ├── userinterfaces/   # Targets por página: CommonUI, AccessUI, ...
    │   └── utils/            # Pages (rutas), TestData (datos únicos), ActorMemory (memoria del actor)
    └── test/
        ├── java/co/edu/udea/calidad/reservahub/
        │   ├── runners/TestRunner.java
        │   └── stepdefinitions/   # Hooks, Actors, pasos comunes, precondiciones y uno por área
        └── resources/
            ├── serenity.conf
            └── features/
                ├── acceso/        # login, logout, bloqueo, registro de clientes y proveedores, permisos
                ├── cliente/       # panel, catálogo, reservar, mis reservas, historial
                ├── proveedor/     # panel, servicios, recursos, horarios, agenda, reservar a cliente,
                │                  # historial del negocio, reportes
                └── e2e/           # recorridos completos del cliente, del proveedor y entre ambos
```

## Cómo están escritas

- **Actores**: "el cliente", "el proveedor", "otro cliente" y "alguien" (visitante sin cuenta), definidos en `stepdefinitions/Actors.java`. Los pasos `Entonces` los responde el actor que actuó último (el del foco).
- **`Dado` por API, `Cuando`/`Entonces` por la UI**: las precondiciones (`PreconditionStepDefinitions`) crean cuentas, servicios, horarios y reservas llamando a la API con la habilidad `CallAnApi`. Es más rápido y no depende de otras pantallas. Lo que se prueba ocurre en el navegador.
- **Datos únicos**: correos, servicios y recursos llevan un sufijo por ejecución (`TestData.unique()`), así que se puede correr la suite muchas veces sobre la misma base de datos.
- **Agenda siempre abierta**: el proveedor de las precondiciones atiende de 00:00 a 23:30 todos los días, para que haya horarios libres a cualquier hora en que corran las pruebas. Las fechas se calculan en la zona `America/Bogota`, igual que la API.
- **Selectores estables**: los `Target` usan los atributos `name` y `data-testid` del frontend. Los CSS llevan el prefijo `css:` y los XPath empiezan por `//`.

## Etiquetas

| Etiqueta | Qué corre |
|----------|-----------|
| `@acceso` | `@login`, `@registro` (`@registro_cliente`, `@registro_proveedor`), `@permisos` |
| `@cliente` | todo lo del cliente (HU-11, 12, 14, 16, 17 y catálogo) |
| `@proveedor` | todo lo del proveedor (HU-05 a 10, 13, 15, 16, 18 a 21) |
| `@e2e` | recorridos completos |
