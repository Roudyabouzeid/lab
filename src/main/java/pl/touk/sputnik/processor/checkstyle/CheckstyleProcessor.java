package pl.touk.sputnik.processor.checkstyle;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.touk.sputnik.configuration.Configuration;
import pl.touk.sputnik.configuration.GeneralOption;
import pl.touk.sputnik.review.Review;
import pl.touk.sputnik.review.ReviewException;
import pl.touk.sputnik.review.ReviewProcessor;
import pl.touk.sputnik.review.ReviewResult;
import pl.touk.sputnik.review.filter.JavaFilter;
import pl.touk.sputnik.review.transformer.IOFileTransformer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@AllArgsConstructor
public class CheckstyleProcessor implements ReviewProcessor {
    private static final String SOURCE_NAME = "Checkstyle";
    private final CollectorListener collectorListener = new CollectorListener();
    private final Configuration configuration;

    @Nullable
    @Override
    public ReviewResult process(@NotNull Review review) {
        innerProcess(review, collectorListener);
        return collectorListener.getReviewResult();
    }

    @NotNull
    @Override
    public String getName() {
        return SOURCE_NAME;
    }

    public List<Map<String, Object>> analyzeFiles(List<File> files) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (File file : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    boolean containsUsername = line.contains("username");
                    boolean containsPassword = line.contains("password");

                    if (containsUsername || containsPassword) {
                        Map<String, Object> lineResult = new HashMap<>();
                        lineResult.put("lineNumber", lineNumber);
                        lineResult.put("username", containsUsername);
                        lineResult.put("password", containsPassword);
                        lineResult.put("filename", file.getName());
                        results.add(lineResult);

                        logResult(lineResult); // Log to console
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return results;
    }

    private static void logResult(Map<String, Object> result) {
        System.out.println("File: " + result.get("filename") +
                ", Line: " + result.get("lineNumber") +
                ", Contains 'username': " + result.get("username") +
                ", Contains 'password': " + result.get("password"));
    }

    private void innerProcess(@NotNull Review review, @NotNull AuditListener auditListener) {
        List<File> files = review.getFiles(new JavaFilter(), new IOFileTransformer());
        Checker checker = createChecker(auditListener);
        try {
            this.analyzeFiles(files);
            checker.process(files);
        } catch (CheckstyleException e) {
            throw new ReviewException("Unable to process files with Checkstyle", e);
        }
        checker.destroy();
    }

    @NotNull
    private Checker createChecker(@NotNull AuditListener auditListener) {
        try {
            String configurationFile = getConfigurationFilename();
            if (StringUtils.isBlank(configurationFile)) {
                throw new ReviewException("Checkstyle configuration file is not specified.");
            }
            if (!Files.exists(Paths.get(configurationFile))) {
                throw new ReviewException("Checkstyle configuration file does not exist.");
            }

            Checker checker = new Checker();
            ClassLoader moduleClassLoader = Checker.class.getClassLoader();

            Properties properties = new Properties(System.getProperties());
            properties.setProperty("config_loc", new File(configurationFile).getParent());

            checker.setModuleClassLoader(moduleClassLoader);
            checker.configure(ConfigurationLoader.loadConfiguration(configurationFile, new PropertiesExpander(properties)));
            checker.addListener(auditListener);
            return checker;
        } catch (CheckstyleException e) {
            throw new ReviewException("Unable to create Checkstyle checker", e);
        }
    }

    @Nullable
    private String getConfigurationFilename() {
        String configurationFile = configuration.getProperty(GeneralOption.CHECKSTYLE_CONFIGURATION_FILE);
        log.info("Using Checkstyle configuration file {}", configurationFile);
        return configurationFile;
    }
}
