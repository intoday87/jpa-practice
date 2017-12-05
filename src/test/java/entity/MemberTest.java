package entity;

import org.junit.*;

import javax.persistence.*;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
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
            member.setUsername(ANY_USER_NAME);
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
            assertThat(member.getUsername(), not(equalTo(found.getUsername())));

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
            assertThat(member.getUsername(), not(equalTo(found.getUsername())));

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
            member.setUsername(ANY_USER_NAME);
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
            assertThat(member.getUsername(), equalTo(found.getUsername()));
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

        Member member = new Member();

        try {
            tx1.begin();
            member.setUsername(ANY_USER_NAME);
            member.setId(ANY_ID);
            member.setAge(ANY_AGE);

            em1.persist(member);
            tx1.commit();
        } catch (Exception e) {
            tx1.rollback();
        } finally {
            em1.close();
        }

        member.setUsername("otherUserName");

        try {
            tx2.begin();
            Member found = em2.find(Member.class, member.getId());
            assertThat("엔티티가 준영속 상태가 된 후 username을 변경하였으나 영속성 컨텍스트가 관리하지 않기 때문에 update가 되지 않았을 것이다", member.getUsername(), not(equalTo(found.getUsername())));

            Member merged = em2.merge(member);

            assertThat("엔티티가 준영속 상태의 엔티티를 merge를 통해 영속성 콘텍스트가 관리하는 1차 캐시의 엔티티로 병합이 되면서 변경된 내용이 update가 된다",
                member.getUsername(), equalTo(found.getUsername()));
            assertTrue("병합후 반환된 엔티티는 인자로 넘긴 준영속 엔티티와 다른 인스턴스다", member != merged);
            assertTrue("병합후 반환된 엔티티와 병합후 find로 찾은 엔티티는 같은 1차 캐시에서 반환되어 같은 인스턴스일 것이다", found == merged);

            member.setUsername("anotherUserName");

            assertThat("member 엔티티는 merge의 인자로 넘겨졌다고 해서 준영속에서 영속상태가 되는 것은 아니다. 단지 영속성 컨텍스트에서 관리하는 id가 같은 엔티티에 값을 뒤집어 씌울뿐", member.getUsername(), not(equalTo(merged.getUsername())));

            tx2.commit();
        } catch (Exception e) {
            tx2.rollback();
        } finally {
            em2.close();
        }
    }

    @Test
    public void changeEntityIdAndMerge() throws Exception {
        EntityManager em1 = emf.createEntityManager();
        EntityTransaction tx1 = em1.getTransaction();
        EntityManager em2 = emf.createEntityManager();
        EntityTransaction tx2 = em2.getTransaction();

        Member member = new Member();

        try {
            tx1.begin();
            member.setId(ANY_ID);
            member.setUsername(ANY_USER_NAME);
            member.setAge(ANY_AGE);
            em1.persist(member);
            tx1.commit();
        } catch (Exception e) {
            tx1.rollback();
        } finally {
            em1.close();
        }

        member.setId("other@email.com");

        try {
            tx2.begin();
            assertThat("준영속 상태의 엔티티의 id를 바꾸었다고 해서 DB에 업데이트 되지 않았다", em2.find(Member.class, member.getId()), nullValue());

            Member merged = em2.merge(member);

            assertThat("member가 병합되면서 set한 이아디의 엔티티가 존재한다", merged == em2.find(Member.class, member.getId()));
            assertThat("기존에 영속화 했던 member는 존재한다", em2.find(Member.class, ANY_ID).getId(), equalTo(ANY_ID));
            tx2.commit();
        } catch (Exception e) {
            tx2.rollback();
        } finally {
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
