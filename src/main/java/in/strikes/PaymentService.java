package in.strikes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * =============================================================================
 * CLASS: PaymentService
 * =============================================================================
 *
 * ROLE IN THIS DEMO:
 *   This class is responsible for processing payment for an order.
 *   In a real application, it would call a payment gateway, validate cards,
 *   initiate UPI transactions, etc.
 *
 *   In this demo, it also calls orderService.getOrderDetails() — which is
 *   the deliberate "bad design" that creates the circular dependency and
 *   serves as the core teaching point of this project.
 *
 * =============================================================================
 * HOW PAYMENTSERVICE IS THE "OTHER HALF" OF THE CIRCULAR DEPENDENCY:
 * =============================================================================
 *
 *   The circular dependency graph looks like this:
 *
 *       OrderService  ──depends on──►  PaymentService
 *            ▲                               │
 *            └───────────── depends on ──────┘
 *
 *   - OrderService needs PaymentService → to call .pay()
 *   - PaymentService needs OrderService → to call .getOrderDetails()
 *
 *   This two-way dependency loop is the DEFINITION of circular dependency.
 *
 * =============================================================================
 * WHY THIS CLASS NEEDS OrderService AT ALL:
 * =============================================================================
 *
 *   The only reason PaymentService holds a reference to OrderService is
 *   because of this single line inside pay():
 *
 *       orderService.getOrderDetails();
 *
 *   If this line is removed (or moved to OrderService.placeOrder() where it
 *   logically belongs), PaymentService would become fully independent —
 *   it would have zero dependencies — and the circular dependency would
 *   cease to exist entirely.
 *
 *   This is the core design lesson of this demo:
 *     → Calling back into the CALLER from inside the CALLEE creates a
 *       circular dependency.
 *     → The callee (PaymentService) should not need to call methods on
 *       the caller (OrderService). That is the caller's responsibility.
 *
 * =============================================================================
 * HOW IT WORKS WITH FIELD INJECTION — CURRENT APPROACH:
 * =============================================================================
 *
 *   Spring can resolve this circular dependency ONLY because field injection
 *   (@Autowired on a field) is used in BOTH classes.
 *
 *   Spring's internal resolution using its 3-level cache:
 *
 *   Step 1: Spring creates an empty shell of OrderService (via default constructor).
 *           Its fields are all null at this point.
 *
 *   Step 2: Spring registers this early/incomplete OrderService reference in its
 *           singletonFactories cache (the "third level" of the cache).
 *
 *   Step 3: Spring creates an empty shell of PaymentService.
 *
 *   Step 4: Spring needs to inject OrderService into PaymentService's
 *           `orderService` field. It looks in the cache and finds the early
 *           reference from Step 2 — it injects that (even though it is not
 *           fully initialized yet).
 *
 *   Step 5: PaymentService is now fully created (its `orderService` field points
 *           to the early OrderService reference).
 *
 *   Step 6: Spring goes back to OrderService and injects the now-complete
 *           PaymentService into its `paymentService` field.
 *
 *   Step 7: Both beans are promoted to the final singletonObjects cache.
 *           The application starts successfully.
 *
 *   ⚠️  IMPORTANT CAVEAT:
 *       This only works in pure Spring Core (spring-context dependency).
 *       In SPRING BOOT (2.6+), circular references are DISABLED by default.
 *       Spring Boot will throw:
 *
 *           The dependencies of some of the beans in the application context
 *           form a cycle:
 *           orderService -> paymentService -> orderService
 *
 *       To allow it in Spring Boot (not recommended):
 *           spring.main.allow-circular-references=true
 *
 * =============================================================================
 * WHY CONSTRUCTOR INJECTION WOULD HAVE BROKEN EVERYTHING:
 * =============================================================================
 *
 *   Look at the commented-out constructor below:
 *
 *       public PaymentService(OrderService orderService) {
 *           this.orderService = orderService;
 *       }
 *
 *   If you uncomment this AND also uncomment the constructor in OrderService:
 *
 *       public OrderService(PaymentService paymentService) { ... }
 *
 *   Here is what Spring would attempt:
 *
 *     1. Create OrderService → needs PaymentService injected via constructor.
 *     2. Create PaymentService first → needs OrderService injected via constructor.
 *     3. Create OrderService first → needs PaymentService injected via constructor.
 *     4. DEADLOCK: infinite dependency chain.
 *
 *   Spring CANNOT put an early reference into a constructor argument.
 *   The constructor MUST receive a fully initialized object.
 *   Since neither can be fully initialized without the other, the application
 *   FAILS TO START with:
 *
 *       BeanCurrentlyInCreationException:
 *       Error creating bean with name 'paymentService':
 *       Requested bean is currently in creation.
 *
 *   THE REASON IT IS ESPECIALLY PROBLEMATIC IN THIS CODE:
 *   ──────────────────────────────────────────────────────
 *   Inside pay(), this line exists:
 *
 *       orderService.getOrderDetails();
 *
 *   Because of this line, PaymentService absolutely requires OrderService.
 *   If PaymentService used a constructor for injection, Spring would need
 *   to have a fully created OrderService before creating PaymentService.
 *   But OrderService itself needs PaymentService in its constructor.
 *   → Neither can be created → startup crash.
 *
 * =============================================================================
 * DESIGN NOTE — WHY THIS IS BAD DESIGN:
 * =============================================================================
 *
 *   From a software design perspective, PaymentService should ONLY process
 *   payments. It should NOT care about what an order looks like or what the
 *   order details are. Its job ends at "Payment done."
 *
 *   Calling orderService.getOrderDetails() from inside pay() means:
 *
 *   1. VIOLATION OF SINGLE RESPONSIBILITY PRINCIPLE (SRP):
 *      PaymentService is now doing two things — processing payment AND
 *      fetching order details.
 *
 *   2. TIGHT COUPLING:
 *      PaymentService cannot be reused for any other kind of payment
 *      (subscriptions, refunds, top-ups) without dragging OrderService along.
 *
 *   3. RUNTIME RISK:
 *      If in the future someone adds another payment-related call inside
 *      getOrderDetails(), the call chain becomes circular at RUNTIME too:
 *         placeOrder() → pay() → getOrderDetails() → pay() → ...
 *      This would cause: java.lang.StackOverflowError
 *
 *   THE FIX (CLEAN DESIGN):
 *      Remove orderService from PaymentService entirely.
 *      Let OrderService handle the call to getOrderDetails() directly:
 *
 *          // In OrderService.placeOrder():
 *          paymentService.pay();       // PaymentService stays self-contained
 *          getOrderDetails();          // OrderService handles its own detail
 *          System.out.println("Order placed");
 *
 *      With that change, PaymentService would have ZERO dependencies —
 *      no @Autowired field, no constructor parameter, no circular reference.
 *
 * =============================================================================
 */
