package org.lf10.stimmungsumfrage.Repositories;

import org.lf10.stimmungsumfrage.Models.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Long> {
    Optional<Country> findByName(String name);
}