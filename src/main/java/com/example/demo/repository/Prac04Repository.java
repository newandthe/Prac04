package com.example.demo.repository;

import com.example.demo.config.MemberRowMapper;
import com.example.demo.model.Member;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class Prac04Repository {


    private final JdbcTemplate jdbcTemplate;

    public Prac04Repository(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Member getUser(String username) {
        // 로그인 성공 / 실패 여부를 확인하기 위한 쿼리 작성
        String sql = "SELECT * FROM member WHERE username = ?";

        Member member = jdbcTemplate.queryForObject(sql, new Object[]{username}, new MemberRowMapper());

        return member;
    }

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

    public Member findByUsername(String username) {
        try {
            String sql = "SELECT * FROM member WHERE username = ?";
            return jdbcTemplate.queryForObject(sql, new Object[]{username}, (rs, rowNum) -> {
                Member member = new Member();
                member.setMemberseq(rs.getInt("memberseq"));
                member.setUsername(rs.getString("username"));
                member.setPassword(rs.getString("password"));
                member.setRole(rs.getString("role"));

                System.out.println("member!!!");
                System.out.println(member);
                return member;
            });
        } catch (DataAccessException ex) {
            throw new RuntimeException("유저 아이디 탐색 실패.", ex);
        }
    }
}
