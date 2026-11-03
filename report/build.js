// Builds the E-Canteen J2EE course report (.docx)
const fs = require("fs");
const path = require("path");
const sharp = require("sharp");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  AlignmentType, HeadingLevel, BorderStyle, WidthType, ShadingType,
  PageBreak, ImageRun, Header, Footer, PageNumber, TableOfContents,
  LevelFormat, VerticalAlign
} = require("docx");

const ASSETS = path.join(__dirname, "assets");
const CONTENT_W = 9360;          // letter, 1" margins, in DXA
const CONTENT_PX = 624;          // 6.5"
const RED = "E4322B", YELLOW = "F6A609", INK = "1C1C1C", GREY = "555555";

// ---------- image helpers ----------
const dims = {};
async function loadDims() {
  const files = fs.readdirSync(ASSETS).filter(f => f.endsWith(".png"));
  for (const f of files) {
    const m = await sharp(path.join(ASSETS, f)).metadata();
    dims[f] = { w: m.width, h: m.height };
  }
}
function fig(file, caption, maxPx = 560) {
  const d = dims[file];
  const w = Math.min(maxPx, d.w);
  const h = Math.round(w * d.h / d.w);
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 140, after: 40 },
      children: [new ImageRun({
        type: "png",
        data: fs.readFileSync(path.join(ASSETS, file)),
        transformation: { width: w, height: h },
        altText: { title: caption, description: caption, name: file }
      })]
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 160 },
      children: [new TextRun({ text: caption, italics: true, size: 18, color: GREY })]
    })
  ];
}

// ---------- text helpers ----------
function h1(text) {
  return new Paragraph({ heading: HeadingLevel.HEADING_1, pageBreakBefore: true, children: [new TextRun(text)] });
}
function h2(text) {
  return new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(text)] });
}
function h3(text) {
  return new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun(text)] });
}
function p(text, opts = {}) {
  return new Paragraph({
    spacing: { after: 140, line: 300 },
    alignment: opts.justify === false ? AlignmentType.LEFT : AlignmentType.JUSTIFIED,
    children: [new TextRun({ text, size: 22 })]
  });
}
// paragraph with mixed runs (for bold lead-ins)
function pmix(runs) {
  return new Paragraph({
    spacing: { after: 140, line: 300 },
    alignment: AlignmentType.JUSTIFIED,
    children: runs.map(r => typeof r === "string"
      ? new TextRun({ text: r, size: 22 })
      : new TextRun({ text: r.t, bold: !!r.b, italics: !!r.i, size: 22, color: r.c }))
  });
}
function bullet(text) {
  return new Paragraph({
    numbering: { reference: "bullets", level: 0 },
    spacing: { after: 70, line: 290 },
    children: [new TextRun({ text, size: 22 })]
  });
}
function num(text) {
  return new Paragraph({
    numbering: { reference: "nums", level: 0 },
    spacing: { after: 70, line: 290 },
    children: [new TextRun({ text, size: 22 })]
  });
}

// ---------- code block ----------
function codeBlock(code, caption) {
  const lines = code.replace(/\t/g, "    ").split("\n");
  const rows = lines.map(l => new TableRow({
    children: [new TableCell({
      borders: noBorder,
      margins: { top: 0, bottom: 0, left: 120, right: 80 },
      shading: { fill: "F4F4F4", type: ShadingType.CLEAR },
      children: [new Paragraph({
        spacing: { after: 0, line: 250 },
        children: [new TextRun({ text: l.length ? l : " ", font: "Consolas", size: 17 })]
      })]
    })]
  }));
  const out = [new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: [CONTENT_W],
    borders: {
      top: { style: BorderStyle.SINGLE, size: 4, color: "D9D9D9" },
      bottom: { style: BorderStyle.SINGLE, size: 4, color: "D9D9D9" },
      left: { style: BorderStyle.SINGLE, size: 4, color: "D9D9D9" },
      right: { style: BorderStyle.SINGLE, size: 4, color: "D9D9D9" },
      insideHorizontal: { style: BorderStyle.NONE },
      insideVertical: { style: BorderStyle.NONE }
    },
    rows
  })];
  if (caption) out.push(new Paragraph({
    spacing: { before: 40, after: 160 },
    children: [new TextRun({ text: caption, italics: true, size: 18, color: GREY })]
  }));
  else out.push(new Paragraph({ spacing: { after: 120 }, children: [] }));
  return out;
}
const noBorder = {
  top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE },
  left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE }
};

// ---------- simple info table ----------
function infoTable(rows, head) {
  const border = { style: BorderStyle.SINGLE, size: 2, color: "CCCCCC" };
  const borders = { top: border, bottom: border, left: border, right: border };
  const w1 = 2600, w2 = CONTENT_W - 2600;
  const trs = [];
  if (head) {
    trs.push(new TableRow({ tableHeader: true, children: head.map((t, i) => new TableCell({
      borders, width: { size: i === 0 ? w1 : w2, type: WidthType.DXA },
      shading: { fill: INK, type: ShadingType.CLEAR },
      margins: { top: 60, bottom: 60, left: 120, right: 120 },
      children: [new Paragraph({ children: [new TextRun({ text: t, bold: true, color: "FFFFFF", size: 20 })] })]
    })) }));
  }
  rows.forEach(r => trs.push(new TableRow({ children: r.map((t, i) => new TableCell({
    borders, width: { size: i === 0 ? w1 : w2, type: WidthType.DXA },
    shading: { fill: i === 0 ? "FBF1E6" : "FFFFFF", type: ShadingType.CLEAR },
    margins: { top: 60, bottom: 60, left: 120, right: 120 },
    children: [new Paragraph({ children: [new TextRun({ text: t, bold: i === 0, size: 20 })] })]
  })) })));
  return new Table({ width: { size: CONTENT_W, type: WidthType.DXA }, columnWidths: [w1, w2], rows: trs });
}

// read a code excerpt file content (we inline strings instead)
function spacer(n = 1) { const a = []; for (let i = 0; i < n; i++) a.push(new Paragraph({ children: [] })); return a; }

