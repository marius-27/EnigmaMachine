package com.example.enigmamachine.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.enigmamachine.domain.MachineConfiguration;
import com.example.enigmamachine.domain.Reflector;
import com.example.enigmamachine.domain.RotorDefinition;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Windows cannot replace a file another program holds open, which is what a virus scanner does for a moment
 * after a file is written. A RandomAccessFile holds a file open in the same way, so these tests run on Windows.
 */
class LockedFileSaveTest {

    private static final MachineConfiguration OLD = new MachineConfiguration(
            List.of(RotorDefinition.I, RotorDefinition.II, RotorDefinition.III),
            Reflector.B,
            List.of(0, 0, 0),
            List.of(0, 0, 0),
            List.of());
    private static final MachineConfiguration NEW = new MachineConfiguration(
            List.of(RotorDefinition.V, RotorDefinition.I, RotorDefinition.IV),
            Reflector.C,
            List.of(3, 7, 12),
            List.of(25, 0, 9),
            List.of("QW", "ER"));

    @TempDir
    Path folder;

    private final JsonConfigurationRepository repository = new JsonConfigurationRepository();
    private Path file;

    @BeforeEach
    void saveTheOldSettings() throws Exception {
        assumeTrue(System.getProperty("os.name").startsWith("Windows"), "Only Windows locks open files.");
        file = folder.resolve("machine.json");
        repository.save(OLD, file);
    }

    @Test
    void saveWaitsForAFileThatIsOpenForAMoment() throws Exception {
        CountDownLatch opened = new CountDownLatch(1);
        CompletableFuture<Void> scanner = CompletableFuture.runAsync(() -> {
            try (RandomAccessFile open = new RandomAccessFile(file.toFile(), "r")) {
                opened.countDown();
                Thread.sleep(150);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        assertTrue(opened.await(5, TimeUnit.SECONDS));

        repository.save(NEW, file);

        scanner.get(5, TimeUnit.SECONDS);
        assertEquals(NEW, repository.load(file));
    }

    @Test
    void interruptedSaveStopsWaitingAndKeepsTheInterrupt() throws Exception {
        try (RandomAccessFile open = new RandomAccessFile(file.toFile(), "r")) {
            Thread.currentThread().interrupt();
            long start = System.nanoTime();

            assertThrows(ConfigurationFileException.class, () -> repository.save(NEW, file));

            assertTrue(Thread.interrupted(), "The interrupt is kept for the caller.");
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 500, "It did not wait for every retry.");
        }
        assertEquals(OLD, repository.load(file));
    }

    @Test
    void fileThatStaysOpenIsReportedAndKeepsItsSettings() throws Exception {
        ConfigurationFileException error;
        try (RandomAccessFile open = new RandomAccessFile(file.toFile(), "r")) {
            error = assertThrows(ConfigurationFileException.class, () -> repository.save(NEW, file));
        }

        assertTrue(error.getMessage().startsWith("Could not save machine.json"), error.getMessage());
        assertEquals(OLD, repository.load(file));
        try (var files = Files.list(folder)) {
            assertEquals(List.of(file), files.toList(), "No temporary file is left behind.");
        }
    }
}
