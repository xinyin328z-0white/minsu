package com.example.minsumgr.controller;

import com.example.minsumgr.domain.HomestayType;
import com.example.minsumgr.repository.HomestayTypeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/homestay-types")
public class HomestayTypeController {

    private final HomestayTypeRepository repository;

    public HomestayTypeController(HomestayTypeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<HomestayType> list() {
        return repository.findAll();
    }

    @PostMapping
    public HomestayType create(@RequestBody HomestayType type) {
        return repository.save(type);
    }

    @PutMapping("/{id}")
    public HomestayType update(@PathVariable Long id, @RequestBody HomestayType type) {
        type.setId(id);
        return repository.save(type);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
