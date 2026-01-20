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

import com.personal.scheduler_api.model.UserContact;
import com.personal.scheduler_api.repository.UserContactRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {
    
    private final UserContactRepository repository;

    @GetMapping
    public List<UserContact> getContacts() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<UserContact> addContact(@RequestBody UserContact contact) {
        return ResponseEntity.ok(repository.save(contact));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteContact(@PathVariable java.util.UUID id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}