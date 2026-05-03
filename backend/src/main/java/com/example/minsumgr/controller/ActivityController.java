package com.example.minsumgr.controller;

import com.example.minsumgr.domain.Activity;
import com.example.minsumgr.repository.ActivityRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "http://localhost:5173")
public class ActivityController {

    private final ActivityRepository repository;

    public ActivityController(ActivityRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Activity> list() {
        return repository.findAll();
    }

    @PostMapping
    public Activity create(@RequestBody Activity activity) {
        return repository.save(activity);
    }

    @PutMapping("/{id}")
    public Activity update(@PathVariable Long id, @RequestBody Activity activity) {
        activity.setId(id);
        return repository.save(activity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
