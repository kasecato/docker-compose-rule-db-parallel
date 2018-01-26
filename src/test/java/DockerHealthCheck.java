import com.mysql.cj.jdbc.MysqlDataSource;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import com.palantir.docker.compose.execution.ImmutableDockerComposeExecutable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class DockerHealthCheck {

    @ClassRule
    public static DockerComposeRule dockerDb = DockerComposeRule.builder()
            .file("src/test/resources/docker-compose-healthcheck-test.yml")
            .dockerCompose(new DockerComposeTest(
                    ImmutableDockerComposeExecutable.builder()
                            .dockerComposeFiles(new DockerComposeFiles(Collections.singletonList(
                                    new File("src/test/resources/docker-compose-healthcheck-test.yml"))))
                            .dockerConfiguration(DockerMachine.localMachine().build())
                            .projectName(ProjectName.random())
                            .build(),
                    DockerMachine.localMachine().build()
            ))
            .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
            .build();

    private static Connection conn;

    @BeforeClass
    public static void before() throws SQLException {

        final MysqlDataSource dataSource = new MysqlDataSource() {{
            final String url = dockerDb.containers().container("db").port(3306).inFormat("jdbc:mysql://$HOST:$EXTERNAL_PORT/sampledb");
            setURL(url);
            setUser("root");
            setPassword("sa");
        }};

        conn = dataSource.getConnection();
    }

    @AfterClass
    public static void after() {
        dockerDb.after();
    }

    @Test
    public void test1() throws SQLException {
        // arrange
        conn.prepareStatement("INSERT INTO account (name) VALUES ('Dikstra')").executeUpdate();

        // act
        final ResultSet resultSet = conn.prepareStatement("SELECT * FROM account").executeQuery();
        resultSet.next();
        final Integer id = resultSet.getInt("id");
        final String name = resultSet.getString("name");

        // assert
        assertEquals(1, id.intValue());
        assertEquals("Dikstra", name);
    }

    @Test
    public void test2() throws SQLException {
        // arrange
        conn.prepareStatement("INSERT INTO account (name) VALUES ('Dikstra')").executeUpdate();

        // act
        final ResultSet resultSet = conn.prepareStatement("SELECT * FROM account").executeQuery();
        resultSet.next();
        final Integer id = resultSet.getInt("id");
        final String name = resultSet.getString("name");

        // assert
        assertEquals(1, id.intValue());
        assertEquals("Dikstra", name);
    }

}
