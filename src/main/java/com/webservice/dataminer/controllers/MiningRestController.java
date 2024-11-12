package com.webservice.dataminer.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class MiningRestController {
    @PostMapping("/process-csv")
    public ResponseEntity<String> processCsv(@RequestParam("laptop_prices") MultipartFile file) {
        // Verificar si el archivo está vacío
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Archivo CSV vacío o no enviado");
        }

        try {
            List<Map<String, String>> data = MiningRestController.loadData(file);
            List<String> results = new ArrayList<>();

            // Filtrar y almacenar los resultados con el formato deseado
            for (Map<String, String> laptop : data) {
                String decision = makeDecision(laptop);
                if (!decision.contains("Descartado")) {  // Si la laptop no fue descartada
                    results.add(decision);
                }
            }

            // Convertir la lista de resultados en un único string
            String responseMessage = String.join("\n", results);

            // Retornar el resultado como una cadena
            return ResponseEntity.ok(responseMessage);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al procesar el archivo CSV");
        }


    }

    // Método para cargar los datos desde un archivo CSV
    public static List<Map<String, String>> loadData(MultipartFile file) {
        List<Map<String, String>> data = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            String[] headers = br.readLine().split(",");  // Leer los encabezados del CSV

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
    public static String makeDecision(Map<String, String> laptop) {
        // Filtrar por precio
        double price = Double.parseDouble(laptop.get("Price_euros"));
        if (price > 1000) {
            return "Descartado: Precio excede el presupuesto";
        }

        // Filtrar por RAM
        double ram = Double.parseDouble(laptop.get("Ram"));
        if (ram < 8) {
            return "Descartado: RAM insuficiente";
        }

        // Filtrar por almacenamiento primario (SSD preferido)
        String storageType = laptop.get("PrimaryStorageType");
        if (!storageType.equalsIgnoreCase("SSD")) {
            return "Descartado: Tipo de almacenamiento no aceptado";
        }

        // Filtrar por espacio de almacenamiento (mínimo 256GB)
        int storage = Integer.parseInt(laptop.get("PrimaryStorage"));
        if (storage < 256) {
            return "Descartado: Almacenamiento insuficiente";
        }

        // Filtrar por peso
        double weight = Double.parseDouble(laptop.get("Weight"));
        if (weight > 1.5) {
            return "Descartado: Peso excede el límite";
        }

        // Filtrar por tamaño de pantalla
        double inches = Double.parseDouble(laptop.get("Inches"));
        if (inches < 13 || inches > 15.6) {
            return "Descartado: Tamaño de pantalla fuera del rango aceptado";
        }

        // Si pasa todas las condiciones, se considera como una opción válida
        String brand = laptop.get("Company");
        return "Marca: " + brand + ", Precio: " + price + " euros, RAM: " + ram + "GB, " +
                "Almacenamiento: " + storage + "GB, Pantalla: " + inches + "\", Peso: " + weight + "kg";
    }
}
