package de.firemage.treeg;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class RegExParserTest {

    @TestFactory
    Stream<DynamicTest> generateTests() throws URISyntaxException, IOException {
        return Files.readAllLines(Path.of(this.getClass().getResource("regex.txt").toURI()))
                .stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> DynamicTest.dynamicTest(line, () -> {
                    try {
                        var regex = RegExParser.parse(line);
                        System.out.println(regex.toTree());
                        assertEquals(line, regex.toRegEx());
                    } catch (InvalidRegExSyntaxException e) {
                        fail("Failed to parse '" + line + "'", e);
                    }
                }));
    }
}
