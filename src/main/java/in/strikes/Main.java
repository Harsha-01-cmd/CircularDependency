package in.strikes;

import in.strikes.simple.A;
import in.strikes.simple.B;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * =============================================================================
 * CLASS: Main
 * =============================================================================
 *
 * PURPOSE:
 *   This is the application entry point for the CircularDependencyDemo.
 *   It boots the Spring IoC container, retrieves the OrderService bean,
 *   and triggers the order placement flow to demonstrate that the Spring-managed
 *   circular dependency (via field injection) works correctly at runtime.
 *
 * =============================================================================
 * HOW THE SPRING CONTAINER BOOTS UP:
 * =============================================================================
 *
 *   Line:
 *     ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
 *
 *   What happens internally:
 *
 *   1. Spring reads AppConfig.class.
 *   2. It sees @ComponentScan → scans package in.strikes and sub-packages.
 *   3. It finds @Component on OrderService → registers it as a bean definition.
 *   4. It finds @Component on PaymentService → registers it as a bean definition.
 *   5. It resolves dependencies using the 3-level singleton cache:
 *      a. Creates an empty OrderService (fields are null).
 *      b. Exposes this early OrderService to the internal cache.
 *      c. Creates an empty PaymentService.
 *      d. Injects the early OrderService reference into PaymentService's
 *         `orderService` field via @Autowired.
 *      e. PaymentService is now fully initialized.
 *      f. Injects the fully initialized PaymentService into OrderService's
 *         `paymentService` field via @Autowired.
 *      g. OrderService is now fully initialized.
 *   6. Both beans are ready in the application context.
 *
 *   ⚠️  This works because FIELD INJECTION is used in both OrderService
 *       and PaymentService. If CONSTRUCTOR INJECTION were used, step 5
 *       would fail with BeanCurrentlyInCreationException.
 *
 * =============================================================================
 * ABOUT THE COMMENTED A AND B IMPORTS:
 * =============================================================================
 *
 *   Classes A and B (from package in.strikes.simple) demonstrate the same
 *   circular dependency problem in PLAIN JAVA without Spring.
 *
 *   If you were to try:
 *       A a = new A();
 *   it would immediately throw java.lang.StackOverflowError because:
 *       new A() creates new B()
 *       new B() creates new A()
 *       new A() creates new B()
 *       ... (infinite recursion until stack overflows)
 *
 *   These imports are kept here to show that A and B exist as teaching examples,
 *   but calling new A() or new B() is intentionally not demonstrated in main()
 *   because it would crash the program immediately.
 *
 * =============================================================================
 * RUNTIME EXECUTION FLOW:
 * =============================================================================
 *
 *   When main() runs:
 *
 *   1. Spring context starts (see boot up steps above).
 *   2. context.getBean(OrderService.class)
 *      → Returns the fully initialized singleton OrderService bean.
 *   3. order.placeOrder() is called.
 *      → Internally calls paymentService.pay()
 *         → Prints "Payment done"
 *         → Calls orderService.getOrderDetails()
 *            → Prints "Order Details"
 *      → Returns to placeOrder()
 *      → Prints "Order placed"
 *
 *   Final console output:
 *     Payment done
 *     Order Details
 *     Order placed
 *
 * =============================================================================
 */
public class Main {

    public static void main(String[] args) {

        // ─────────────────────────────────────────────────────────────────────
        // BOOT UP THE SPRING IOC CONTAINER
        //
        // AnnotationConfigApplicationContext reads the Java-based configuration
        // class (AppConfig) instead of an XML file.
        //
        // During this single line, Spring:
        //  - Scans for @Component classes in package in.strikes
        //  - Creates and wires all singleton beans (including the circular pair:
        //    OrderService <─► PaymentService)
        //  - Resolves the circular dependency using the early-singleton cache
        //    (possible only because @Autowired field injection is used)
        // ─────────────────────────────────────────────────────────────────────
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // ─────────────────────────────────────────────────────────────────────
        // RETRIEVE THE OrderService BEAN FROM THE CONTAINER
        //
        // At this point, OrderService already has PaymentService injected,
        // and PaymentService already has OrderService injected.
        // Both beans are fully initialized and ready to use.
        // ─────────────────────────────────────────────────────────────────────
        OrderService order = context.getBean(OrderService.class);

        // ─────────────────────────────────────────────────────────────────────
        // TRIGGER THE ORDER PLACEMENT FLOW
        //
        // This call demonstrates that even though a circular dependency exists
        // between OrderService and PaymentService, the application runs correctly
        // because Spring resolved the dependency graph at startup using
        // field injection and the early-singleton cache.
        //
        // The call chain:
        //   placeOrder() → pay() → getOrderDetails()
        //
        // Output:
        //   Payment done
        //   Order Details
        //   Order placed
        // ─────────────────────────────────────────────────────────────────────
        order.placeOrder();

        // ─────────────────────────────────────────────────────────────────────
        // PLAIN JAVA CIRCULAR DEPENDENCY DEMO (would cause StackOverflowError)
        //
        // The following line is intentionally not written because it would
        // crash the program:
        //
        //   A a = new A();
        //      → A() creates B()
        //      → B() creates A()
        //      → A() creates B()
        //      → ... StackOverflowError
        //
        // See classes in.strikes.simple.A and in.strikes.simple.B for the
        // full explanation of why plain Java cannot resolve constructor-based
        // circular dependencies.
        // ─────────────────────────────────────────────────────────────────────
    }
}