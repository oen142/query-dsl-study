package querydslstudy.querydslstudy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import querydslstudy.querydslstudy.dto.MemberSearchCondition;
import querydslstudy.querydslstudy.dto.MemberTeamDto;
import querydslstudy.querydslstudy.repository.MemberJpaRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition){
        return memberJpaRepository.searchByWhere(condition);
    }
}
