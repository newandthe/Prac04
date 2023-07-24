package com.example.demo.repository;


import com.example.demo.config.MemberRowMapper;
import com.example.demo.model.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MemberDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MemberDao(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public boolean regiAf(Member member){
        try {
            String sql = "INSERT INTO member (username, password) VALUES (?, ?)";
            int n = jdbcTemplate.update(sql, member.getUsername(), member.getPassword());

            System.out.println("member: " + member.toString());

            return n > 0;
        } catch (DataAccessException ex){

            throw new RuntimeException("회원가입에 실패했습니다.", ex);
        }
    }

    public static Member findByUserId(String username) {
        // TODO Auto-generated method stub
        return null;
    }


}
