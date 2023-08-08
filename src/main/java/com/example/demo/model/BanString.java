package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BanString {

    boolean isBanned_forbidden; // 금칙어

    boolean isBanned_stopword;  // 불용어

    String BannedKeyWord_forbidden; // 금칙어

    String BannedKeyWord_stopwrod;  // 불용어
}
