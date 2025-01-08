package com.webservice.dataminer.controllers;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

class Producto {
    private String marca;
    private String precio;
    private String ram;
    private String storage;
    private String inches;
    private String peso;

    // Constructor
    public Producto(String marca, String precio, String ram, String almacenamiento, String pantalla, String peso) {
        this.marca = marca;
        this.precio = precio;
        this.ram = ram;
        this.storage = almacenamiento;
        this.inches = pantalla;
        this.peso = peso;
    }

    // Getters y setters
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getPrecio() { return precio; }
    public void setPrecio(String precio) { this.precio = precio; }
    public String getRam() { return ram; }
    public void setRam(String ram) { this.ram = ram; }
    public String getAlmacenamiento() { return storage; }
    public void setAlmacenamiento(String almacenamiento) { this.storage = almacenamiento; }
    public String getPantalla() { return inches; }
    public void setPantalla(String pantalla) { this.inches = pantalla; }
    public String getPeso() { return peso; }
    public void setPeso(String peso) { this.peso = peso; }
}

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200") // Habilita CORS solo para este controlador
public class MiningRestController {
    // Ruta del archivo CSV en el sistema de archivos local
    private static final String CSV_FILE_PATH = "C:\\Users\\Dell\\Downloads\\laptop_prices.csv";

    @PostMapping("/process-csv")
    public ResponseEntity<?> processCsv(@RequestBody Map<String, String> values) {
        File file = new File(CSV_FILE_PATH);

        // Verificar si el archivo existe
        if (!file.exists() || file.isDirectory()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Archivo CSV no encontrado en la ruta especificada");
        }

        try {
            List<Map<String, String>> data = MiningRestController.loadData(file);
            // Lista para almacenar los productos
            List<Producto> productos = new ArrayList<>();

            // Filtrar y almacenar los resultados con el formato deseado
            for (Map<String, String> laptop : data) {
                String decision = makeDecision(laptop, values);
                if (!decision.contains("Descartado")) { // Si la laptop no fue descartada
                    String[] lineas = decision.split("\n");

                    // Convertir cada línea en un Producto
                    for (String linea : lineas) {
                        Producto producto = convertirStringAProducto(linea);
                        productos.add(producto);
                    }
                }
            }

            // Retornar el resultado como una cadena
            return ResponseEntity.status(HttpStatus.OK).header("Content-Type", "application/json").body(productos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar el archivo CSV");
        }
    }

    public static Producto convertirStringAProducto(String linea) {
        String[] atributos = linea.split(", ");

        // Extraer valores de cada atributo
        String marca = atributos[0].split(": ")[1];
        String precio = atributos[1].split(": ")[1];
        String ram = atributos[2].split(": ")[1];
        String almacenamiento = atributos[3].split(": ")[1];
        String pantalla = atributos[4].split(": ")[1];
        String peso = atributos[5].split(": ")[1];

        // Crear y devolver el objeto Producto
        return new Producto(marca, precio, ram, almacenamiento, pantalla, peso);
    }

    // Método para cargar los datos desde un archivo CSV
    public static List<Map<String, String>> loadData(File file) {
        List<Map<String, String>> data = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String[] headers = br.readLine().split(","); // Leer los encabezados del CSV

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], values[i]);
                }
                data.add(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    // Método para tomar la decisión en función de los criterios del árbol de decisiones
    public static String makeDecision(Map<String, String> laptop, Map<String, String> values) {
        // Filtrar por precio
        double price = Double.parseDouble(laptop.get("Price_euros"));
        double presupuesto = Double.parseDouble(values.get("presupuesto"));
        if (price > presupuesto) {
            return "Descartado: Precio excede el presupuesto";
        }

        // Filtrar por RAM
        double ram = Double.parseDouble(laptop.get("Ram"));
        double uRam = Double.parseDouble(values.get("ram"));
        if (ram < uRam) {
            return "Descartado: RAM insuficiente";
        }

        // Filtrar por almacenamiento primario (SSD preferido)
        String storageType = laptop.get("PrimaryStorageType");
        if (!storageType.equalsIgnoreCase(values.get("storageType"))) {
            return "Descartado: Tipo de almacenamiento no aceptado";
        }

        // Filtrar por espacio de almacenamiento (mínimo 256GB)
        int storage = Integer.parseInt(laptop.get("PrimaryStorage"));
        int uStorage = Integer.parseInt(values.get("storage"));
        if (storage < uStorage) {
            return "Descartado: Almacenamiento insuficiente";
        }

        // Filtrar por peso
        double weight = Double.parseDouble(laptop.get("Weight"));
        double peso = Double.parseDouble(values.get("peso"));

        if (weight > peso) {
            return "Descartado: Peso excede el límite";
        }

        // Filtrar por tamaño de pantalla
        double inches = Double.parseDouble(laptop.get("Inches"));
        double size = Double.parseDouble(values.get("inches"));
        if (inches < size) {
            return "Descartado: Tamaño de pantalla fuera del rango aceptado";
        }

        // Si pasa todas las condiciones, se considera como una opción válida
        String brand = laptop.get("Company");

        return "Marca: " + brand + ", " + "Precio: " + laptop.get("Price_euros") + ", " + "Ram: " + laptop.get("Ram") + ", " + "Storage: " + laptop.get("PrimaryStorage") + ", " + "Inches: " + laptop.get("Inches") + ", " + "Peso: " + laptop.get("Weight");
    }
}
