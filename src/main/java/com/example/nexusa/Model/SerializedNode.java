package com.example.nexusa.Model;

import lombok.Data;

import java.util.List;

@Data
public class SerializedNode {
    private CMENode data;
    private List<SerializedNode> children;
}
