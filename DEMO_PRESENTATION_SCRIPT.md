# E-Canteen v2 — Demo and Implementation Presentation Script

**Speaker:** Muhammad Rizki Eka Pratama (L25020010)
**Target length:** about 12 to 13 minutes
**Format:** live demo first, then a short walk through the key code

Notes for you:
- Lines in plain text are what you say out loud.
- Lines in **[brackets]** are stage directions, what to click or open. Do not read them.
- Have two things ready before you start: the running app in a browser, and your IDE open on the `com.canteen` package.
- Speak slowly on the four code parts. Those are the parts the lecturer cares about most.

---

## 0. Opening (0:00 to 1:00)

Good morning everyone. My name is Muhammad Rizki Eka Pratama, and this is my final project for the J2EE course. It is called E-Canteen v2, a web application for ordering food at a campus canteen.

The idea started from a simple problem. At lunch time the canteen has long lines, students are in a hurry before the next class, and the stall owner has to remember every order by hand. My system lets a student order and pay ahead from the website, and then just walk over and pick the food up when it is ready. There is no delivery driver. The slogan on the login page sums it up, skip the line, order ahead.

In the next ten minutes I will first show the working app from three points of view, the student, the stall vendor, and the superadmin. After that I will open the code and explain the four parts that needed the most design work.

---

## 1. What it is and the tech stack (1:00 to 2:00)

Before the demo, the technology in one breath. It is built with Java 17 and Spring Boot, which gives the Spring MVC structure. The data layer uses MyBatis with hand written SQL, the pages are rendered with Thymeleaf, and everything is stored in a MySQL database with eleven tables. Passwords are hashed with BCrypt.

**[Open the architecture diagram, Figure 2.2 in the report or the drawio file.]**

This is the layered design. A request comes from the browser into the controller, the controller calls a service where the real rules live, the service calls a MyBatis mapper, and the mapper talks to MySQL. On the side there are two cross cutting pieces, a login interceptor that checks every protected page, and a log service that records important actions. Keep this picture in mind, because the demo follows exactly these layers.

---

## 2. Demo, the student flow (2:00 to 5:30)

**[Switch to the browser. Start on the login page.]**

Let me start as a student. This is the login page. I already have an account, so I log in.

**[Log in as a student. Land on the menu home page.]**

This is the student home. At the top is the active campus deal. Below it the food is grouped by stall, and only open stalls are shown. I can also search by name.

**[Type a keyword in search to show filtering, then clear it.]**

The search is one query in the database. The same query returns everything when the box is empty, and filters by name when I type something. I will show that query in the code part later.

**[Add items from two or three different stalls into the cart, then open the cart.]**

Now here is the first interesting design point. My cart has items from three different stalls. In a real canteen each stall cooks and is paid on its own. So the cart shows a running subtotal, but when I check out, the system will not make one big order.

**[Click Checkout. Show the price breakdown page.]**

On the checkout page the system previews the price first. You can see the subtotal, then the stall discount, then the campus promotion, and finally the total. I want to point out that these two discounts stack in order. The stall discount comes off first, then the campus promotion is taken off what is left, not off the original price. Every number here is calculated on the server, never in the browser. There are three payment methods, Alipay HK, WeChat Pay, and the meal card.

**[Pick a payment method and confirm the order.]**

And here is the result. The message says my items were split into three orders, one per stall, but they share one receipt so it still feels like a single purchase. At the same time, behind the scenes, the stock of every item I bought was reduced and my cart was emptied.

**[Go to order history, then open one order detail.]**

As a student I can follow each order. The history page even shows a banner when something is ready for pickup. If I open an order I get the full status timeline, pending, processing, ready, picked up. While an order is still pending I can also cancel it, and the stock goes back on the shelf.

---

## 3. Demo, the vendor portal (5:30 to 7:30)

**[Log out. Log in as a vendor, the account whose role is admin.]**

Now I switch to the stall owner side. This account was approved earlier, so it has the vendor portal.

**[Show the vendor dashboard.]**

This is the vendor dashboard. There is a seven day revenue chart, a donut of orders by status, and a switch to open or close the stall. These numbers come straight from the orders table for this one stall.

**[Open Manage Orders.]**

Here are the orders for my stall. I move an order forward one step at a time with this button, pending to processing to ready to picked up. Every step also writes a row into the status log, which is the timeline the student saw a moment ago.

**[Open Manage Menu, then the discount page.]**

On the menu page I can add items, edit them, restock, or hide them. And here I set one discount for the whole stall. That percentage is the stall discount that was applied during checkout earlier.

One important rule here. A vendor can only ever touch their own stall. If I try to advance an order that belongs to another stall, the system simply ignores it. I will show that check in the code.

---

## 4. Demo, the superadmin (7:30 to 9:00)

**[Log out. Log in as the superadmin.]**

Last role, the superadmin, who runs the whole canteen. There is only one superadmin account and it is created directly in the database.

**[Show the overview dashboard.]**

The overview shows the totals for the whole canteen, users, active stalls, revenue, and orders today.

**[Open Vendor Approvals.]**

