import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.execution.Command;
import com.palantir.docker.compose.execution.DefaultDockerCompose;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import com.palantir.docker.compose.execution.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DockerComposeTest extends DefaultDockerCompose {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeTest.class);

    private final Command command;

    public DockerComposeTest(final DockerComposeExecutable rawExecutable, final DockerMachine dockerMachine) {
        super(rawExecutable, dockerMachine);
        this.command = new Command(rawExecutable, log::trace);
    }

    @Override
    public void down() throws IOException, InterruptedException {
        command.execute(swallowingDownCommandDoesNotExist(), "down", "--rmi", "all");
    }

    private static boolean downCommandWasPresent(final String output) {
        return !output.contains("No such command");
    }

    private static ErrorHandler swallowingDownCommandDoesNotExist() {
        return (exitCode, output, commandName, commands) -> {
            if (downCommandWasPresent(output)) {
                Command.throwingOnError().handle(exitCode, output, commandName, commands);
            }

            log.warn("It looks like `docker-compose down` didn't work.");
            log.warn("This probably means your version of docker-compose doesn't support the `down` command");
            log.warn("Updating to version 1.6+ of docker-compose is likely to fix this issue.");
        };
    }

}
