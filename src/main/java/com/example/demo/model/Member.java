package com.example.demo.model;


//import lombok.Data;

//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import lombok.ToString;


import lombok.*;

import javax.validation.constraints.NotBlank;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Member  {

    private int memberseq;

    @NotBlank(message = "유저 이름은 공백일 수 없습니다.")
    private String username;

    @NotBlank(message = "비밀번호는 공백일 수 없습니다.")
    private String password;

    private String role;

}