This is the most important screen for the role lifecycle. When someone registers as a vendor, they wait here. When I click Approve, three things happen in one step. A stall is created, the person's account is upgraded from vendor to admin, and the new stall is linked to them. From that moment their vendor portal unlocks, without logging out.

**[Open Manage Users.]**

Here I can suspend a user, reactivate them, or reset a password. There is a guard rail, the superadmin cannot suspend another superadmin, no matter what the form sends.

**[Open the activity log.]**

And finally the audit log. Every login, every order, and every discount change is recorded here. This was also surprisingly handy while I was building and debugging.

That is the full app. Now let me show the code behind the parts that mattered most.

---

## 5. The code, four key parts (9:00 to 12:30)

**[Switch to the IDE.]**

I will not read the whole project. I will show four small pieces, the ones that carry the real design.

### Part 1, passwords are never stored in plain text (about 9:00)

**[Open UserService, the register method.]**

When a user registers, I never save the raw password. This one line hashes it with BCrypt before it goes into the database.

```java
user.setPasswordHash(passwordEncoder.encode(rawPassword)); // BCrypt hash
```

BCrypt is slow on purpose and it salts every hash, so even if the database leaked, the passwords are not readable. At login I use the matching method to compare, never plain text.

### Part 2, access control in one place (about 9:45)

**[Open LoginInterceptor.]**

Instead of repeating security checks in every controller, I put them in a single interceptor that runs before any protected page. It re-reads the user from the database on every request, so a suspended account is blocked on its very next click.

```java
User fresh = userMapper.findById(user.getId());   // re-read from DB
if (fresh == null || Boolean.FALSE.equals(fresh.getIsActive())) {
    session.invalidate();                          // suspended, kick out
    response.sendRedirect("/login?suspended=1");
    return false;
}
if (!fresh.getRole().equals(user.getRole())) {     // role changed?
    session.setAttribute("loginUser", fresh);      // vendor to admin
}
```

This same check is also where the live role change happens. When the superadmin approves a vendor, this code notices the new role on the next request and updates the session. That is why the vendor did not have to log out.

### Part 3, checkout, the per stall split and stacked discounts (about 10:45)

**[Open OrderService, the checkout method.]**

This is the heart of the project. The service groups the cart by stall and loops over each group. For each stall it adds up the lines, takes the stall discount off first, then takes the campus promotion off the remainder.

```java
BigDecimal afterStall    = subtotal.subtract(stallDiscount);
BigDecimal adminDiscount = afterStall.multiply(adminPercent)
                                     .divide(HUNDRED, 2, RoundingMode.HALF_UP);
BigDecimal total = subtotal.subtract(stallDiscount).subtract(adminDiscount);
orderMapper.insertOrder(order);            // one order per stall
```

Two things to notice. First, I use BigDecimal, not double, because this is money and rounding has to be exact. Second, all of this runs on the server. The browser never sends a price, it only sends which items and how many.

### Part 4, stock can never go negative (about 11:45)

**[Open MenuMapper.xml, the decreaseStock update.]**

The last piece is in the SQL itself. When I reduce stock at checkout, the update only succeeds when there is enough stock. The WHERE clause does the protection.

```xml
<update id="decreaseStock">
    UPDATE menu_items
    SET stock = stock - #{qty},
        is_available = CASE WHEN (stock - #{qty}) &lt;= 0
                            THEN FALSE ELSE is_available END
    WHERE id = #{id} AND stock >= #{qty}   <!-- never go negative -->
</update>
```

So if two students try to buy the last portion at the same time, only one update can win. The other simply does not match the condition. It also flips the item to unavailable the moment stock hits zero, so a sold out dish disappears from the menu on its own.

One more design note in the database. When I create the order lines, I copy the item name and price into the order, instead of joining back to the menu later. An order is a historical record, so if a vendor changes a price tomorrow, an old receipt must not change. I traded a little duplicated data for correct receipts, which is the right call for anything about money.

---

## 6. Closing (12:30 to 13:00)

To wrap up. E-Canteen v2 is a complete campus pre order system across three roles, built on the layered J2EE structure, controllers, services, MyBatis mappers, and MySQL behind Thymeleaf pages.

I will be honest about the limits. It does not move real money, payment is confirmed at the stall. There is no live notification yet, and there are no automated tests, so I checked everything by hand. The natural next steps would be real online payment, a notification when an order is ready, and a set of tests.

What I take away from this project is how the layers of a Java web application actually fit together, which was the whole point. Thank you. I am happy to take any questions.

---

## Backup answers for likely questions

- **Why MyBatis and not JPA or Hibernate?** I wanted to write and see the real SQL for learning, and to control performance. JPA hides the query, MyBatis keeps it visible.
- **Why one order per stall?** Each stall cooks and is paid separately, so each needs its own order and status. The shared group id keeps the student experience as one receipt.
- **How do you handle two checkouts at the same time?** The stock update has a condition in the WHERE clause, so only the update that still sees enough stock succeeds.
- **Is it secure enough for real money?** No, and I do not claim that. It covers the common risks for a campus app, hashed passwords, role checks, ownership checks, server side totals, and an audit log.
- **What was the hardest part?** Getting the per stall split and the stacked discounts to match between the preview and the final saved order, and the live role change inside the interceptor.
