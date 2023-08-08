package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Iterator;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommManual{

    private List<String> recomm;

    public void removeItemContaining(String substring) {
        Iterator<String> iterator = recomm.iterator();
        while (iterator.hasNext()) {
            String item = iterator.next();
            if (item.contains(substring)) {
                iterator.remove();
            }
        }
    }
}
