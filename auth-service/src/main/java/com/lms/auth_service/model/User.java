//Model = “What your system IS”

package com.lms.auth_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Document("users")
public class User {
    @Id
    private String id;

    @Indexed
    private String email;

    private String passwordHash;

    private Set<String> roles;

    private Instant createdAt = Instant.now();

    public User(){

    }

    public User(String email, String passwordHash, Set<String> roles){
        this.email=email;
        this.passwordHash=passwordHash;
        this.roles=roles;
        this.createdAt=Instant.now();
    }

    //getters and setters

    //id
    public String getId(){
        return id;
    }
    public void setId(){
        this.id=id;
    }

    //email
    public String getEmail(){
        return email;
    }
    public void setEmail(){
        this.email=email;
    }

    //password
    public String getPasswordHash(){
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    //roles
    public Set<String> getRoles() {
        return roles;
    }
    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    //createdAt
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
