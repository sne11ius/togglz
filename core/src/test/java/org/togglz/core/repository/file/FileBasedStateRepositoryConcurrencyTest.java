package org.togglz.core.repository.file;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.fest.assertions.data.MapEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

public class FileBasedStateRepositoryConcurrencyTest {

    private File testFile;

    private ExecutorService executor;

    private final int NUMBER_OF_FEATURES = 100;

    @Before
    public void before() throws IOException {
        testFile = File.createTempFile(this.getClass().getSimpleName(), "test");
        executor = Executors.newFixedThreadPool(NUMBER_OF_FEATURES);
    }

    @Test
    public void shouldWorkUnderHeavyLoad() throws Exception {

        final FileBasedStateRepository repo = new FileBasedStateRepository(testFile);

        // Step 1: concurrently write a large number of state
        for (int i = 0; i < NUMBER_OF_FEATURES; i++) {

            // build up a feature state containing some data
            String name = "FEATURE" + i;
            Feature feature = new TestFeature(name);
            final FeatureState state = new FeatureState(feature)
                .setStrategyId("strategy-for-" + name)
                .setParameter("param-of-" + name, "some-value-of-" + name);

            // queue a thread writing that state
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    repo.setFeatureState(state);
                }
            });

        }

        // Step 2: Wait for all threads to finish
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Step 3: Verify the state written to the repository
        for (int i = 0; i < NUMBER_OF_FEATURES; i++) {

            // read the state
            String name = "FEATURE" + i;
            TestFeature feature = new TestFeature(name);
            FeatureState state = repo.getFeatureState(feature);

            // verify that the state is as expected
            assertThat(state).isNotNull();
            assertThat(state.getStrategyId()).isEqualTo("strategy-for-" + name);
            assertThat(state.getParameterMap())
                .hasSize(1)
                .contains(MapEntry.entry("param-of-" + name, "some-value-of-" + name));

        }

    }

    @After
    public void after() throws Exception {
        executor.shutdownNow();
        testFile.delete();
    }

    private class TestFeature implements Feature {

        private final String name;

        private TestFeature(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean isActive() {
            throw new UnsupportedOperationException();
        }

    }

}
