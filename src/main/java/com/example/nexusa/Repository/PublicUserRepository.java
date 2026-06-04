package com.example.nexusa.Repository;

import com.example.nexusa.Model.PublicUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublicUserRepository extends JpaRepository<PublicUser, UUID> {
    Optional<PublicUser> findByEmail(String email);
}
