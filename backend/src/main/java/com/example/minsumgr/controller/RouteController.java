package com.example.minsumgr.controller;

import com.example.minsumgr.domain.Route;
import com.example.minsumgr.repository.RouteRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = "http://localhost:5173")
public class RouteController {

    private final RouteRepository repository;

    public RouteController(RouteRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Route> list() {
        return repository.findAll();
    }

    @PostMapping
    public Route create(@RequestBody Route route) {
        return repository.save(route);
    }

    @PutMapping("/{id}")
    public Route update(@PathVariable Long id, @RequestBody Route route) {
        route.setId(id);
        return repository.save(route);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