@Component
public class PaymentService {

    /**
     * Field injection of OrderService.
     *
     * WHY THIS EXISTS:
     *   Because pay() calls orderService.getOrderDetails(), PaymentService
     *   must hold a reference to OrderService.
     *
     * WHY FIELD INJECTION IS USED (NOT CONSTRUCTOR):
     *   If this were injected via constructor, the circular dependency would
     *   cause BeanCurrentlyInCreationException at startup (see class-level
     *   comments above for the full explanation).
     *
     *   Field injection allows Spring to:
     *   1. Create PaymentService using its default no-arg constructor first.
     *   2. Inject OrderService into this field AFTER construction.
     *   This breaks the deadlock that constructor injection would cause.
     *
     * ⚠️  NOTE FOR LEARNERS:
     *   In production code, field injection with @Autowired is generally
     *   discouraged. Constructor injection is preferred because:
     *   - It makes dependencies explicit and visible.
     *   - It makes the class easier to unit test (no need for Spring context).
     *   - It guarantees the class is in a valid state after construction.
     *   But here, field injection is the ONLY option because constructor
     *   injection is physically impossible due to the circular dependency.
     *   This is itself a signal that the design needs to be fixed.
     */
    @Autowired
    private OrderService orderService;


    /**
     * CONSTRUCTOR INJECTION — COMMENTED OUT INTENTIONALLY
     * ─────────────────────────────────────────────────────
     *
     * If you uncomment this constructor AND also uncomment the constructor
     * in OrderService, the application WILL NOT START.
     *
     * ERROR:
     *   BeanCurrentlyInCreationException:
     *   Error creating bean with name 'paymentService':
     *   Requested bean is currently in creation: Is there an unresolvable
     *   circular reference?
     *
     * WHY IT FAILS:
     *   Spring needs to call: new PaymentService(orderService)
     *   But to get orderService bean, Spring needs: new OrderService(paymentService)
     *   But to get paymentService bean, Spring needs: new PaymentService(orderService)
     *   → Infinite regress. Neither can be constructed first.
     *
     * THE SPECIFIC REASON IT IS WORSE HERE:
     *   Because this pay() method calls orderService.getOrderDetails(), there is
     *   no way to separate PaymentService from OrderService with constructor
     *   injection — they permanently need each other. The only escape is to
     *   redesign so that PaymentService does NOT need to call back into
     *   OrderService at all.
     */
    // public PaymentService(OrderService orderService) {
    //     this.orderService = orderService;
    // }


