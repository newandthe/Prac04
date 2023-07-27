package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class searchParsedEntity {

    private String must[];

    private String must_not[];

    private String match_pharse[];
}
