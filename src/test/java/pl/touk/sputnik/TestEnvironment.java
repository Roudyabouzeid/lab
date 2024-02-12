package pl.touk.sputnik;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import pl.touk.sputnik.configuration.Configuration;
import pl.touk.sputnik.configuration.ConfigurationBuilder;
import pl.touk.sputnik.review.Review;
import pl.touk.sputnik.review.ReviewFile;
import pl.touk.sputnik.review.ReviewFormatter;
import pl.touk.sputnik.review.ReviewFormatterFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.io.Resources.getResource;

public abstract class TestEnvironment {

    protected Configuration config;
    protected ReviewFormatter formatter;

    @BeforeEach
    public void setUpTestEnvironment() {
        config = ConfigurationBuilder.initFromResource("test.properties");
        formatter = ReviewFormatterFactory.get(config);
    }

    protected Review review() {
        return review("java/TestFile.java");
    }

    protected List<File> review2(Boolean withError) {
        List<File> fileList = new ArrayList<>();
        if (withError) {
            fileList.add(new File(Resources.getResource("java/TestFile2.java").getFile()));
        } else {
            fileList.add(new File(Resources.getResource("java/TestFile.java").getFile()));
        }
        return fileList;
    }


    protected Review review(String filename) {
        return new Review(ImmutableList.of(new ReviewFile(Resources.getResource(filename).getFile())), formatter);
    }

    protected Review nonExistentReview() {
        return new Review(ImmutableList.of(new ReviewFile("test")), formatter);
    }

    protected Review nonExistentReview(String filename) {
        return new Review(ImmutableList.of(new ReviewFile(filename)), formatter);
    }

    protected File getResourceAsFile(String resourceName) {
        return new File(getResource(resourceName).getFile());
    }

}