    /**
     * Processes the payment for an order.
     *
     * WHAT THIS METHOD DOES:
     *   1. Prints "Payment done" — simulates payment processing.
     *   2. Calls orderService.getOrderDetails() — this is the DESIGN PROBLEM.
     *
     * WHY CALLING orderService.getOrderDetails() HERE IS BAD DESIGN:
     * ────────────────────────────────────────────────────────────────
     *   - PaymentService is the CALLEE in the flow: OrderService calls PaymentService.
     *   - A callee calling back into the caller creates a circular dependency.
     *   - Payment processing has nothing to do with retrieving order details.
     *   - Fetching order details is ORDER logic, not PAYMENT logic.
     *
     * WHAT SHOULD HAPPEN INSTEAD (CLEAN DESIGN):
     *   OrderService should call getOrderDetails() on itself:
     *
     *       // In OrderService.placeOrder():
     *       paymentService.pay();   // Pay — PaymentService has no back-reference
     *       getOrderDetails();      // Order detail fetching stays in OrderService
     *       System.out.println("Order placed");
     *
     *   With that design, PaymentService would look like this:
     *
     *       public void pay() {
     *           System.out.println("Payment done");
     *           // No orderService reference needed here at all!
     *       }
     *
     *   That would make PaymentService a fully self-contained, independent class
     *   with no circular dependency and no need for @Autowired OrderService.
     *
     * RUNTIME EXECUTION (as it currently stands):
     *   When OrderService.placeOrder() calls this method:
     *   → "Payment done" is printed.
     *   → orderService.getOrderDetails() runs → "Order Details" is printed.
     *   → This method returns.
     *   → Back in placeOrder(): "Order placed" is printed.
     *
     *   Final console output:
     *     Payment done
     *     Order Details
     *     Order placed
     */
    public void pay() {
        System.out.println("Payment done");

        // ─────────────────────────────────────────────────────────────────────
        // THIS LINE IS THE ROOT CAUSE OF THE CIRCULAR DEPENDENCY IN THIS PROJECT.
        //
        // Why it exists here:  To demonstrate what happens when a callee
        //                      calls back into the caller.
        //
        // Why it works now:    Both classes use field injection (@Autowired),
        //                      so Spring can resolve the cycle using its
        //                      3-level singleton cache.
        //
        // Why it would FAIL    If constructor injection were used in both
        // with constructors:   classes, Spring would deadlock trying to
        //                      create either bean and throw
        //                      BeanCurrentlyInCreationException at startup.
        //
        // Why it is bad:       Payment processing logic should NOT reach back
        //                      into Order logic. This breaks SRP and creates
        //                      tight coupling between two business domains.
        // ─────────────────────────────────────────────────────────────────────
        orderService.getOrderDetails();
    }
}
