package com.jokati.invoice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.jokati.invoice.model.Country;
import com.jokati.invoice.repository.CountryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    public Country saveCountry(Country country) {
        return countryRepository.save(country);
    }

    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    public Country updateCountry(String id, Country updatedCountry) {
        Country existing = countryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Country not found"));

        existing.setCode(updatedCountry.getCode());
        existing.setName(updatedCountry.getName());

        return countryRepository.save(existing);
    }

    public void deleteCountry(String id) {
        if (!countryRepository.existsById(id)) {
            throw new RuntimeException("Country not found");
        }
        countryRepository.deleteById(id);
    }
}
