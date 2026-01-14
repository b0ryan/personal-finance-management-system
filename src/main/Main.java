import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import ui.CommandLineInterface;

/**
 * Главный класс приложения для управления личными финансами
 */
public class Main {
    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("Предупреждение: не удалось установить кодировку UTF-8 для консоли");
        }
        
        CommandLineInterface cli = new CommandLineInterface();
        cli.run();
    }
}
