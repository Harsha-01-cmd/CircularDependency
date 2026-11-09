package in.strikes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * =============================================================================
 * CLASS: AppConfig
 * =============================================================================
 *
 * PURPOSE:
 *   This is the Spring configuration class for the CircularDependencyDemo.
 *   It tells the Spring IoC container where to scan for beans and how to
 *   set up the application context.
 *
 * =============================================================================
 * HOW @Configuration WORKS:
 * =============================================================================
 *
 *   @Configuration marks this class as a source of Spring bean definitions.
 *   Spring creates a CGLIB-enhanced subclass proxy of this class at runtime.
 *   This proxy ensures that:
 *   - Any @Bean method called from within this class is intercepted.
 *   - Instead of creating a new object every time, the proxy returns the
 *     already-registered singleton bean from the container's cache.
 *
 *   This proxy behavior is important when you use direct method calls to
 *   wire beans (e.g., calling createOrderService(createPaymentService())).
 *
 * =============================================================================
 * HOW @ComponentScan WORKS:
 * =============================================================================
 *
 *   @ComponentScan without arguments scans the CURRENT PACKAGE and all
 *   sub-packages for classes annotated with:
 *     - @Component
 *     - @Service
 *     - @Repository
 *     - @Controller
 *     - @RestController
 *     - etc.
 *
 *   In this project, the scan covers package: in.strikes
 *   It finds and registers these beans automatically:
 *     1. OrderService  (annotated with @Component)
 *     2. PaymentService (annotated with @Component)
 *
 *   NOTE: Classes in the sub-package in.strikes.simple (A and B) are also
 *   scanned but NOT registered as beans because they are NOT annotated with
 *   @Component or any Spring stereotype annotation. They exist only as
 *   plain Java classes to demonstrate the basic circular dependency concept.
 *
 * =============================================================================
 * WHY THIS CLASS IS INTENTIONALLY EMPTY (NO @Bean METHODS):
 * =============================================================================
 *
 *   Both OrderService and PaymentService are annotated with @Component,
 *   which means Spring's component scan will automatically detect, instantiate,
 *   and register them as beans.
 *
 *   There is no need to explicitly define @Bean methods here for them.
 *   The @ComponentScan annotation does all the work.
 *
 *   If you wanted to register them manually (without @Component on the classes),
 *   you could write:
 *
 *       @Bean
 *       public OrderService orderService(PaymentService paymentService) {
 *           return new OrderService(paymentService);
 *       }
 *
 *       @Bean
 *       public PaymentService paymentService(OrderService orderService) {
 *           return new PaymentService(orderService);
 *       }
 *
 *   But those would fail with BeanCurrentlyInCreationException because they
 *   require the other bean to already exist (constructor-based).
 *
 * =============================================================================
 */
@Configuration
@ComponentScan
public class AppConfig {
    // No explicit @Bean methods needed.
    // OrderService and PaymentService are auto-discovered via @ComponentScan
    // because both are annotated with @Component in the in.strikes package.
}
