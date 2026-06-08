package com.example.nexusa.Reviewer.Dto;

import lombok.Data;
import java.util.UUID;

@Data
public class AddVolumeFromMarkDTO {
    private UUID volumeMarkId;
    private Integer position;
}