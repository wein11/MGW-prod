package com.mgwprod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Punto de entrada de toda la aplicación. @SpringBootApplication activa el auto-config
// de Spring Boot y el escaneo de componentes (@Service, @RestController, @Repository,
// etc.) en todo el paquete com.mgwprod y sus subpaquetes.
@SpringBootApplication
public class MgwProdApplication {
    // Arranca Spring Boot: levanta el servidor en el puerto 8080 y crea todos los beans.
    public static void main(String[] args) {
        SpringApplication.run(MgwProdApplication.class, args);
    }
}
