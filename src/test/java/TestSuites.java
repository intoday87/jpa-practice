import entity.BoardTest;
import entity.MemberTest;
import entity.SequenceTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    BoardTest.class,
    MemberTest.class,
    SequenceTest.class
})
public class TestSuites {
}
