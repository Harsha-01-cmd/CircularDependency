package in.strikes.simple;

/**
 * =============================================================================
 * CLASS: A
 * =============================================================================
 * PURPOSE:
 *   This class exists purely as a teaching tool to demonstrate what happens
 *   when two plain Java classes have a circular dependency through their
 *   constructors — with NO Spring container involved.
 *
 * WHAT IS CIRCULAR DEPENDENCY?
 *   Circular dependency occurs when:
 *     - Object A needs object B to be created.
 *     - Object B needs object A to be created.
 *
 *   This creates a chicken-and-egg problem:
 *     "Who gets created first?"
 *
 * WHAT HAPPENS WHEN YOU CALL: new A()
 * ------------------------------------------------------------------
 *   Step 1:  JVM enters constructor of A.
 *   Step 2:  A's constructor executes: this.b = new B()
 *            → JVM now needs to construct B first.
 *   Step 3:  JVM enters constructor of B.
 *   Step 4:  B's constructor executes: this.a = new A()
 *            → JVM now needs to construct A again!
 *   Step 5:  JVM enters A's constructor again (Step 1 repeats).
 *   Step 6:  This loop never ends.
 *            A → B → A → B → A → B → (infinite)
 *
 * RESULT:
 *   The JVM call stack overflows because each method call consumes a stack
 *   frame, and they stack up infinitely without any base case to stop.
 *
 *   Error:   java.lang.StackOverflowError
 *
 * WHY SPRING DOES NOT HAVE THIS EXACT PROBLEM:
 *   Spring separates object construction from dependency injection.
 *   It uses a 3-level cache system (singletonObjects, earlySingletonObjects,
 *   singletonFactories) to expose a partially constructed bean early so that
 *   the circular partner can receive a reference before its own creation
 *   is complete — but only for FIELD or SETTER injection, NOT constructor.
 *
 * =============================================================================
 */
public class A {

    // A holds a reference to B.
    // If B also holds a reference to A and both create each other inside
    // their constructors, we get infinite recursion.
    private B b;

    /**
     * Constructor of A.
     *
     * PROBLEM HERE:
     *   This constructor directly calls `new B()` to create an instance of B.
     *   B's constructor in turn calls `new A()`.
     *   This creates infinite mutual instantiation leading to StackOverflowError.
     *
     *   The key rule: In plain Java, an object MUST be fully created
     *   before the constructor returns. So neither A nor B can "wait" for the
     *   other — they both demand the other upfront.
     */
    public A() {
        System.out.println("A created");
        // This line triggers B's constructor, which triggers A's constructor again:
        // A() -> B() -> A() -> B() -> ... StackOverflowError
        this.b = new B();
    }

    public B getB() {
        return b;
    }
}
