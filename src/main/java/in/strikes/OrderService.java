package in.strikes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * =============================================================================
 * CLASS: OrderService
 * =============================================================================
 *
 * ROLE IN THIS DEMO:
 *   This is the main business class that handles placing orders.
 *   It depends on PaymentService to process the payment as part of
 *   the order-placing flow.
 *
 * =============================================================================
 * HOW THE CIRCULAR DEPENDENCY IS FORMED IN THIS PROJECT:
 * =============================================================================
 *
 *   OrderService  ──depends on──►  PaymentService
 *       ▲                                │
 *       └────────────────────────────────┘
 *            PaymentService depends back on OrderService
 *
 *   Both classes annotated with @Component are discovered and registered
 *   by the Spring IoC container. Because both need each other, there is
 *   a circular dependency in the bean graph.
 *
 * =============================================================================
 * HOW IT WORKS WITH FIELD INJECTION (@Autowired on a field) — CURRENT CODE:
 * =============================================================================
 *
 *   Spring uses field injection here (not constructor injection).
 *   Because of this, Spring is able to resolve the circular dependency
 *   using its internal 3-level singleton cache:
 *
 *   Step 1: Spring calls `new OrderService()` using the default constructor.
 *           The object is now created but its `paymentService` field is null.
 *
 *   Step 2: Spring immediately puts this INCOMPLETE OrderService reference
 *           into its early-singleton cache (singletonFactories map).
 *
 *   Step 3: Spring creates PaymentService similarly using `new PaymentService()`.
 *
 *   Step 4: Spring needs to inject `OrderService` into PaymentService's
 *           `orderService` field. It finds the early reference from Step 2
 *           in its cache and injects it. PaymentService is now fully ready.
 *
 *   Step 5: Spring goes back and injects the now-complete PaymentService
 *           into OrderService's `paymentService` field.
 *
 *   Step 6: Both beans are fully initialized and registered in the container.
 *
 *   This works in pure Spring Core. However:
 *   ⚠️  Spring Boot 2.6+ BLOCKS circular references by default!
 *       You need spring.main.allow-circular-references=true to override.
 *       The recommended solution is always to FIX the design.
 *
 * =============================================================================
 * WHY CONSTRUCTOR INJECTION WOULD HAVE FAILED HERE:
 * =============================================================================
 *
 *   Look at the commented constructor below:
 *
 *       public OrderService(PaymentService paymentService) {
 *           this.paymentService = paymentService;
 *       }
 *
 *   If this constructor was active AND PaymentService also had a constructor
 *   injecting OrderService:
 *
 *       public PaymentService(OrderService orderService) { ... }
 *
 *   Spring would need to:
 *     → Create OrderService → needs PaymentService first
 *     → Create PaymentService → needs OrderService first
 *     → DEADLOCK: neither can be created first
 *
 *   Spring throws: BeanCurrentlyInCreationException
 *
 *   THE CRITICAL REASON IT WOULD FAIL IN THIS SPECIFIC CODE:
 *   ──────────────────────────────────────────────────────────
 *   Inside PaymentService.pay(), there is this call:
 *
 *       orderService.getOrderDetails();
 *
 *   This means PaymentService MUST have a reference to OrderService.
 *   If PaymentService's constructor requires OrderService, and
 *   OrderService's constructor requires PaymentService, Spring
 *   cannot figure out who to create first — resulting in startup failure.
 *
 *   With field injection, Spring can create an empty shell of OrderService
 *   first, give that shell to PaymentService, and later fill in the
 *   PaymentService reference back into OrderService — avoiding the deadlock.
 *
 * =============================================================================
 * DESIGN NOTE — WHY THIS IS CONSIDERED BAD DESIGN:
 * =============================================================================
 *
 *   Even though field injection makes this WORK, it is not GOOD.
 *   Here is why:
 *
 *   1. VIOLATION OF SINGLE RESPONSIBILITY PRINCIPLE (SRP):
 *      PaymentService is calling orderService.getOrderDetails() — it is
 *      reaching into order-related logic from inside a payment class.
 *      Payment processing should ONLY care about processing payments.
 *
 *   2. HARD TO TEST:
 *      You cannot write a unit test for OrderService without also wiring
 *      a PaymentService, and you cannot test PaymentService without also
 *      wiring OrderService. The two are inseparably tangled.
 *
 *   3. FRAGILE AT RUNTIME:
 *      If in the future someone adds an orderService.verifyPaymentStatus()
 *      call inside getOrderDetails(), the execution flow becomes:
 *         placeOrder → pay → getOrderDetails → verifyPaymentStatus → pay →
 *         getOrderDetails → ... (infinite loop, StackOverflowError at runtime)
 *
 *   THE CORRECT DESIGN:
 *      OrderService should call both paymentService.pay() AND its own
 *      getOrderDetails() directly, instead of relying on PaymentService
 *      to call back. See the commented line below in placeOrder().
 *
 * =============================================================================
 */