async function main() {
  await loadDims();
  const children = [];

  /* ===================== COVER ===================== */
  children.push(new Paragraph({ spacing: { before: 1200 }, children: [] }));
  children.push(new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { after: 40 },
    children: [new TextRun({ text: "COURSE DESIGN REPORT", bold: true, size: 28, color: YELLOW })]
  }));
  children.push(new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { after: 200 },
    children: [new TextRun({ text: "J2EE Programming", size: 24, color: GREY })]
  }));
  children.push(new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { before: 200, after: 0 },
    children: [new TextRun({ text: "E-Canteen", bold: true, size: 64, color: RED })]
  }));
  children.push(new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { before: 60, after: 40 },
    children: [new TextRun({ text: "A Campus Canteen Pre-Order Web Application", bold: true, size: 30, color: INK })]
  }));
  children.push(new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { after: 500 },
    children: [new TextRun({ text: "Built with J2EE · Spring Boot · MyBatis · MySQL", size: 22, color: GREY })]
  }));
  // thin rule
  children.push(new Paragraph({
    border: { bottom: { style: BorderStyle.SINGLE, size: 10, color: YELLOW, space: 1 } },
    spacing: { after: 300 }, children: []
  }));
  const coverRows = [
    ["Name", "Muhammad Rizki Eka Pratama"],
    ["Student ID", "L25020010"],
    ["Course", "J2EE Programming"],
    ["Major", "Software Engineering"],
    ["Class", "Class 1"],
    ["University", "Nanjing Xiaozhuang University"],
  ];
  children.push(infoTable(coverRows));
  children.push(new Paragraph({ children: [new PageBreak()] }));

  /* ===================== TOC ===================== */
  children.push(new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("Table of Contents")] }));
  children.push(new TableOfContents("Table of Contents", { hyperlink: true, headingStyleRange: "1-2" }));
  children.push(new Paragraph({ children: [new PageBreak()] }));

  /* ===================== 1. INTRODUCTION ===================== */
  children.push(h1("1. Introduction"));
  children.push(h2("1.1 Background"));
  children.push(p("Every day around lunch time the campus canteen turns into the busiest place on campus. Students rush in between classes, the queues in front of each stall get long, and a lot of time is wasted just standing and waiting. By the time you finally reach the counter, you sometimes find out that the dish you wanted is already sold out. This is the small but very real problem that gave me the idea for this project."));
  children.push(p("E-Canteen is a web application that lets students order and pay for their food before they even arrive at the canteen. The idea is simple: skip the line, order ahead. A student opens the website, browses the stalls, adds food to a cart, checks out and chooses a payment method, and then just walks to the stall to pick up the order when it is ready. On the other side, the stall owners (vendors) get a clean dashboard where they can manage their menu, see incoming orders and move each order through its preparation steps."));
  children.push(p("I chose this topic because it is close to my daily life, but it is also a good fit for what we learned in the J2EE Programming course. It naturally needs user login and sessions, different user roles, database access through MyBatis, server-side validation, and dynamic pages rendered with Thymeleaf. In other words, it touches almost every topic from the course in one connected system instead of small, separate exercises."));

  children.push(h2("1.2 Objectives"));
  children.push(p("The main goals I set for this project were:"));
  children.push(num("Build a complete web application using the layered style taught in class: Controller, Service, Mapper (interface plus XML) and the MySQL database."));
  children.push(num("Support more than one type of user (student, vendor, admin and superadmin) and make sure each one only sees the pages they are allowed to see."));
  children.push(num("Use MyBatis for all database access, keeping the SQL in XML mapper files instead of writing JDBC by hand."));
  children.push(num("Handle real business logic such as a shopping cart, stock checking, and a discount calculation that combines a per-stall discount with a campus-wide promotion."));
  children.push(num("Keep the code simple and readable, without any heavy design patterns, so that the structure stays close to the examples from the lectures."));

  children.push(h2("1.3 Scope of the System"));
  children.push(p("The system covers the full ordering journey for a student and the full management side for the people who run the canteen. A student can register, log in, browse menus by stall, search for a dish, add items to a cart, check out, choose a payment method, track the live status of an order, cancel an order while it is still pending, re-order a previous order, and leave a rating after the food is ready. A vendor can apply to open a stall and, once approved, becomes an admin who manages the menu, stock, orders, stall discount and sales report for that single stall. A superadmin sits above everyone and approves vendors, manages users and stalls, sets the global discount, watches all transactions and reads the activity log."));

  children.push(h2("1.4 Technology Stack"));
  children.push(infoTable([
    ["Language", "Java 17"],
    ["Framework", "Spring Boot 3.5.x (Spring MVC)"],
    ["Persistence", "MyBatis (mybatis-spring-boot-starter), SQL kept in XML"],
    ["Database", "MySQL (database name: ecanteen)"],
    ["View layer", "Thymeleaf templates + Bootstrap / custom CSS"],
    ["Security", "BCrypt password hashing + HttpSession + a login interceptor"],
    ["Build tool", "Maven"],
    ["Server", "Embedded Tomcat, port 8080"],
  ], ["Item", "Choice"]));
  children.push(new Paragraph({ spacing: { after: 60 }, children: [] }));
  children.push(p("I deliberately used spring-security-crypto only for the BCrypt encoder, not the full Spring Security framework. Login, sessions and access control are written by hand with an interceptor, because that is much closer to what we practised in the course and it is easier to explain line by line."));

  /* ===================== 2. UML ===================== */
  children.push(h1("2. System Modelling (UML)"));
  children.push(p("Before writing the code I drew a few diagrams to understand what the system has to do and how the pieces fit together. This section shows the use case diagram, the entity relationship diagram of the database, a class diagram of the most important classes, and a sequence diagram for the most important action in the whole system: placing an order. I also added an overview of the layered architecture, because that layering is the backbone of the whole project."));

  children.push(h2("2.1 Layered Architecture Overview"));
  children.push(p("The application follows a strict four-layer flow. A request from the browser hits a Controller, the Controller calls a Service, the Service calls one or more Mappers, and the Mapper runs SQL against MySQL. The answer then travels back up the same path. Each layer only talks to the layer directly below it, which keeps responsibilities clear and makes the code easy to follow."));
  fig("d_architecture.png", "Figure 2.1 — The four-layer architecture used across the whole application.", 520).forEach(x => children.push(x));

  children.push(h2("2.2 Use Case Diagram"));
  children.push(p("The use case diagram shows the four actors and what each of them is allowed to do. The student is the main customer of the system. The vendor is a special account that has registered but is still waiting for approval, so the only thing it can do is apply to open a stall. Once approved, that account becomes an admin and gains all the stall-management use cases. The superadmin controls the platform: approving vendors, managing users and stalls, setting the global discount, and reading the global reports and logs."));
  fig("d_usecase.png", "Figure 2.2 — Use case diagram with the four actors (student, vendor, admin, superadmin).", 560).forEach(x => children.push(x));

  children.push(h2("2.3 Entity Relationship Diagram"));
  children.push(p("The database is the heart of the system, so the ER diagram is probably the most useful diagram here. The users table stores every account regardless of role, with a role column and an optional stall_id that links an approved admin to the stall they own. Each stall has many menu_items. A cart_items row links a user to a menu item with a quantity. When the student checks out, the cart turns into one orders row plus several order_items rows; the order items keep a frozen copy of the name and price so that later menu changes do not rewrite history. Every status change of an order is written to order_status_logs for auditing. The extra tables reviews, vendor_applications and discounts support the rating feature, the vendor approval flow, and the global promotion."));
  fig("d_er.png", "Figure 2.3 — Entity relationship diagram of the \"ecanteen\" database.", 580).forEach(x => children.push(x));
  children.push(p("A few design choices are worth pointing out. First, money columns (subtotal, discount_total, total_amount, unit_price, price) are stored as decimals, never as floating point, so the totals are always exact. Second, the order_items table copies item_name and unit_price at the moment of purchase. If a vendor later renames a dish or changes its price, the old receipts still show what the student actually paid. This is a small thing but it makes the order history trustworthy."));

  children.push(h2("2.4 Class Diagram"));
  children.push(p("The class diagram groups the classes by layer and shows how they depend on each other. Controllers depend on services, services depend on mapper interfaces, and the mapper interfaces are backed by XML files that MyBatis turns into real objects at run time. The entity classes (User, Order, and so on) are plain Java classes with fields, a constructor and getters and setters. There is no inheritance tree and no abstract factory anywhere; the relationships are just simple \"uses\" arrows, which matches the no-pattern rule I set for myself."));
  fig("d_class.png", "Figure 2.4 — Class diagram of the key controllers, services, mappers and entities.", 580).forEach(x => children.push(x));

  children.push(h2("2.5 Sequence Diagram — Placing an Order"));
  children.push(p("Placing an order is the action that touches the most layers, so it is the best one to show as a sequence diagram. When the student presses \"Place Order\", the OrderController receives the POST request and calls OrderService.checkout(). The service reads the cart and the active discount through the mappers, computes the totals with BigDecimal arithmetic, then inserts the order header, inserts each order line, and decreases the stock of each item. Finally it clears the cart and returns the new order id, and the controller redirects the student to the confirmation page."));
  fig("d_sequence.png", "Figure 2.5 — Sequence diagram for a student placing an order.", 580).forEach(x => children.push(x));

  /* ===================== 3. WEBSITE FLOW ===================== */
  children.push(h1("3. Website Flow"));
  children.push(p("This section describes how a user moves through the website. Everything starts at the login and register page. After a successful login, the system reads the user's role from the session and sends them to the correct home page. Because the four roles use very different parts of the site, the flow splits into four branches right after login."));
  fig("d_flow.png", "Figure 3.1 — Role-based navigation flow of the website.", 580).forEach(x => children.push(x));

  children.push(h2("3.1 Student Flow"));
  children.push(p("A student lands on the menu page. From there the natural path is: browse the stalls and dishes, search or filter if needed, add items to the cart, open the cart to review quantities, go to checkout, choose a payment method and place the order. After the order is placed the student sees a confirmation page and can then track the order status or, later, open the order history to re-order or leave a review."));
  children.push(h2("3.2 Vendor Flow"));
  children.push(p("A vendor account is created through a separate \"register as vendor\" page. After logging in, the vendor cannot order food and cannot manage anything yet. The only available action is to fill in the \"apply to open a stall\" form. After submitting, the vendor simply waits; the page shows an \"awaiting approval\" message until the superadmin makes a decision."));
  children.push(h2("3.3 Admin (Approved Vendor) Flow"));
  children.push(p("When the superadmin approves the application, the system creates the stall and upgrades the vendor account to the admin role with that stall attached. The next time the user logs in, they are sent to the admin dashboard, where they can manage their menu and stock, process incoming orders, set their stall discount, look at their sales report and read customer reviews for their dishes."));
  children.push(h2("3.4 Superadmin Flow"));
  children.push(p("The superadmin logs in and lands on the control center. From there they reach all the platform tools: vendor approvals, user management, stall management, global and per-stall discount settings, a list of every transaction, the canteen-wide sales report, review moderation and the activity log."));

  /* ===================== 4. SYSTEM DESIGN ===================== */
  children.push(h1("4. System Design"));
  children.push(p("This section explains how the system is designed on the inside: the package structure, the responsibility of each layer, the database schema, the security design, and the design of the most important features. The guiding rule throughout was to keep the design simple and explicit. I avoided design patterns on purpose so that any classmate reading the code can follow it from the controller all the way down to the SQL without learning new abstractions."));

  children.push(h2("4.1 Package Structure"));
  children.push(p("The Java code lives under the base package com.canteen and is split into folders by layer. Keeping one clear folder per responsibility makes the project easy to navigate."));
  children.push(infoTable([
    ["entity", "Plain Java classes that mirror the database tables (User, Stall, MenuItem, CartItem, Order, OrderItem, Review, ...)."],
    ["mapper", "MyBatis mapper interfaces; the matching SQL lives in resources/mapper/*.xml."],
    ["service", "Business logic such as login, cart handling, checkout and reports."],
    ["controller", "Spring MVC controllers that handle HTTP requests and return view names."],
    ["interceptor", "LoginInterceptor — checks login and role before protected pages."],
    ["config", "WebConfig (interceptor + upload folder) and PasswordConfig (BCrypt bean)."],
  ], ["Package", "Responsibility"]));
  children.push(new Paragraph({ spacing: { after: 60 }, children: [] }));

  children.push(h2("4.2 The Four Layers in Detail"));
  children.push(pmix([{ t: "Controller layer. ", b: true }, "Each controller is a normal class annotated with @Controller. Its job is only to read the request (parameters, the logged-in user from the session), call a service method, put the result into the model, and return the name of a Thymeleaf template. Controllers never contain SQL and never do calculations; they delegate everything to services."]));
  children.push(pmix([{ t: "Service layer. ", b: true }, "Services hold the real logic. For example, UserService hashes passwords and checks logins, CartService validates that an item is orderable before adding it, and OrderService computes discounts and writes the order. A service may call several mappers to get its job done. Services are plain @Service classes with constructor injection — no interface plus implementation split, because the project does not need swappable implementations."]));
  children.push(pmix([{ t: "Mapper layer. ", b: true }, "Each mapper is an interface annotated with @Mapper. There is no hand-written implementation class; MyBatis generates it at run time from the XML file whose namespace equals the interface name. Every method maps to one SQL statement. This keeps the SQL in one place and out of the Java code."]));
  children.push(pmix([{ t: "Database layer. ", b: true }, "MySQL stores the data. The setting map-underscore-to-camel-case lets the database use snake_case column names (password_hash) while the Java fields use camelCase (passwordHash), so I do not have to write a result mapping for every simple query."]));

  children.push(h2("4.3 Database Design"));
  children.push(p("The schema has seven core tables plus a few extra tables for the advanced features. The core tables are users, stalls, menu_items, cart_items, orders, order_items and discounts. The extra tables are vendor_applications (the approval queue), reviews (ratings), order_status_logs (the audit trail) and the security/activity log."));
  children.push(infoTable([
    ["users", "id, username, password_hash, email, role, stall_id, is_active"],
    ["stalls", "id, name, description, is_active, is_open, discount_percent"],
    ["menu_items", "id, stall_id, name, description, price, image_url, is_available, stock"],
    ["cart_items", "id, user_id, menu_item_id, quantity"],
    ["orders", "id, user_id, subtotal, discount_total, total_amount, payment_method, status, created_at"],
    ["order_items", "id, order_id, menu_item_id, item_name, unit_price, quantity"],
    ["order_status_logs", "id, order_id, status, changed_at"],
    ["discounts", "id, name, discount_percent, is_active"],
  ], ["Table", "Main columns"]));
  children.push(new Paragraph({ spacing: { after: 60 }, children: [] }));
  children.push(p("The discount design is intentionally split in two places. A per-stall discount lives on the stall row (discount_percent), and a single global promotion lives as the active row in the discounts table. This lets a vendor run a sale on their own stall while the superadmin runs a campus-wide promotion at the same time, and the checkout simply applies both."));

  children.push(h2("4.4 Security and Access Control Design"));
  children.push(p("Security in this project has three parts. First, passwords are never stored in plain text; they are hashed with BCrypt before going into the database, and login checks the typed password against the hash. Second, the logged-in user is kept in the HttpSession under the key loginUser, so the server remembers who you are between requests. Third, a LoginInterceptor runs before every protected page; it checks that you are logged in and that your role is allowed to enter that URL area, redirecting you to your own home page otherwise."));
  children.push(p("One detail I am proud of is that the interceptor re-reads the account from the database on every protected request. This means that if the superadmin suspends an account, or approves a vendor into an admin, the change takes effect immediately on the user's next click, without waiting for the session to expire."));

  children.push(h2("4.5 Design of the Key Features"));
  children.push(h3("4.5.1 Cart and Stock"));
  children.push(p("The cart is stored in the database, one row per user and menu item. Adding the same item again just increases the quantity instead of creating a duplicate row. Validation happens on the server: even if a button is hidden in the UI, the service still refuses to add an item that is unavailable, out of stock, or that belongs to a closed stall. Stock is reduced at checkout, and if it reaches zero the item is automatically marked as unavailable so it shows up as \"Sold Out\"."));
  children.push(h3("4.5.2 Discount Calculation"));
  children.push(p("The total is computed in two steps with BigDecimal. First the per-stall discount is applied to each line, then the global promotion is applied to the amount that remains. All values are rounded to two decimal places using HALF_UP rounding. The checkout page previews this calculation before the student confirms, so there are no surprises."));
  children.push(h3("4.5.3 Order Status Lifecycle"));
  children.push(p("An order moves through a fixed set of statuses: PENDING, then PROCESSING, then READY, then PICKED_UP. The vendor advances the order one step at a time. Instead of a State pattern, this is just a small switch statement that returns the next status. Every change is also written to the order_status_logs table, and the student sees these timestamps as a status timeline on the order detail page."));
  children.push(h3("4.5.4 Vendor Approval Workflow"));
  children.push(p("This is the feature that connects all four roles. A vendor submits an application, which is saved as PENDING. The superadmin sees it in the approval queue and can approve or reject it (with an optional reason). On approval, the system creates a new stall, promotes the vendor account to admin and attaches the new stall to it. From that point the user manages a real stall."));

  /* ===================== 5. IMPLEMENTATION ===================== */
  children.push(h1("5. Implementation and Demonstration"));
  children.push(p("This is the longest section. It walks through the real code of the project, file by file and feature by feature, and pairs each part with screenshots from the running application so it is clear what the code actually produces on screen. All the code shown here is taken directly from my project."));

  // 5.1 config
  children.push(h2("5.1 Project Configuration"));
  children.push(p("The whole project is configured through two files: pom.xml for the libraries and application.properties for the runtime settings. The important part of application.properties is the database connection and the three MyBatis settings."));
  codeBlock(
`server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/ecanteen?useSSL=false&serverTimezone=Asia/Jakarta
spring.datasource.username=root
spring.datasource.password=********
mybatis.mapper-locations=classpath:mapper/*.xml
mybatis.type-aliases-package=com.canteen.entity
mybatis.configuration.map-underscore-to-camel-case=true
spring.servlet.multipart.max-file-size=5MB`,
    "Listing 5.1 — application.properties (database + MyBatis settings)."
  ).forEach(x => children.push(x));
  children.push(p("The mapper-locations line tells MyBatis where to find the SQL XML files. type-aliases-package lets me write resultType=\"User\" in the XML instead of the full class name. And map-underscore-to-camel-case is what links snake_case columns to camelCase fields automatically."));
  children.push(p("BCrypt is registered once as a Spring bean so it can be injected anywhere it is needed:"));
  codeBlock(
`@Configuration
public class PasswordConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}`,
    "Listing 5.2 — PasswordConfig.java"
  ).forEach(x => children.push(x));

  // 5.2 auth + login screenshot
  children.push(h2("5.2 Authentication: Register, Login and Sessions"));
  children.push(p("The login page is the entry point of the system. It has the sign-in form on the right and the campaign-style \"Skip the line, order ahead\" banner on the left."));
  fig("s01.png", "Figure 5.1 — The login page (student/vendor sign in).", 560).forEach(x => children.push(x));
  children.push(p("Registration is handled by UserService. Before creating an account it checks that the username is free, then hashes the password with BCrypt and stores the new user with the role \"student\". The vendor registration method is almost identical but stores the role \"vendor\" instead."));
  codeBlock(
`public boolean register(String username, String email, String rawPassword) {
    User existing = userMapper.findByUsername(username);
    if (existing != null) {
        return false;                       // username already taken
    }
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(rawPassword));   // BCrypt hash
    user.setRole("student");
    userMapper.insertUser(user);
    return true;
}`,
    "Listing 5.3 — UserService.register(): hashing the password before saving."
  ).forEach(x => children.push(x));
  children.push(p("Login does the reverse. It loads the user by username and uses passwordEncoder.matches() to compare the typed password with the stored hash. It returns the User on success or null on failure, so the controller can show an error message."));
  codeBlock(
`public User login(String username, String rawPassword) {
    User user = userMapper.findByUsername(username);
    if (user == null) {
        return null;
    }
    if (passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
        return user;
    }
    return null;
}`,
    "Listing 5.4 — UserService.login(): checking the password against the BCrypt hash."
  ).forEach(x => children.push(x));
  children.push(p("The AuthController ties this to the web. On a successful login it records the user in the session and then sends each role to its own home page. It also writes a small entry to the activity log and supports an optional \"remember username\" cookie."));
  codeBlock(
`@PostMapping("/login")
public String login(@RequestParam String username,
                    @RequestParam String password,
                    HttpSession session, Model model) {
    User user = userService.login(username, password);
    if (user == null) {
        logService.record(username, "LOGIN_FAILURE", "wrong username/password");
        model.addAttribute("error", "Incorrect username or password.");
        return "login";
    }
    session.setAttribute("loginUser", user);              // remember who is logged in
    String role = user.getRole();
    if ("superadmin".equals(role)) return "redirect:/superadmin";
    if ("admin".equals(role))      return "redirect:/admin";
    if ("vendor".equals(role))     return "redirect:/vendor";
    return "redirect:/menu";                              // student
}`,
    "Listing 5.5 — AuthController.login(): saving the session and routing by role."
  ).forEach(x => children.push(x));

  // 5.3 mapper pattern
  children.push(h2("5.3 The MyBatis Mapper Pattern"));
  children.push(p("Every database operation follows the same two-file pattern: a Java interface that declares the method, and an XML element with the same id that holds the SQL. There is no implementation class — MyBatis builds it for me. Here is the UserMapper interface (trimmed):"));
  codeBlock(
`@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    int insertUser(User user);
    User findById(@Param("id") Integer id);
    int updatePassword(@Param("id") Integer id,
                       @Param("passwordHash") String passwordHash);
}`,
    "Listing 5.6 — UserMapper.java (interface only — no implementation)."
  ).forEach(x => children.push(x));
  children.push(p("And the matching XML. The namespace is the full interface name and each statement id matches a method name. Notice that user input is always bound with #{...}, which MyBatis turns into a safe prepared-statement parameter — this is what protects the app from SQL injection."));
  codeBlock(
`<mapper namespace="com.canteen.mapper.UserMapper">

    <select id="findByUsername" resultType="User">
        SELECT id, username, password_hash, email, role, stall_id, is_active
        FROM users
        WHERE username = #{username}
    </select>

    <insert id="insertUser" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (username, password_hash, email, role)
        VALUES (#{username}, #{passwordHash}, #{email}, IFNULL(#{role}, 'student'))
    </insert>

</mapper>`,
    "Listing 5.7 — UserMapper.xml: SQL bound safely with #{...}."
  ).forEach(x => children.push(x));

  // 5.4 interceptor
  children.push(h2("5.4 Access Control with an Interceptor"));
  children.push(p("Instead of repeating a login check in every controller, I wrote one LoginInterceptor and registered it for all protected URLs in WebConfig. The interceptor runs before the controller. It checks the session, re-reads the account from the database (so suspensions and role changes apply immediately), and then makes sure the role is allowed to enter that URL area."));
  codeBlock(
`public boolean preHandle(HttpServletRequest request,
                         HttpServletResponse response, Object handler) {
    HttpSession session = request.getSession(false);
    User user = (session != null) ? (User) session.getAttribute("loginUser") : null;
    if (user == null) {
        response.sendRedirect("/login");           // not logged in
        return false;
    }
    // re-check the account every request (suspend / role change takes effect now)
    User fresh = userMapper.findById(user.getId());
    if (fresh == null || Boolean.FALSE.equals(fresh.getIsActive())) {
        session.invalidate();
        response.sendRedirect("/login?suspended=1");
        return false;
    }
    String uri = request.getRequestURI();
    String role = user.getRole();
    if (uri.startsWith("/superadmin") && !"superadmin".equals(role)) {
        response.sendRedirect(getHomePage(role));  // wrong area -> own home
        return false;
    }
    // ... same idea for /admin, /vendor and the student pages
    return true;
}`,
    "Listing 5.8 — LoginInterceptor.preHandle(): login + role check in one place."
  ).forEach(x => children.push(x));
  codeBlock(
`@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginInterceptor)
            .addPathPatterns("/menu", "/menu/**", "/cart/**", "/order/**",
                    "/review/**", "/profile", "/profile/**",
                    "/vendor", "/vendor/**", "/admin/**", "/superadmin/**");
}`,
    "Listing 5.9 — WebConfig: registering the interceptor for protected URLs."
  ).forEach(x => children.push(x));

  // 5.5 student browse + menu
  children.push(h2("5.5 Browsing the Menu and the Cart"));
  children.push(p("After logging in, a student sees the menu page grouped by stall, with each dish shown as a card. Items that are sold out are clearly marked and cannot be added."));
  fig("s03.png", "Figure 5.2 — The student menu page, grouped by stall.", 560).forEach(x => children.push(x));
  children.push(p("Adding to the cart goes through CartService. The service validates everything on the server before touching the database: the item must exist and be orderable, and the stall must be open. If the item is already in the cart, the quantity is increased instead of adding a duplicate row."));
  codeBlock(
`public String addToCart(Integer userId, Integer menuItemId) {
    MenuItem item = menuMapper.findItemById(menuItemId);
    if (item == null || !item.isOrderable()) {
        return "This item is unavailable or out of stock.";
    }
    Stall stall = menuMapper.findStallById(item.getStallId());
    if (stall == null || !Boolean.TRUE.equals(stall.getIsOpen())) {
        return "The stall is currently closed, you can't order yet.";
    }
    CartItem existing = cartMapper.findByUserAndItem(userId, menuItemId);
    if (existing != null) {
        cartMapper.updateQuantity(existing.getId(), existing.getQuantity() + 1, userId);
    } else {
        CartItem c = new CartItem();
        c.setUserId(userId);
        c.setMenuItemId(menuItemId);
        c.setQuantity(1);
        cartMapper.insertCartItem(c);
    }
    return null;   // null means success
}`,
    "Listing 5.10 — CartService.addToCart(): server-side validation before insert."
  ).forEach(x => children.push(x));
  children.push(p("The cart page lists the chosen items, lets the student change quantities or remove a line, and shows the subtotal. The note reminds the student that discounts are applied at checkout."));
  fig("s04.png", "Figure 5.3 — The shopping cart page.", 560).forEach(x => children.push(x));
  children.push(p("Reading the cart needs data from three tables (the cart row, the menu item and the stall), so this mapper uses a resultMap and a JOIN. The isOrderable() helper on the MenuItem entity, used above, is a tiny piece of logic that keeps the rule for \"can this be ordered\" in one place:"));
  codeBlock(
`// MenuItem.java — orderable only if available AND still in stock
public boolean isOrderable() {
    return Boolean.TRUE.equals(isAvailable) && stock != null && stock > 0;
}`,
    "Listing 5.11 — MenuItem.isOrderable(): the rule for an orderable item."
  ).forEach(x => children.push(x));

  // 5.6 checkout + discount (the highlight)
  children.push(h2("5.6 Checkout: The Discount Engine"));
  children.push(p("Checkout is the most interesting part of the project. The checkout page shows an order summary, a clear price breakdown (subtotal, stall discount, admin promo and final total) and a list of payment methods."));
  fig("s08.png", "Figure 5.4 — Checkout page: price breakdown and \"Place Order\".", 540).forEach(x => children.push(x));
  fig("s07.png", "Figure 5.5 — Checkout page: choosing a payment method.", 540).forEach(x => children.push(x));
  children.push(p("The calculation lives in OrderService and is done entirely with BigDecimal so the money is always exact. Step one adds up each line and the per-stall discount. Step two applies the global promotion to whatever is left after the stall discount. Everything is rounded to two decimals."));
  codeBlock(
`// 1) subtotal and per-stall discount, line by line
BigDecimal subtotal = BigDecimal.ZERO;
BigDecimal stallDiscount = BigDecimal.ZERO;
for (CartView row : rows) {
    BigDecimal lineTotal = row.getUnitPrice()
            .multiply(BigDecimal.valueOf(row.getQuantity()));
    subtotal = subtotal.add(lineTotal);

    BigDecimal stallPercent = row.getStallDiscountPercent();
    BigDecimal lineStallDiscount = lineTotal.multiply(stallPercent)
            .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    stallDiscount = stallDiscount.add(lineStallDiscount);
}

// 2) global admin discount on the amount AFTER the stall discount
BigDecimal afterStall = subtotal.subtract(stallDiscount);
BigDecimal adminPercent = BigDecimal.ZERO;
Discount active = discountMapper.findActiveDiscount();
if (active != null) {
    adminPercent = active.getDiscountPercent();
}
BigDecimal adminDiscount = afterStall.multiply(adminPercent)
        .divide(HUNDRED, 2, RoundingMode.HALF_UP);

BigDecimal discountTotal = stallDiscount.add(adminDiscount);
BigDecimal total = subtotal.subtract(discountTotal);`,
    "Listing 5.12 — OrderService.checkout(): the two-step discount calculation."
  ).forEach(x => children.push(x));
  children.push(p("In the screenshots the subtotal is 20.00, the 5% stall discount removes 1.00, and the 10% campus promo then removes 1.90 from the remaining 19.00, giving a final total of 17.10. The fact that the promo is calculated on 19.00 and not on the original 20.00 is exactly what the two-step design produces."));
  children.push(p("After the totals are ready, the same method saves the order header, saves each line with a frozen name and price, reduces the stock, writes the first status log and clears the cart:"));
  codeBlock(
`// save order header
Order order = new Order();
order.setUserId(userId);
order.setSubtotal(subtotal);
order.setDiscountTotal(discountTotal);
order.setTotalAmount(total);
order.setPaymentMethod(paymentMethod);
order.setStatus("PENDING");
orderMapper.insertOrder(order);

// save each line (frozen price) and decrease stock
for (CartView row : rows) {
    OrderItem item = new OrderItem();
    item.setOrderId(order.getId());
    item.setMenuItemId(row.getMenuItemId());
    item.setItemName(row.getItemName());
    item.setUnitPrice(row.getUnitPrice());
    item.setQuantity(row.getQuantity());
    orderMapper.insertOrderItem(item);
    menuMapper.decreaseStock(row.getMenuItemId(), row.getQuantity());
}
addStatusLog(order.getId(), "PENDING");
cartService.clearCart(userId);`,
    "Listing 5.13 — OrderService.checkout(): saving the order and updating stock."
  ).forEach(x => children.push(x));
  children.push(p("The decreaseStock SQL is also clever in a small way: it only subtracts when there is enough stock, and it flips the item to unavailable the moment the stock hits zero, so a sold-out dish disappears from ordering automatically."));
  codeBlock(
`<update id="decreaseStock">
    UPDATE menu_items
    SET stock = stock - #{qty},
        is_available = CASE WHEN (stock - #{qty}) &lt;= 0
                            THEN FALSE ELSE is_available END
    WHERE id = #{id} AND stock >= #{qty}
</update>`,
    "Listing 5.14 — MenuMapper.xml: stock decrease with auto sold-out."
  ).forEach(x => children.push(x));
  children.push(p("Once the order is placed the student sees a confirmation page that repeats the receipt and explains what happens next."));
  fig("s09.png", "Figure 5.6 — Order placed confirmation.", 520).forEach(x => children.push(x));

  // 5.7 order tracking
  children.push(h2("5.7 Order Tracking and History"));
  children.push(p("The order detail page shows a live status timeline built from the order_status_logs table, the items ordered and the payment summary. Each time the vendor advances the order, a new entry with a timestamp appears here."));
  fig("s10.png", "Figure 5.7 — Order detail with the status timeline.", 520).forEach(x => children.push(x));
  children.push(p("The order history page lists all of the student's past orders with filter buttons (All, Pending, Processing, Ready, Picked Up, Cancelled). Pending orders can be cancelled, and completed ones can be re-ordered with one click."));
  fig("s05.png", "Figure 5.8 — Order history with status filters.", 560).forEach(x => children.push(x));
  children.push(p("Advancing the status uses a plain switch statement instead of a State pattern. It is easy to read and easy to change:"));
  codeBlock(
`public void advanceStatus(Integer orderId) {
    Order order = orderMapper.findOrderById(orderId);
    if (order == null) return;
    String current = order.getStatus();
    String next;
    switch (current) {
        case "PENDING":    next = "PROCESSING"; break;
        case "PROCESSING": next = "READY";      break;
        case "READY":      next = "PICKED_UP";  break;
        default:           next = current;      break;   // already finished
    }
    if (!next.equals(current)) {
        orderMapper.updateStatus(orderId, next);
        addStatusLog(orderId, next);                      // write the audit row
    }
}`,
    "Listing 5.15 — OrderService.advanceStatus(): the order lifecycle."
  ).forEach(x => children.push(x));
  children.push(p("The order history query also shows how MyBatis builds dynamic SQL. The same statement serves \"all orders\" and \"orders with a chosen status\" thanks to an <if> block:"));
  codeBlock(
`<select id="findOrdersByUserFiltered" resultType="Order">
    SELECT id, user_id, subtotal, discount_total, total_amount,
           payment_method, status, created_at
    FROM orders
    WHERE user_id = #{userId}
    <if test="status != null and status != ''">
        AND status = #{status}
    </if>
    ORDER BY id DESC
</select>`,
    "Listing 5.16 — Dynamic SQL: an optional status filter with <if>."
  ).forEach(x => children.push(x));

  // 5.8 profile
  children.push(h2("5.8 Profile and Change Password"));
  children.push(p("Every role shares one profile page where the user can see their account info and change their own password. The service checks the old password first, then stores the BCrypt hash of the new one."));
  fig("s06.png", "Figure 5.9 — The profile page with change-password form.", 540).forEach(x => children.push(x));

  // 5.9 vendor application
  children.push(h2("5.9 The Vendor Application Flow"));
  children.push(p("A vendor registers through a dedicated page and then logs in to the vendor portal, where the only available action is the \"apply to open a stall\" form."));
  fig("s02.png", "Figure 5.10 — The \"open your stall on campus\" vendor sign-up.", 540).forEach(x => children.push(x));
  fig("s30.png", "Figure 5.11 — Vendor portal: the apply-to-open-a-stall form.", 520).forEach(x => children.push(x));
  fig("s31.png", "Figure 5.12 — Filling in the stall application.", 520).forEach(x => children.push(x));
  children.push(p("After submitting, the vendor sees a friendly \"awaiting approval\" message. The service refuses to submit a second application while one is still pending."));
  fig("s32.png", "Figure 5.13 — Application submitted, waiting for approval.", 520).forEach(x => children.push(x));
  codeBlock(
`public String apply(Integer userId, String stallName, String stallDescription,
                    String ownerName, String phone) {
    VendorApplication existing = vendorMapper.findLatestByUser(userId);
    if (existing != null && "PENDING".equals(existing.getStatus())) {
        return "Your application is already in the queue; please wait.";
    }
    VendorApplication app = new VendorApplication();
    app.setUserId(userId);
    app.setStallName(stallName);
    app.setStallDescription(stallDescription);
    app.setOwnerName(ownerName);
    app.setPhone(phone);
    vendorMapper.insertApplication(app);     // saved as PENDING
    return null;
}`,
    "Listing 5.17 — VendorService.apply(): submitting a stall application."
  ).forEach(x => children.push(x));

  // 5.10 admin dashboard + management
  children.push(h2("5.10 The Vendor / Admin Dashboard"));
  children.push(p("Once approved, the vendor becomes an admin and lands on a dashboard with live numbers: pending orders, today's revenue, a seven-day revenue chart and a doughnut chart of orders by status."));
  fig("s11.png", "Figure 5.14 — Admin dashboard with revenue and status charts.", 560).forEach(x => children.push(x));
  fig("s12.png", "Figure 5.15 — Dashboard actions: manage menu, orders, discount, reports.", 540).forEach(x => children.push(x));
  children.push(p("The menu manager lets the admin add a dish, edit its details, upload a photo, toggle availability, delete it, or quickly add stock."));
  fig("s13.png", "Figure 5.16 — Managing menu items (with image upload and stock).", 560).forEach(x => children.push(x));
  children.push(p("Uploaded images are stored in an uploads/ folder on disk and served back through a resource handler, so the photo URL is just /uploads/filename:"));
  codeBlock(
`@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:uploads/");
}`,
    "Listing 5.18 — WebConfig: serving uploaded menu photos."
  ).forEach(x => children.push(x));
  children.push(p("The order manager lists the stall's incoming orders with the same status filters and an \"Advance\" button that pushes an order to its next status. A vendor can only advance orders that actually contain their own stall's items — the service checks ownership before doing anything."));
  fig("s14.png", "Figure 5.17 — Managing incoming orders and advancing status.", 560).forEach(x => children.push(x));
  codeBlock(
`// a vendor may only advance orders that contain their own stall's menu
public void advanceStatusForStall(Integer orderId, Integer stallId) {
    if (orderMapper.countOrderInStall(orderId, stallId) == 0) {
        return;                         // not this stall's order -> ignore
    }
    advanceStatus(orderId);
}`,
    "Listing 5.19 — OrderService: ownership check before advancing an order."
  ).forEach(x => children.push(x));
  children.push(p("The admin can also run a sale on the whole stall, look at a quick sales report, and read the reviews customers left for their dishes."));
  fig("s15.png", "Figure 5.18 — Setting a stall-wide discount.", 500).forEach(x => children.push(x));
  fig("s16.png", "Figure 5.19 — The stall sales report.", 500).forEach(x => children.push(x));
  fig("s17.png", "Figure 5.20 — Customer reviews for this stall.", 500).forEach(x => children.push(x));

  // 5.11 superadmin
  children.push(h2("5.11 The Superadmin Control Center"));
  children.push(p("The superadmin has the widest view of the system. The control center shows platform-wide statistics and a revenue-by-stall chart, with shortcut tiles to every management tool."));
  fig("s18.png", "Figure 5.21 — Superadmin control center overview.", 520).forEach(x => children.push(x));
  fig("s19.png", "Figure 5.22 — Superadmin operations and shortcut tiles.", 520).forEach(x => children.push(x));
  children.push(p("The most important superadmin tool is vendor approval. When there are no pending applications the page shows an \"all clear\" state; when an application arrives, it appears in a table with Approve and Reject buttons (the reject can include a reason)."));
  fig("s20.png", "Figure 5.23 — Vendor approvals: empty \"all clear\" state.", 540).forEach(x => children.push(x));
  fig("s33.png", "Figure 5.24 — A pending vendor application ready to approve or reject.", 560).forEach(x => children.push(x));
  children.push(p("Approval is where the four roles connect. The service creates the stall, promotes the account to admin and attaches the stall in one step:"));
  codeBlock(
`public void approve(Integer applicationId) {
    VendorApplication app = vendorMapper.findById(applicationId);
    if (app == null || !"PENDING".equals(app.getStatus())) return;

    Stall stall = new Stall();
    stall.setName(app.getStallName());
    stall.setDescription(app.getStallDescription());
    stall.setIsActive(true);
    stall.setDiscountPercent(BigDecimal.ZERO);
    menuMapper.insertStall(stall);                       // 1) create the stall

    userMapper.updateRoleAndStall(app.getUserId(), "admin", stall.getId()); // 2) promote
    vendorMapper.updateStatus(applicationId, "APPROVED");                    // 3) close app
}`,
    "Listing 5.20 — VendorService.approve(): stall creation + role promotion."
  ).forEach(x => children.push(x));
  children.push(p("The remaining superadmin tools manage the rest of the platform. User management can suspend an account or reset a password (a superadmin account is protected and cannot be touched). Stall management can suspend a whole stall. Discount settings control the one active global promotion plus each stall's discount."));
  fig("s21.png", "Figure 5.25 — User management (suspend / reset password).", 560).forEach(x => children.push(x));
  fig("s22.png", "Figure 5.26 — Stall management.", 560).forEach(x => children.push(x));
  fig("s23.png", "Figure 5.27 — Global and per-stall discount settings.", 560).forEach(x => children.push(x));
  children.push(p("Finance and moderation tools complete the picture: a list of every transaction in the canteen, a canteen-wide sales report broken down by stall, review moderation, and an activity log that records logins, orders and admin actions."));
  fig("s24.png", "Figure 5.28 — All transactions across the canteen.", 540).forEach(x => children.push(x));
  fig("s25.png", "Figure 5.29 — Canteen-wide sales report.", 540).forEach(x => children.push(x));
  fig("s26.png", "Figure 5.30 — Sales report: breakdown by stall.", 540).forEach(x => children.push(x));
  fig("s27.png", "Figure 5.31 — Review moderation.", 560).forEach(x => children.push(x));
  fig("s28.png", "Figure 5.32 — The activity / audit log.", 540).forEach(x => children.push(x));
  children.push(p("The sales report is a good example of doing aggregation in SQL rather than in Java. The query joins stalls, menu items and order items and uses SUM and COUNT so the database does the heavy lifting:"));
  codeBlock(
`<select id="salesPerStall" resultType="SalesReport">
    SELECT s.name AS stall_name,
           IFNULL(SUM(oi.unit_price * oi.quantity), 0) AS revenue,
           COUNT(DISTINCT oi.order_id) AS order_count
    FROM stalls s
    LEFT JOIN menu_items m   ON m.stall_id = s.id
    LEFT JOIN order_items oi ON oi.menu_item_id = m.id
    GROUP BY s.id, s.name
    ORDER BY s.id
</select>`,
    "Listing 5.21 — OrderMapper.xml: sales aggregation per stall."
  ).forEach(x => children.push(x));
  children.push(p("Finally, the filled vendor sign-up below shows the very start of that whole approval chain — a new vendor (\"vendor3\") describing the stall they want to open, which later becomes the pending application the superadmin approves."));
  fig("s29.png", "Figure 5.33 — A new vendor filling in their stall details.", 540).forEach(x => children.push(x));

  /* ===================== 6. SUMMARY ===================== */
  children.push(h1("6. Summary"));
  children.push(p("Building E-Canteen taught me how the separate topics from the J2EE course fit together into one working system. Starting from an empty Spring Boot project, I built a four-layer application where a request flows cleanly from a controller, to a service, to a MyBatis mapper, and down to MySQL, and back again. Keeping that flow strict, and keeping the SQL in XML, made the code predictable and easy to debug."));
  children.push(p("The features I am most happy with are the ones that needed real thinking rather than just CRUD. The two-step discount engine with BigDecimal taught me to be careful with money. The vendor approval workflow taught me how a single action can change a user's role and unlock a whole new part of the site. The interceptor that re-checks the account on every request taught me that access control has to be enforced on the server, not hidden in the UI. And writing dynamic SQL with <if> and aggregation with SUM and GROUP BY showed me how much work the database can take off my hands."));
  children.push(p("If I continued the project, I would move the database password out of the properties file into an environment variable, add automated tests for the checkout calculation, and maybe push real-time order updates to the student's screen instead of refreshing the page. But for the goal of this course — a complete, readable J2EE application that I can explain from top to bottom — I feel the project meets what I set out to do. Most importantly, I now understand why the layered style is taught the way it is: it is not extra ceremony, it is what keeps a growing application from turning into a mess."));

  /* ===================== DOC ===================== */
  const doc = new Document({
    creator: "Muhammad Rizki Eka Pratama",
    title: "E-Canteen — J2EE Course Design Report",
    styles: {
      default: { document: { run: { font: "Calibri", size: 22, color: "222222" } } },
      paragraphStyles: [
        { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: 34, bold: true, color: RED, font: "Calibri" },
          paragraph: { spacing: { before: 240, after: 180 }, outlineLevel: 0 } },
        { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: 27, bold: true, color: INK, font: "Calibri" },
          paragraph: { spacing: { before: 200, after: 120 }, outlineLevel: 1 } },
        { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: 23, bold: true, color: "B5470F", font: "Calibri" },
          paragraph: { spacing: { before: 140, after: 80 }, outlineLevel: 2 } },
      ]
    },
    numbering: {
      config: [
        { reference: "bullets", levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 260 } } } }] },
        { reference: "nums", levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 260 } } } }] },
      ]
    },
    sections: [{
      properties: { page: { size: { width: 12240, height: 15840 }, margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } } },
      headers: { default: new Header({ children: [new Paragraph({
        alignment: AlignmentType.RIGHT,
        border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: "DDDDDD", space: 2 } },
        children: [new TextRun({ text: "E-Canteen — J2EE Course Design Report", size: 16, color: "999999" })]
      })] }) },
      footers: { default: new Footer({ children: [new Paragraph({
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text: "Muhammad Rizki Eka Pratama · L25020010      Page ", size: 16, color: "999999" }),
          new TextRun({ children: [PageNumber.CURRENT], size: 16, color: "999999" })]
      })] }) },
      children
    }]
  });

  const buffer = await Packer.toBuffer(doc);
  const outPath = path.join(__dirname, "E-Canteen_Report_L25020010.docx");
  fs.writeFileSync(outPath, buffer);
  console.log("WROTE", outPath, "(" + Math.round(buffer.length / 1024) + " KB)");
}

main().catch(e => { console.error(e); process.exit(1); });
