package entity;

import org.junit.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import java.util.Objects;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MemberTest {
    public static final String ANY_ID = "address@email.com";
    public static final String ANY_ID_IN_DB = "address2@email.com";
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
            Member member = new Member();
            member.setId(ANY_ID_IN_DB);
            entityManager.persist(member);
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

            assertTrue("영속성 컨텍스트에서 1차 캐시는 동등성을 보장한다",member == findMember);
            assertTrue("같은 인스턴스이기 때문에 hashcode()도 같다", member.hashCode() == findMember.hashCode());

            entityManager.remove(member);

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

            System.out.println(found.getUsername());
            assertTrue(member != found);
            assertTrue(Objects.isNull(found.getUsername()));

            transaction.commit();
        } catch (Throwable e) {
            e.printStackTrace();
            transaction.rollback();
            fail("롤백됨");
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
            fail("롤백됨");
        }
    }

    @After
    public void tearDown() throws Exception {
        entityManager.close();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        emf.close();
    }
}