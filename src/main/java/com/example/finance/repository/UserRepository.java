package com.example.finance.repository;

import com.example.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

// JPA - Java Persistence API 
// It is used to manage the data between the Java application and the database
public interface UserRepository extends JpaRepository<User, Long> {
    // User - The entity class for which the repository is created
    // Long - The data type of the primary key of the entity class
}

//eg. UserRepository userRepository = 
//      Spring Data JPA will create an implementation of this interface at runtime
//      It will use reflection to create a proxy object that implements the JpaRepository interface