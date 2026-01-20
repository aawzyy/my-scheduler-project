package com.personal.scheduler_api.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_contacts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserContact {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;          // Kunci identifikasi (Unique)
    private String name;
    private String category;       // VIP, TEAM, FAMILY, BLACKLIST
    private int priorityScore;     // Bonus skor (Misal: VIP = +50, BLACKLIST = -999)

    public UserContact(String email, String name, String category, int priorityScore) {
        this.email = email;
        this.name = name;
        this.category = category;
        this.priorityScore = priorityScore;
    }
}