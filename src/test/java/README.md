# Placeholder de tests JUnit

Los tests JUnit de `src/test/java/` se incorporan en `C-03`, cuando se resuelvan y descarguen las dependencias declaradas en `pom.xml`.

En `C-02` no se ejecuta Maven ni se instalan dependencias. El verificación ejecutable de este change es el harness sin dependencias externas `tests/SmokeTest.java` (compilado con `javac`, ejecutado con `java`).
