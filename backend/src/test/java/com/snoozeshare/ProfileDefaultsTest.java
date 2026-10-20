package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/** With no profile selected the application runs as dev (E19). */
class ProfileDefaultsTest {

    @Test
    void defaultProfileIsDev() throws IOException {
        List<PropertySource<?>> sources =
            new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        assertThat(sources).anySatisfy(source ->
            assertThat(source.getProperty("spring.profiles.default")).isEqualTo("dev"));
    }
}
