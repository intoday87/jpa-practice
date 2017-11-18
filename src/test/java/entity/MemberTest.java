package entity;

import org.junit.*;

import javax.persistence.*;
import java.util.Objects;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MemberTest {
    private static final String ANY_ID = "address@email.com";
    private static final String ANY_ID_IN_DB = "address2@email.com";
    public static final String ANY_USER_NAME = "USER_NAME";
    public static final int ANY_AGE = 1;
    static EntityManagerFactory emf;
    EntityManager entityManager;
    EntityTransaction transaction;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        emf = Persistence.createEntityManagerFactory("jpa-practice");
    }

    @Before
    public void setUp() throws Exception {
        entityManager = emf.createEntityManager();
        transaction = entityManager.getTransaction();

        if (Objects.isNull(entityManager.find(Member.class, ANY_ID_IN_DB))) {
            transaction.begin();
            Member member = new Member();
            member.setId(ANY_ID_IN_DB);
            entityManager.persist(member);
            transaction.commit();
        }
    }

    @Test
    public void firstCacheGuaranteeIdentity() throws Exception {
        try {
            transaction.begin();

            Member member = new Member();
            member.setId(ANY_ID);
            member.setUsername("nick");
            member.setAge(31);

            entityManager.persist(member);

            Member findMember = entityManager.find(Member.class, ANY_ID);

            assertTrue("영속성 컨텍스트에서 1차 캐시는 동등성을 보장한다", member == findMember);
            assertTrue("같은 인스턴스이기 때문에 hashcode()도 같다", member.hashCode() == findMember.hashCode());

            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            fail("예외가 발생함");
        }
    }

    @Test
    public void detachedMemberAndFoundMemberAreEqual() throws Exception {
        try {
            transaction.begin();

            Member member = entityManager.find(Member.class, ANY_ID_IN_DB);
            entityManager.detach(member);

            final String username = "userName";
            member.setUsername(username);

            Member found = entityManager.find(Member.class, ANY_ID_IN_DB);

            assertTrue(member != found);
            assertTrue(Objects.isNull(found.getUsername()));

            transaction.commit();
        } catch (Throwable e) {
            e.printStackTrace();
            transaction.rollback();
            fail("rollback");
        }
    }

    @Test
    public void jpqlResultIsNotMachedWithDetachedEntity() throws Exception {
        try {
            transaction.begin();

            Member member = entityManager.find(Member.class, ANY_ID_IN_DB);
            entityManager.detach(member);

            final String username = "userName";
            member.setUsername(username);

            Member found = entityManager.createQuery("SELECT m FROM Member m WHERE m.id LIKE '" + ANY_ID_IN_DB + "'", Member.class).getResultList().get(0);

            assertTrue(member != found);
            assertTrue(Objects.isNull(found.getUsername()));

            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            fail("rollback");
        }
    }

    @Test
    public void rollbackExceptionFollowedByDetachedEntityToCommit() throws Exception {
        try {
            transaction.begin();
            Member member = new Member();
            member.setId(ANY_ID);
            entityManager.persist(member);
            entityManager.detach(member);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            if (e instanceof RollbackException) {
                return;
            }

            fail("RollbackException did not occurred");
        }
    }

    @Test
    public void clearAllOfSqlWhenClearPersistenceContext() throws Exception {
        try {
            transaction.begin();
            Member member = entityManager.find(Member.class, ANY_ID_IN_DB);
            entityManager.clear();
            member.setUsername(ANY_USER_NAME);
            transaction.commit();

            entityManager = emf.createEntityManager();
            transaction.begin();
            Member found = entityManager.find(Member.class, ANY_ID_IN_DB);
            assertTrue(Objects.isNull(found.getUsername()));
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
        }
    }

    @Test
    public void merge() throws Exception {
        EntityManager em1 = emf.createEntityManager();
        EntityTransaction tx1 = em1.getTransaction();
        EntityManager em2 = emf.createEntityManager();
        EntityTransaction tx2 = em2.getTransaction();

        try {
            tx1.begin();
            Member member = new Member();
            member.setUsername(ANY_USER_NAME);
            member.setId(ANY_ID);
            member.setAge(ANY_AGE);

            em1.persist(member);
            tx1.commit();
            em1.close();

            tx2.begin();

            Member merged = em2.merge(member);

            assertTrue("병합후 반환된 엔티티는 인자로 넘긴 준영속 엔티티와 다른 인스턴스다", member != merged);

            Member found = em2.find(Member.class, member.getId());

            assertTrue("병합후 반환된 엔티티와 병합후 find로 찾은 엔티티는 같은 1차 캐시에서 반환되어 같은 인스턴스일 것이다", found == merged);

            tx2.commit();
            em2.close();

        } catch (Exception e) {
           tx1.rollback();
           tx2.rollback();
           em1.close();
           em2.close();
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            transaction.begin();
            entityManager.createQuery("DELETE FROM Member m WHERE m.id NOT LIKE '" + ANY_ID_IN_DB + "'").executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            System.out.println("tear down rollback");
        }
        entityManager.close();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        EntityManager entityManager = emf.createEntityManager();
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            entityManager.createQuery("DELETE FROM Member").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            System.out.println("after class tear down rollback");
        }
        entityManager.close();
        emf.close();
    }
}