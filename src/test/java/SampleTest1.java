import com.mysql.cj.jdbc.MysqlDataSource;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.waiting.ClusterHealthCheck;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import com.palantir.docker.compose.connection.waiting.jdbc.JdbcContainerConfiguration;
import com.palantir.docker.compose.connection.waiting.jdbc.JdbcContainerHealthCheck;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class SampleTest1 {

    private DockerComposeRule dockerDb;
    private Connection conn;

    @Before
    public void before() throws IOException, InterruptedException, SQLException {
        dockerDb = DockerComposeRule.builder()
                .file("src/test/resources/docker-compose-test.yml")
                .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
                .addClusterWait(
                        new ClusterWait(
                                ClusterHealthCheck.transformingHealthCheck(
                                        cluster -> cluster.container("db").port(3306),
                                        target -> SuccessOrFailure.fromBoolean(target.isListeningNow(), target + " was not opened")),
                                Duration.standardSeconds(30)),
                        new ClusterWait(
                                ClusterHealthCheck.serviceHealthCheck(
                                        "db",
                                        JdbcContainerHealthCheck.of(JdbcContainerConfiguration.ofMySql("sampledb", "root", "sa"))),
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
    }

    @After
    public void tearDown() {
        dockerDb.after();
    }

    @Test
    public void test1() throws SQLException {
        // arrange
        conn.prepareStatement("INSERT INTO account (name) VALUES ('Dikstra')").executeUpdate();

        // act
        final ResultSet resultSet = conn.prepareStatement("SELECT * FROM account").executeQuery();
        resultSet.next();
        final String name = resultSet.getString("name");

        // assert
        assertEquals("Dikstra", name);
    }

    @Test
    public void test2() throws SQLException {
        // arrange
        conn.prepareStatement("INSERT INTO account (name) VALUES ('Dikstra')").executeUpdate();

        // act
        final ResultSet resultSet = conn.prepareStatement("SELECT * FROM account").executeQuery();
        resultSet.next();
        final String name = resultSet.getString("name");

        // assert
        assertEquals("Dikstra", name);
    }

}
