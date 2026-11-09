package in.strikes.simple;

/**
 * =============================================================================
 * CLASS: B
 * =============================================================================
 * PURPOSE:
 *   This is the second half of the plain Java circular dependency demo.
 *   It mirrors the problem shown in class A — both A and B mutually try
 *   to instantiate each other inside their constructors.
 *
 * HOW IT FITS INTO THE CIRCULAR DEPENDENCY:
 *
 *   A's constructor calls:  new B()
 *   B's constructor calls:  new A()
 *
 *   This creates a chain reaction:
 *
 *       new A()           <- You write this in your code
 *         └─► new B()     <- A's constructor triggers this
 *               └─► new A()  <- B's constructor triggers this
 *                     └─► new B()
 *                           └─► new A()
 *                                 ... (never stops)
 *
 *   CRASH: java.lang.StackOverflowError
 *
 * HOW IS B DIFFERENT FROM A?
 *   It is symmetric. Both are equally "guilty" of the circular dependency.
 *   It doesn't matter which one you try to create first — the outcome is
 *   always a StackOverflowError.
 *
 * LESSON:
 *   In plain Java, circular dependency through constructors is IMPOSSIBLE
 *   to resolve. You must break the cycle:
 *     Option 1: One class should not depend on the other at all.
 *     Option 2: Use a setter or a factory method to inject lazily after construction.
 *     Option 3: Introduce a third class that both A and B depend on instead of
 *               depending directly on each other.
 *
 * SPRING'S PARTIAL SOLUTION:
 *   Spring's IoC container can resolve circular dependencies for SINGLETON beans
 *   when field injection (@Autowired on a field) or setter injection is used,
 *   because Spring creates the object first (using a no-arg or primary constructor)
 *   and THEN injects the dependency — not during construction.
 *   But even Spring CANNOT solve constructor-based circular dependency.
 *
 * =============================================================================
 */
public class B {

    // B holds a reference to A.
    // Combined with A holding a reference to B inside their respective
    // constructors, this creates the circular instantiation problem.
    private A a;

    /**
     * Constructor of B.
     *
     * PROBLEM HERE:
     *   This constructor directly calls `new A()`.
     *   But A's constructor calls `new B()`.
     *   We are right back where we started — B calling A calling B calling A...
     *
     * WHAT JAVA REQUIRES:
     *   Java requires a constructor to COMPLETE before its result (the new object)
     *   can be used. Since A cannot complete without B, and B cannot complete
     *   without A, the program is logically stuck in an infinite loop.
     *
     * RUNTIME EFFECT:
     *   Each constructor call adds a new stack frame on the JVM's call stack.
     *   The stack has a finite size. When it fills up:
     *   → java.lang.StackOverflowError is thrown.
     */
    public B() {
        System.out.println("B created");
        // This line triggers A's constructor, which triggers B's constructor again:
        // B() -> A() -> B() -> A() -> ... StackOverflowError
        this.a = new A();
    }

    public A getA() {
        return a;
    }
}
