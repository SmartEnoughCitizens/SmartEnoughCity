package com.trinity.hermes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HermesApplicationTests {

  @Test
  void contextLoads() {
    assertTrue(true);
  }

  @Test
  void mockTestAlwaysPasses() {

    MyService myService = Mockito.mock(MyService.class);
    Mockito.when(myService.greet()).thenReturn("Hello World");
    String result = myService.greet();
    assertThat(result).isEqualTo("Hello World");
  }

  static class MyService {
    String greet() {
      return "Hi";
    }
  }
}
