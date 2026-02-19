package org.lf10.stimmungsumfrage.Models;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "countries")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "country_name", nullable = false)
    private String countryName;

    // Default constructor
    public Country() {
    }

    // Constructor with fields
    public Country(String countryName) {
        this.countryName = countryName;
    }
}
