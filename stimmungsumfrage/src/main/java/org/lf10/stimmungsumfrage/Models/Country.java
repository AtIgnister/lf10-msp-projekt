package org.lf10.stimmungsumfrage.Models;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "countries")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(nullable = false)
    private String name;

    public Country() {
    }

    public Country(String countryName) {
        this.name = countryName;
    }
}