@Component
public class OrderService {

    /**
     * Field injection of PaymentService.
     *
     * @Autowired tells Spring: "After creating this object, find a PaymentService
     * bean in the container and inject it into this field."
     *
     * This happens AFTER the constructor runs — which is why it avoids the
     * circular deadlock that constructor injection would cause.
     */
    @Autowired
    private PaymentService paymentService;


    /**
     * CONSTRUCTOR INJECTION — COMMENTED OUT INTENTIONALLY
     * ─────────────────────────────────────────────────────
     * If you uncomment this constructor AND also uncomment the constructor
     * in PaymentService, the application will FAIL TO START with:
     *
     *   BeanCurrentlyInCreationException:
     *   Error creating bean with name 'orderService':
     *   Requested bean is currently in creation: Is there an unresolvable
     *   circular reference?
     *
     * WHY:
     *   Spring needs to call new OrderService(paymentService).
     *   But to get paymentService, it needs to call new PaymentService(orderService).
     *   But to get orderService, it needs to call new OrderService(paymentService).
     *   → Infinite dependency chain at bean creation time.
     *
     * ADDITIONALLY, in this specific code, PaymentService.pay() calls:
     *   orderService.getOrderDetails()
     * ...which means PaymentService MUST hold a reference to OrderService.
     * This further locks in the mutual dependency and makes constructor
     * injection impossible for this circular pair.
     */
    // public OrderService(PaymentService paymentService) {
    //     this.paymentService = paymentService;
    // }


    /**
     * Places an order by processing payment and printing confirmation.
     *
     * EXECUTION FLOW (with current working code):
     * ─────────────────────────────────────────────
     * 1. paymentService.pay() is called.
     *    → Inside pay():
     *        a. Prints "Payment done"
     *        b. Calls orderService.getOrderDetails()
     *           → Prints "Order Details"
     *        c. Returns back to placeOrder()
     * 2. Prints "Order placed"
     *
     * Console output will be:
     *     Payment done
     *     Order Details
     *     Order placed
     *
     * DESIGN NOTE — The Better Approach (commented out below):
     * ─────────────────────────────────────────────────────────
     * In clean design, OrderService itself should call getOrderDetails()
     * directly, and PaymentService should NOT call back into OrderService.
     * That would eliminate the circular dependency entirely:
     *
     *     public void placeOrder() {
     *         paymentService.pay();     // PaymentService stays independent
     *         getOrderDetails();        // OrderService handles its own logic
     *         System.out.println("Order placed");
     *     }
     *
     * With that design, the dependency graph becomes a clean one-way flow:
     *   OrderService ──► PaymentService    (no back-reference)
     */
    public void placeOrder() {
        paymentService.pay();

        // GOOD DESIGN NOTE:
        // If getOrderDetails() was called here (and NOT inside PaymentService.pay()),
        // then PaymentService would have NO reason to depend on OrderService at all.
        // That would completely break the circular dependency and also
        // allow constructor injection to work safely.
        //
        // Currently this line is commented out because getOrderDetails() is
        // already being called from inside PaymentService.pay() — calling it here
        // too would print "Order Details" twice.
        // getOrderDetails();

        System.out.println("Order placed");
    }

    /**
     * Returns the details of the current order.
     *
     * IMPORTANT — WHERE THIS METHOD IS CALLED FROM:
     * ───────────────────────────────────────────────
     * This method is NOT called from placeOrder() in the current design.
     * Instead, it is called from PaymentService.pay():
     *
     *     // Inside PaymentService.pay():
     *     orderService.getOrderDetails();
     *
     * This is exactly what creates the circular dependency:
     *   OrderService depends on PaymentService (to call .pay())
     *   PaymentService depends on OrderService (to call .getOrderDetails())
     *
     * If this method were only called from OrderService itself (or from Main),
     * PaymentService would have NO need to hold a reference to OrderService —
     * and the circular dependency would vanish completely.
     */
    public void getOrderDetails() {
        System.out.println("Order Details");
    }
}
