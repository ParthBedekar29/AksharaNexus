package com.example.nexusa.Repository;

import com.example.nexusa.Model.Enums.Role;
import com.example.nexusa.Model.University;
import com.example.nexusa.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findUserByEmail(String email);

    Optional<User> findUserByUserId(UUID userId);

    List<User> findByUniID(University uniID);

    List<User> findUsersByRole(Role role);

    boolean existsByEmail(String email);


   Optional<User> findByEmail(String email);
}
