package com.example.nexusa.Repository;

import com.example.nexusa.Model.AdminCodes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminCodeRepository extends JpaRepository<AdminCodes, UUID> {
   Optional<AdminCodes> findAdminCodesByEmail(String email);
}
