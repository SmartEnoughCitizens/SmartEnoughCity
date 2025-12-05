package com.trinity.hermes;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class HermesApplicationTests {

    @Test
    void contextLoads() {
        assertTrue(true);
    }

    @Test
    void mockTestAlwaysPasses() {
        // Create a mock object
        MyService myService = Mockito.mock(MyService.class);

        // Define mock behavior
        Mockito.when(myService.greet()).thenReturn("Hello World");

        // Call the method
        String result = myService.greet();

        // Assertion
        assertThat(result).isEqualTo("Hello World");
    }

    // Dummy service class for mocking
    static class MyService {
        String greet() {
            return "Hi";
        }
    }

}
