package querydslstudy.querydslstudy;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import querydslstudy.querydslstudy.dto.MemberDto;
import querydslstudy.querydslstudy.dto.QMemberDto;
import querydslstudy.querydslstudy.dto.UserDto;
import querydslstudy.querydslstudy.entity.Member;
import querydslstudy.querydslstudy.entity.QMember;
import querydslstudy.querydslstudy.entity.QTeam;
import querydslstudy.querydslstudy.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.core.types.Projections.*;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.iterable;
import static querydslstudy.querydslstudy.entity.QMember.member;
import static querydslstudy.querydslstudy.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory jpaQueryFactory;

    @BeforeEach
    public void before() {
        jpaQueryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);


        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();


        List<Member> result = em.createQuery("select m from Member m", Member.class).getResultList();

        for (Member member : result) {
            System.out.println("member = " + member);
            System.out.println("-> member.team " + member.getTeam());
        }
    }

    @Test
    public void startJPQL() {
        //member1을 찾아라

        Member findByJpql = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findByJpql.getUsername()).isEqualTo("member1");


    }

    @Test
    public void startQueryDsl() {

        Member findMember = jpaQueryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    public void search() {
        Member findMember = jpaQueryFactory.selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    public void searchAndParam() {
        Member findMember = jpaQueryFactory.selectFrom(member)
                .where(
                        member.username.eq("member1"), (member.age.eq(10))

                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

/*
    @Test
    public void resultFetch() {
        List<Member> fet = jpaQueryFactory.selectFrom(member)
                .fetch();


        Member fetchOne = jpaQueryFactory.selectFrom(QMember.member)
                .fetchOne();

        Member limitFetch = jpaQueryFactory.selectFrom(member)
                .fetchFirst();

        QueryResults<Member> results = jpaQueryFactory.selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> contents = results.getResults();
        results.getOffset();


        long total = jpaQueryFactory.selectFrom(member)
                .fetchCount();


    }
*/

    /*
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원이름이 없으면 마지막에 출력(nulls last);
     * */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNULL = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNULL.getUsername()).isNull();

    }

    @Test
    public void gaging1() {
        List<Member> result = jpaQueryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();


        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void gaging2() {
        QueryResults<Member> result = jpaQueryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();


        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);

        assertThat(result.getResults().size()).isEqualTo(2);

    }

    @Test
    public void aggregation() {
        List<Tuple> fetch = jpaQueryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = fetch.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /*
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     *
     * */
    @Test
    public void group() {

        List<Tuple> result = jpaQueryFactory.select(
                team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);


        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    /*
     * 팀 A에 소속된 모든회원
     *
     * */
    @Test
    public void join() {
        List<Member> result = jpaQueryFactory.selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username")
                .containsExactly("member1", "member2");
    }

    /*
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = jpaQueryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        for (Member members : result) {
            System.out.println("members = " + members.getUsername());
        }


    }

    /*
     * 예) 회원과 팀을 조인하면서 , 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     *   JPAL : select m , t from Member m left join m.team t on t.name = 'teamA'
     *  */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = jpaQueryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /*
     * 연관관계가 없는 엔티티 외부조인
     * 회원의 이름이 팀 이름과 같은 대상 외부조인
     * */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        List<Tuple> result = jpaQueryFactory.select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }


    }
    /*
     * 페치 조인은 SQL에서 제공하는 기능은 아니다. SQL조인을 활용해서 연관된 엔티티를
     * SQL 한번에 조회하는 기능이다. 주로 성능 최적화에 사용하는 기법이다.
     * */

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();

        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(isLoaded).as("페치 조인 미적용").isFalse();


    }

    @Test
    public void fetchJoinUser() throws Exception {
        em.flush();
        em.clear();

        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(isLoaded).as("페치 조인 미적용").isTrue();


    }

    /*
     * 서브쿼리
     * com.querydsl.jpa.JPAExpressions
     * */
    /*
     * 나이가 가장 많은 회원을 조회
     * */
    @Test
    public void subQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)

                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /*
     * 나이가 평균 이상인 회원
     * */
    @Test
    public void subQueryGoe() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)

                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /*
     * 나이가 평균 이상인 회원
     * */
    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = jpaQueryFactory
                .select(member.username,
                        select(memberSub.age.avg()).from(memberSub))
                .from(member)
                .fetch();


        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /*
     * from절의 서브쿼리 한계
     * JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지않는다.
     * 당연히 Querydsl도 지원하지 않는다. 하이버네이트 구현체를 사용하면 select절의 서브
     * 쿼리는 지원한다. Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.
     *
     * from절의 서브쿼리 해결방안
     * 서브쿼리를 join으로 변경한다.(가능한 상황도 있고, 불가능한 상황도 있다.)
     * 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
     * nativeSQL을 사용한다.
     * */

    @Test
    public void basicCase() {
        List<String> fetch = jpaQueryFactory.select(member.age.when(10).then("열살")
                .when(20).then("스무살")
                .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = jpaQueryFactory
                .select(new CaseBuilder().when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30"
                        ).otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() {
        List<Tuple> result = jpaQueryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() {

        //username_age
        List<String> result = jpaQueryFactory.select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /*
     * 프로젝션 select 대상 지정
     * */

    @Test
    public void simpleProjection() {
        List<String> result = jpaQueryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = jpaQueryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("select new querydslstudy.querydslstudy.dto.MemberDto(m.username , m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
     * 기본 생성자 필요
     * */
    @Test
    public void findDtoByQueryDslToSetter() {
        List<MemberDto> result = jpaQueryFactory
                .select(bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByQueryDslToFields() {
        List<MemberDto> result = jpaQueryFactory
                .select(fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
     * todo java 리플렉션
     * */
    @Test
    public void findDtoByQueryDslToConstructor() {
        List<MemberDto> result = jpaQueryFactory
                .select(constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
     * 프로퍼티나 필드 접근 생성방식에서 이름이 다를때 해결방안
     * ExpressionUtils
     * */
    @Test
    public void findUserDtoByQueryDslToFields() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = jpaQueryFactory
                .select(fields(UserDto.class, member.username.as("name"),
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub), "age"
                        )
                ))
                .from(member)
                .fetch();


        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findUserDtoByQueryDslToConstructor() {
        List<UserDto> result = jpaQueryFactory
                .select(constructor(UserDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("memberDto = " + userDto);
        }
    }

    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = jpaQueryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    /*
     * 동적 쿼리를 해결하는 두가지 방식
     * BooleanBuilder
     * */
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return jpaQueryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }


    /*
     * 동적쿼리 where - 다중파라미터
     * */
    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {

        return jpaQueryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                //.where(allEq(usernameCond , ageCond)
                .fetch();
    }


    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;

    }

    /*
     * 광고 상태 isValid, 날짜가 IN  : isServiceable
     * */
    /*
     * where 조건에 null값은 무시된다
     * 메서드를 다른 쿼리에서도 재활용 할 수 있다.
     * 쿼리 자체의 가독성이 높아진다.
     *
     * 조합 할때 null 체크를 해야한다.
     * */
    private BooleanExpression allEq(String username, Integer ageCond) {
        return usernameEq(username).and(ageEq(ageCond));

    }

    //@Commit
    /*
     * 영속성 컨텍스트에 다 올라가있다.
     * update 해주고
     * flush
     * clear 해야한다.
     * */
    @Test
    public void bulkUpdate() {

        //member1 = 10
        //member2 = 20
        long count = jpaQueryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(27))
                .execute();

    }

    @Test
    public void bulkAdd() {
        //곱은 multiply
        jpaQueryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }


    @Test
    public void blukDelete() {
        jpaQueryFactory
                .delete(member)
                .where(member.age.eq(10))
                .execute();
    }

    /*
     * SQL function 호출하기
     * JPA와 같이 Dialect등록된 내용만 호출가능하다.
     * */

    @Test
    public void sqlFunction() {
        List<String> result = jpaQueryFactory
                .select(
                        Expressions.stringTemplate("function('replace',{0},{1},{2})", member.username, "member", "m"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


    @Test
    public void sqlFunction2() {
        /*
        *       .where(member.username.eq(Expressions.stringTemplate("function('lower',{0})"
                        , member.username))
        * */
        List<String> fetch = jpaQueryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

}
