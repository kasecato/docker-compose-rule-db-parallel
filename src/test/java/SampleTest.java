import com.mysql.cj.jdbc.MysqlDataSource;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.waiting.ClusterHealthCheck;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;


public class SampleTest {

    private DockerComposeRule dockerDb;
    private Connection conn;

    @Before
    public void before() throws IOException, InterruptedException, SQLException {
        dockerDb = DockerComposeRule.builder()
                .file("src/test/resources/docker-compose-test.yml")
                .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
                .addClusterWait(new ClusterWait(
                        ClusterHealthCheck.transformingHealthCheck(
                                cluster -> cluster.container("db").port(3306),
                                target -> SuccessOrFailure.fromBoolean(target.isListeningNow(), target + " was not opened")),
                        Duration.standardSeconds(30)))
                .build();

        dockerDb.before();

        final MysqlDataSource dataSource = new MysqlDataSource() {{
            final String url = dockerDb.containers().container("db").port(3306).inFormat("jdbc:mysql://$HOST:$EXTERNAL_PORT/sampledb");
            setURL(url);
            setUser("root");
            setPassword("sa");
        }};

        conn = dataSource.getConnection();
        // I don't want to use like this codes
//        while (true) {
//            try {
//                conn = dataSource.getConnection();
//                break;
//            } catch (Exception e) {
//                Thread.sleep(1000);
//            }
//        }
    }

    @After
    public void tearDown() {
        dockerDb.after();
    }

    @Test
    public void test1() throws SQLException {
        conn.prepareStatement("SELECT 1").executeQuery();
    }
}
