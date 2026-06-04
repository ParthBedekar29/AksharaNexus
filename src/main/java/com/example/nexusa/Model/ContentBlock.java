package com.example.nexusa.Model;

import com.example.nexusa.Model.Enums.BlockType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ContentBlock {
    private BlockType blockType;
    private Map<String, Object> data;
    private List<Citation> citations;
}