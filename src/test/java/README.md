# Tests

Los tests son JUnit 5 y se ejecutan con `mvn test`. Los de integración usan PostgreSQL con Testcontainers, así que requieren Docker en ejecución.

El smoke test del scaffold base de `C-02` vive en `com/turnosodontologia/SmokeTest.java`. Originalmente era un harness sin dependencias en `tests/SmokeTest.java`, compilado con `javac`, de antes de que el proyecto usara Maven.
