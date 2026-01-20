package com.personal.scheduler_api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.scheduler_api.model.PersonalBlock;
import com.personal.scheduler_api.repository.PersonalBlockRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/personal-blocks")
@RequiredArgsConstructor
public class PersonalBlockController {

    private final PersonalBlockRepository repository;

    @GetMapping
    public List<PersonalBlock> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public PersonalBlock create(@RequestBody PersonalBlock block) {
        return repository.save(block);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}