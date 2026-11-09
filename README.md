# CircularDependency 🔄

A focused Spring Core project demonstrating **circular dependency** — what it is, why it happens, how Spring resolves it, why it is bad design, and what the correct approach is.

---

## 🧠 What is Circular Dependency?

Circular dependency occurs when **two or more beans depend on each other**, forming a loop:

```
OrderService ──depends on──► PaymentService
     ▲                               │
     └──────── depends on ───────────┘
```

- `OrderService` needs `PaymentService` → to call `pay()`
- `PaymentService` needs `OrderService` → to call `getOrderDetails()`

This two-way dependency loop is the definition of circular dependency.

---

## 📁 Project Structure

```
CircularDependency/
├── pom.xml
└── src/main/java/in/strikes/
    ├── AppConfig.java          → Spring @Configuration + @ComponentScan
    ├── Main.java               → Application entry point, boots Spring context
    ├── OrderService.java       → @Component — depends on PaymentService
    ├── PaymentService.java     → @Component — depends on OrderService (the cycle)
    └── simple/
        ├── A.java              → Plain Java class with circular constructor dependency
        └── B.java              → Plain Java class with circular constructor dependency
```

---

## 🗂️ File-by-File Guide

### `simple/A.java` & `simple/B.java` — Plain Java: The StackOverflow Problem
These two classes show what circular dependency looks like **without Spring**, using only plain Java constructors.

```java
public class A {
    public A() { this.b = new B(); }  // A creates B
}
public class B {
    public B() { this.a = new A(); }  // B creates A
}
```

**Result:** `java.lang.StackOverflowError`

The JVM call stack fills up infinitely:
`new A()` → `new B()` → `new A()` → `new B()` → ...

---

### `PaymentService.java` — The Root Cause of the Spring Circular Dependency

```java
@Component
public class PaymentService {

    @Autowired
    private OrderService orderService;  // ← Needs OrderService

    public void pay() {
        System.out.println("Payment done");
        orderService.getOrderDetails(); // ← This line creates the cycle
    }
}
```

`PaymentService` calls `orderService.getOrderDetails()` — a method that belongs to `OrderService`. This forces `PaymentService` to hold a reference to `OrderService`, forming the circular loop.

---

### `OrderService.java` — The Other Half of the Cycle

```java
@Component
public class OrderService {

    @Autowired
    private PaymentService paymentService;  // ← Needs PaymentService

    public void placeOrder() {
        paymentService.pay();
        System.out.println("Order placed");
    }

    public void getOrderDetails() {
        System.out.println("Order Details");
    }
}
```

---

## ⚙️ Why Field Injection Works (Spring's 3-Level Cache)

Spring resolves the circular dependency **at startup** using its internal singleton cache:

```
Step 1: Creates empty OrderService (fields = null)
Step 2: Puts early OrderService reference in singletonFactories cache
Step 3: Creates empty PaymentService
Step 4: Needs to inject OrderService into PaymentService → finds early reference in cache ✅
Step 5: PaymentService is now fully initialized
Step 6: Injects fully ready PaymentService into OrderService
Step 7: Both beans are fully initialized and ready
```

> **This ONLY works with `@Autowired` field or setter injection — NOT constructor injection.**

---

## ❌ Why Constructor Injection Fails

If both classes used constructors:

```java
// OrderService
public OrderService(PaymentService paymentService) { ... }

// PaymentService
public PaymentService(OrderService orderService) { ... }
```

Spring would need:
- `OrderService` → needs `PaymentService` first
- `PaymentService` → needs `OrderService` first
- **Deadlock!** Neither can be created.

**Error:** `BeanCurrentlyInCreationException`

---

## ⚠️ Spring Boot Warning

In **Spring Boot 2.6+**, circular references are **disabled by default**. Even with field injection, Spring Boot will refuse to start and show:

```
The dependencies of some of the beans in the application context form a cycle:
orderService → paymentService → orderService
```

You would need to explicitly allow it with:
```properties
spring.main.allow-circular-references=true
```
But this is **not recommended**. The correct solution is to fix the design.

---

## ✅ The Correct Design (How to Eliminate It)

Move `getOrderDetails()` call out of `PaymentService` and into `OrderService`:

**PaymentService (independent, no back-reference):**
```java
@Component
public class PaymentService {
    public void pay() {
        System.out.println("Payment done");
        // No reference to OrderService needed here!
    }
}
```

**OrderService (orchestrates everything):**
```java
@Component
public class OrderService {

    @Autowired
    private PaymentService paymentService;

    public void placeOrder() {
        paymentService.pay();
        getOrderDetails();           // OrderService calls its own method!
        System.out.println("Order placed");
    }

    public void getOrderDetails() {
        System.out.println("Order Details");
    }
}
```

**Result:** Clean one-way dependency — no circular reference:
```
OrderService ──► PaymentService    (PaymentService has no dependency)
```

---

## 🚀 Expected Output

When `Main.java` runs with the **current (bad but working) design**:
```
Payment done
Order Details
Order placed
```

---

## 🛠️ Build & Run

**Prerequisites:** JDK 17+ · Maven 3.8+

```bash
git clone https://github.com/Harsha-01-cmd/CircularDependency.git
cd CircularDependency
mvn compile exec:java -Dexec.mainClass="in.strikes.Main"
```
