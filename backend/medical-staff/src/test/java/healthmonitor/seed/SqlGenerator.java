package healthmonitor.seed;

import java.io.FileWriter;
import java.io.IOException;

public class SqlGenerator {
    public static void main(String[] args) throws IOException {
        try (FileWriter writer = new FileWriter("init-medical-staff.sql")) {
            writer.write("""
                    CREATE TABLE IF NOT EXISTS medical_staff (
                        id VARCHAR(255) PRIMARY KEY,
                        first_name VARCHAR(255) NOT NULL,
                        last_name VARCHAR(255) NOT NULL,
                        phone_number VARCHAR(255) UNIQUE,
                        license_number VARCHAR(255) UNIQUE
                    );
                    
                    """);

            for (int i = 0; i < 10000; i++) {
                writer.write(String.format(
                        "INSERT INTO medical_staff (id, first_name, last_name, phone_number, license_number) " +
                                "VALUES ('ID-%d', 'First%d', 'Last%d', '+481%09d', '%d');\n",
                        i, i, i, i, 5000000 + i
                ));
            }
        }
        System.out.println("Plik perf-init.sql został wygenerowany!");
    }
}