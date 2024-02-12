package pl.touk.sputnik.processor.checkstyle;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import pl.touk.sputnik.TestEnvironment;
import pl.touk.sputnik.configuration.Configuration;
import pl.touk.sputnik.configuration.ConfigurationSetup;
import pl.touk.sputnik.configuration.GeneralOption;
import pl.touk.sputnik.review.ReviewResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CheckstyleProcessorTest extends TestEnvironment {

    private CheckstyleProcessor fixture;

    @BeforeEach
    void setUp() {
        config = new ConfigurationSetup().setUp(ImmutableMap.of(
                GeneralOption.CHECKSTYLE_CONFIGURATION_FILE.getKey(), "src/test/resources/checkstyle/checkstyle.xml"));
        fixture = new CheckstyleProcessor(config);
    }

    @Test
    void shouldReturnBasicSunViolationsOnSimpleClass() {
        ReviewResult reviewResult = fixture.process(review());

        assertThat(reviewResult).isNotNull();
        assertThat(reviewResult.getViolations())
                .isNotEmpty()
                .hasSize(3)
                .extracting("message")
                .containsOnly(
                        "Missing package-info.java file.",
                        "Missing a Javadoc comment."
                );
    }

    @Test
    void shouldReturnAListWithTheProblemsFound() {
        List<Map<String, Object>> results = fixture.analyzeFiles(review2(true));
        assertThat(results).isNotNull();
        assertThat(results.size()).isGreaterThan(0);
    }

    @Test
    void shouldReturnAnEmptyListBecauseNoProblemFound() {
        List<Map<String, Object>> results = fixture.analyzeFiles(review2(false));
        assertThat(results).isNotNull();
        assertThat(results.size()).isEqualTo(0);
    }
    @Test
    void shouldConsiderSuppressionsWithConfigLocProperty() {
        Configuration configWithSuppressions = new ConfigurationSetup().setUp(ImmutableMap.of(
                GeneralOption.CHECKSTYLE_CONFIGURATION_FILE.getKey(), "src/test/resources/checkstyle/checkstyle-with-suppressions.xml"));
        CheckstyleProcessor fixtureWithSuppressions = new CheckstyleProcessor(configWithSuppressions);

        ReviewResult reviewResult = fixtureWithSuppressions.process(review());

        assertThat(reviewResult)
                .isNotNull()
                .extracting(ReviewResult::getViolations).asList()
                .hasSize(2)
                .extracting("message")
                .containsOnly("Missing a Javadoc comment.");
    }
}
