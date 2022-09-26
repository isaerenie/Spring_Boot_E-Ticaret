package com.works.entities;

import lombok.Data;

import javax.persistence.*;
import java.nio.file.Files;

@Entity
@Data
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer iid;
    // product id
    private Integer pid;
    @Lob
    private String file;
}