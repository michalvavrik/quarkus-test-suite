import static java.util.concurrent.CompletableFuture.runAsync;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.ts.qe.command.AgeCommand;
import io.quarkus.ts.qe.command.CommonOptions;
import io.quarkus.ts.qe.command.EntryCommand;
import io.quarkus.ts.qe.command.HelloCommand;
import io.quarkus.ts.qe.command.OtherCommand;
import io.quarkus.ts.qe.command.OtherEntryCommand;
import io.quarkus.ts.qe.configuration.Configuration;
import io.quarkus.ts.qe.services.AgeService;
import io.quarkus.ts.qe.services.HelloService;

@QuarkusScenario
public class PicocliProfileTestIT {

    @QuarkusApplication(classes = { AgeCommand.class, CommonOptions.class, EntryCommand.class, HelloCommand.class,
            OtherCommand.class, OtherEntryCommand.class,
            Configuration.class, AgeService.class, HelloService.class }, properties = "test.properties")
    static final RestService app = new RestService().setAutoStart(false);

    @Test
    public void verifyErrorForApplicationScopedBeanInPicocliCommand() {
        try {
            app.withProperty("quarkus.args", "age --age 30");
            runAsync(app::start);
            app.logs().assertContains("CDI: programmatic lookup problem detected");
        } finally {
            app.stop();
        }
    }

    @Test
    public void verifyGreetingCommandOutputsExpectedMessage() {
        try {
            app.withProperty("quarkus.args", "greeting --name QE");
            runAsync(app::start);
            app.logs().assertContains("Hello QE!");
        } finally {
            app.stop();
        }
    }

    @Test
    void verifyErrorForBlankArgumentsInGreetingCommand() {
        try {
            app.withProperty("quarkus.args", " --name QE");
            runAsync(app::start);
            app.logs().assertContains("Unmatched arguments from index 0: '', '--name', 'QE'");
        } finally {
            app.stop();
        }
    }

    @Test
    void verifyErrorForInvalidArgumentsInGreetingCommand() {
        try {
            app.withProperty("quarkus.args", "greeting -x QE");
            runAsync(app::start);
            app.logs().assertContains("Unknown options: '-x', 'QE'");
        } finally {
            app.stop();
        }
    }

    /**
     * Chain Commands in a Single Execution is not possible
     */
    @Test
    public void verifyErrorForMultipleCommandsWithoutTopCommand() {
        app
                .withProperty("quarkus.args", "greeting --name EEUU age --age 247");
        try {
            runAsync(app::start);
            app.logs().assertContains("Unmatched arguments from index 3: 'age', '--age', '247'");
        } finally {
            app.stop();
        }
    }

}
