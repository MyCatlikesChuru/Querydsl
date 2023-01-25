package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
//@Commit
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);

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
    }

    // JPQL로 찾아보기
//    @Test
//    public void startJPQL(){
//        // member1을 찾아라
//        String qlString =
//                "select m from Member m "+
//                "where m.username = :username";
//
//        Member findByJPQL = em.createQuery(qlString, Member.class)
//                .setParameter("username", "member1")
//                .getSingleResult();
//
//        assertThat(findByJPQL.getUsername()).isEqualTo("member1");
//    }

    // 파라미터 바인딩을 자동으로 해결해준다 !!
    // 컴파일시 에러를 잡아준다 !!
    @Test
    public void startQuerydsl1(){
        System.out.println("=======startQuerydsl1======");
        QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) // 파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        System.out.println("=======startQuerydsl1======");
    }

    // 코드 줄이기
    // 권장하는 코드
    @Test
    public void startQuerydsl2(){

        Member findMember = queryFactory
                .select(member) // <- Q 타입은 static import할 것
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        // List
        System.out.println("=============List=============");
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();
        System.out.println("=============List=============");

        // 단 건 -> 결과가 둘이상이면 NonUniqueResultException 발생
//        System.out.println("=============단 건=============");
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//        System.out.println("=============단 건=============");

        // 처음 한 건 조회
        System.out.println("=============처음 한 건 조회=============");
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();// = limit(1).fetchOne(); 같은코드다
        System.out.println("=============처음 한 건 조회=============");

        // 페이징에서 사용
        System.out.println("=============페이징에서 사용=============");
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        // count 쿼리로 변경
        results.getTotal();
        List<Member> content = results.getResults();
        System.out.println("=============페이징에서 사용=============");

        long count = queryFactory
                .selectFrom(member)
                .fetchCount();

    }


    /*
    * 회원 정렬 순서
    *
    * 1. 회원 나이 내림차순(desc)
    * 2. 회원 이름 오름차순(asc)
    * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
    * */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    /*
    * 페이징
    * */
    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2(){ // 페이징쿼리가 단순할때만 사용
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        // Tuple 쿼리 dsl이 제공해주는 타입, 여러개의 데이터를 가져올때
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /*
    * 팀의 이름과 각 팀의 평균 연령을 구해라.
    * */
    @Test
    public void groupBy() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10+20) / 2

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30+40) / 2
    }

    /*
    * 팀 A에 소속된 모든 회원 조인
    * */
    @Test
    public void join(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1","member2");
    }

    /*
    * 세타 조인 (연관관계가 없는 엔티티도 조인이 가능하다 !!)
    * 회원의 이름이 팀 이름과 같은 회원 조회
    * 제약 : 외부 조인이 불가능 (LeftOut, RightOut Join)
    * */
    @Test
    public void theta_join(){
        em.persist(new Member("teamA")); // 멤버이름을 teamA
        em.persist(new Member("teamB")); // 멤버이름을 teamB
        em.persist(new Member("teamC")); // 멤버이름을 teamC

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }

    /*
    * 회원과 팀을 Join, Team 이름이 teamA인 팀만 Join 회원은 모두 조회
    * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
    * */
    @Test
    public void join_on_filtering(){
        List<Tuple> result1 = queryFactory
                .select(member, team) // Tuple로 나온이유는 select 타입이 여러개이기때문
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA")) // 내부 조인일 경우 on절이 의미있다.
                .fetch();

        // member3,4가 Null로 생김
        for (Tuple tuple : result1) {
            System.out.println("result1 tuple = " + tuple);
        }

        List<Tuple> result2 = queryFactory
                .select(member, team) // Tuple로 나온이유는 select 타입이 여러개이기때문
                .from(member)
                .join(member.team, team).on(team.name.eq("teamA")) // = 결과가 where(team.name.eq("teamA")) 와 같다.
                .fetch();

        // member3,4가 없어짐
        for (Tuple tuple : result2) {
            System.out.println("result2 tuple = " + tuple);
        }
    }

    /*
    * 연관관계가 없는 엔티티를 외부 조인
    * 회원의 이름과 팀이림이 같은 대상을 외부 조인
    * */
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA")); // 멤버이름을 teamA
        em.persist(new Member("teamB")); // 멤버이름을 teamB
        em.persist(new Member("teamC")); // 멤버이름을 teamC

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name)) // member.team이 아닌 막조인을 함
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple); // 멤버이름과 팀이름이 같은 조건 7,8번은 값이 존재
        }
    }

    /*
    * fetchJoin 미적용
    * */
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo(){
        // 영속성 컨텍스트 초기화
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member) // team은 조회안됨 Lazy
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    /*
    * fetchJoin 적용
    * */
    @Test
    public void fetchJoinUse(){
        // 영속성 컨텍스트 초기화
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() // team도 조회됨
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 적용").isTrue();
    }
}
