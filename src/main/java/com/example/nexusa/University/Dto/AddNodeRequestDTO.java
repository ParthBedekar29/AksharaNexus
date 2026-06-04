package com.example.nexusa.University.Dto;
import com.example.nexusa.Model.CMENode;
import com.example.nexusa.Model.Enums.CommitType;
import lombok.Data;
import java.util.UUID;

@Data
public class AddNodeRequestDTO {
    private UUID parentNodeId;
    private CMENode node;
    private String commitMsg;
    private CommitType commitType;
}