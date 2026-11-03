// Generates all UML / architecture / flow diagrams as PNG into ./assets
// Hand-drawn SVG -> rasterized with sharp at 2x for crisp print quality.
const fs = require("fs");
const path = require("path");
const sharp = require("sharp");

const OUT = path.join(__dirname, "assets");
if (!fs.existsSync(OUT)) fs.mkdirSync(OUT, { recursive: true });

// palette
const RED = "#E4322B";
const YELLOW = "#F6A609";
const INK = "#1c1c1c";
const GREY = "#6b6b6b";
const LINE = "#bdbdbd";
const BG = "#ffffff";
const SOFT = "#faf6f0";

const FONT = "font-family='Segoe UI, Arial, sans-serif'";

function wrap(svg, w, h) {
  return `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ${w} ${h}' width='${w}' height='${h}'>` +
    `<rect x='0' y='0' width='${w}' height='${h}' fill='${BG}'/>` + svg + `</svg>`;
}

// rounded box with title
function box(x, y, w, h, title, fill = SOFT, stroke = INK, tcolor = INK, fs = 15, bold = true) {
  return `<rect x='${x}' y='${y}' width='${w}' height='${h}' rx='10' fill='${fill}' stroke='${stroke}' stroke-width='1.5'/>` +
    `<text x='${x + w / 2}' y='${y + h / 2 + fs / 3}' ${FONT} font-size='${fs}' ${bold ? "font-weight='700'" : ""} fill='${tcolor}' text-anchor='middle'>${title}</text>`;
}

function arrow(x1, y1, x2, y2, color = INK, label = "", dashed = false) {
  const id = "ah" + Math.random().toString(36).slice(2, 7);
  let s = `<defs><marker id='${id}' markerWidth='10' markerHeight='10' refX='8' refY='3' orient='auto'>` +
    `<path d='M0,0 L8,3 L0,6 Z' fill='${color}'/></marker></defs>`;
  s += `<line x1='${x1}' y1='${y1}' x2='${x2}' y2='${y2}' stroke='${color}' stroke-width='1.6' ${dashed ? "stroke-dasharray='5,4'" : ""} marker-end='url(#${id})'/>`;
  if (label) {
    const mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
    s += `<rect x='${mx - label.length * 3.4 - 4}' y='${my - 11}' width='${label.length * 6.8 + 8}' height='18' rx='4' fill='#fff' stroke='${LINE}'/>` +
      `<text x='${mx}' y='${my + 2}' ${FONT} font-size='11' fill='${GREY}' text-anchor='middle'>${label}</text>`;
  }
  return s;
}

async function save(name, svg, w, h) {
  let full = wrap(svg, w, h);
  full = full.replace(/&(?!(amp|lt|gt|quot|apos|#\d+);)/g, "&amp;");
  fs.writeFileSync(path.join(OUT, name + ".svg"), full);
  await sharp(Buffer.from(full), { density: 150 }).png().toFile(path.join(OUT, name + ".png"));
  console.log("wrote", name);
}

/* ---------- 1. ARCHITECTURE (4-layer) ---------- */
async function architecture() {
  const W = 900, H = 560;
  let s = "";
  s += `<text x='${W / 2}' y='34' ${FONT} font-size='20' font-weight='700' fill='${INK}' text-anchor='middle'>E-Canteen Layered Architecture (Spring Boot + MyBatis)</text>`;
  const cx = W / 2, bw = 460, bh = 64, x = cx - bw / 2;
  const layers = [
    ["Browser  (Thymeleaf + Bootstrap views)", "#fff7ed", YELLOW, "User clicks / form submit"],
    ["Controller  (@Controller — handles HTTP, session)", "#fdecea", RED, "calls service method"],
    ["Service  (@Service — business logic, BigDecimal math)", SOFT, INK, "calls mapper method"],
    ["Mapper  (interface + XML — SQL only)", "#eef6ff", "#2f6fb0", "JDBC via MyBatis"],
    ["MySQL  database  (ecanteen)", "#eef9ee", "#2e8b57", ""],
  ];
  let y = 70;
  const gap = 96;
  layers.forEach((L, i) => {
    s += box(x, y, bw, bh, L[0], L[1], L[2], L[2] === YELLOW ? INK : L[2], 15);
    if (i < layers.length - 1) {
      s += arrow(cx, y + bh, cx, y + gap, GREY, L[3]);
      s += arrow(cx + 120, y + gap, cx + 120, y + bh + 4, LINE, "");
    }
    y += gap;
  });
  // side note
  s += `<text x='${cx + 245}' y='${70 + bh / 2}' ${FONT} font-size='11' fill='${GREY}'>no design</text>`;
  s += `<text x='${cx + 245}' y='${70 + bh / 2 + 14}' ${FONT} font-size='11' fill='${GREY}'>patterns</text>`;
  s += `<text x='${cx - 250}' y='${70 + 2 * gap + bh / 2}' ${FONT} font-size='11' fill='${GREY}' text-anchor='end'>data flows</text>`;
  s += `<text x='${cx - 250}' y='${70 + 2 * gap + bh / 2 + 14}' ${FONT} font-size='11' fill='${GREY}' text-anchor='end'>down &amp; back</text>`;
  await save("d_architecture", s, W, H);
}

/* ---------- 2. USE CASE ---------- */
async function useCase() {
  const W = 1000, H = 760;
  let s = "";
  s += `<text x='${W / 2}' y='30' ${FONT} font-size='20' font-weight='700' fill='${INK}' text-anchor='middle'>Use Case Diagram — E-Canteen</text>`;
  // system boundary
  s += `<rect x='300' y='55' width='400' height='680' rx='14' fill='none' stroke='${LINE}' stroke-width='1.5'/>`;
  s += `<text x='500' y='80' ${FONT} font-size='14' font-weight='700' fill='${GREY}' text-anchor='middle'>E-Canteen System</text>`;

  function actor(x, y, name) {
    return `<g stroke='${INK}' stroke-width='2' fill='none'>` +
      `<circle cx='${x}' cy='${y}' r='12' fill='#fff'/>` +
      `<line x1='${x}' y1='${y + 12}' x2='${x}' y2='${y + 40}'/>` +
      `<line x1='${x - 18}' y1='${y + 22}' x2='${x + 18}' y2='${y + 22}'/>` +
      `<line x1='${x}' y1='${y + 40}' x2='${x - 14}' y2='${y + 62}'/>` +
      `<line x1='${x}' y1='${y + 40}' x2='${x + 14}' y2='${y + 62}'/></g>` +
      `<text x='${x}' y='${y + 80}' ${FONT} font-size='13' font-weight='700' fill='${INK}' text-anchor='middle'>${name}</text>`;
  }
  function uc(cx, y, label, color = INK) {
    const w = 180, h = 38;
    return `<ellipse cx='${cx}' cy='${y}' rx='${w / 2}' ry='${h / 2}' fill='${SOFT}' stroke='${color}' stroke-width='1.4'/>` +
      `<text x='${cx}' y='${y + 4}' ${FONT} font-size='11.5' fill='${INK}' text-anchor='middle'>${label}</text>`;
  }
  function link(x1, y1, x2, y2, color = GREY) { return `<line x1='${x1}' y1='${y1}' x2='${x2}' y2='${y2}' stroke='${color}' stroke-width='1'/>`; }

  // actors: student & vendor on left, admin & superadmin on right
  s += actor(110, 200, "Student");
  s += actor(110, 560, "Vendor");
  s += actor(890, 220, "Admin");
  s += actor(890, 560, "Superadmin");

  const LX = 410;   // left column of use cases (student/vendor)
  const RX = 590;   // right column (admin/superadmin)

  // student use cases
  const sUC = [["Register / Login", 120], ["Browse menu & search", 175], ["Add to cart", 230], ["Checkout & pay", 285], ["Track order status", 340], ["Review & rate food", 395]];
  sUC.forEach(u => { s += link(135, 230, LX - 90, u[1]); s += uc(LX, u[1], u[0], RED); });
  // vendor
  s += link(135, 590, LX - 90, 470); s += uc(LX, 470, "Apply to open stall", INK);

  // admin use cases
  const aUC = [["Manage menu & stock", 130], ["Process orders", 185], ["Set stall discount", 240], ["View sales report", 295], ["Open / close stall", 350]];
  aUC.forEach(u => { s += link(865, 250, RX + 90, u[1], "#2f6fb0"); s += uc(RX, u[1], u[0], "#2f6fb0"); });
  // superadmin use cases
  const spUC = [["Approve / reject vendor", 470], ["Manage users & stalls", 525], ["Set global discount", 580], ["View global reports & logs", 635]];
  spUC.forEach(u => { s += link(865, 590, RX + 90, u[1], YELLOW); s += uc(RX, u[1], u[0], YELLOW); });

  await save("d_usecase", s, W, H);
}

/* ---------- 3. ER DIAGRAM ---------- */
async function er() {
  const W = 1000, H = 720;
  let s = "";
  s += `<text x='${W / 2}' y='30' ${FONT} font-size='20' font-weight='700' fill='${INK}' text-anchor='middle'>Entity Relationship Diagram — database "ecanteen"</text>`;

  function table(x, y, name, cols, head = INK) {
    const w = 190, rh = 22, hh = 28;
    const h = hh + cols.length * rh;
    let t = `<rect x='${x}' y='${y}' width='${w}' height='${h}' rx='6' fill='#fff' stroke='${INK}' stroke-width='1.4'/>`;
    t += `<rect x='${x}' y='${y}' width='${w}' height='${hh}' rx='6' fill='${head}'/>`;
    t += `<rect x='${x}' y='${y + hh - 8}' width='${w}' height='8' fill='${head}'/>`;
    t += `<text x='${x + w / 2}' y='${y + 19}' ${FONT} font-size='13' font-weight='700' fill='#fff' text-anchor='middle'>${name}</text>`;
    cols.forEach((c, i) => {
      const cy = y + hh + i * rh + 15;
      t += `<text x='${x + 10}' y='${cy}' ${FONT} font-size='11' fill='${c.startsWith("PK") ? RED : (c.startsWith("FK") ? "#2f6fb0" : INK)}' font-weight='${c.startsWith("PK") || c.startsWith("FK") ? "700" : "400"}'>${c}</text>`;
      if (i < cols.length - 1) t += `<line x1='${x}' y1='${y + hh + (i + 1) * rh}' x2='${x + w}' y2='${y + hh + (i + 1) * rh}' stroke='#eee'/>`;
    });
    return { svg: t, x, y, w, h, cx: x + w / 2, cy: y + h / 2 };
  }
  function rel(a, b, card) {
    return `<line x1='${a.x2}' y1='${a.y2}' x2='${b.x2}' y2='${b.y2}' stroke='${GREY}' stroke-width='1.3'/>`;
  }
  function conn(x1, y1, x2, y2, label) {
    let t = `<line x1='${x1}' y1='${y1}' x2='${x2}' y2='${y2}' stroke='${YELLOW}' stroke-width='2'/>`;
    const mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
    t += `<rect x='${mx - 12}' y='${my - 10}' width='26' height='16' rx='3' fill='#fff' stroke='${LINE}'/>`;
    t += `<text x='${mx + 1}' y='${my + 2}' ${FONT} font-size='10' fill='${GREY}' text-anchor='middle'>${label}</text>`;
    return t;
  }

  const users = table(40, 70, "users", ["PK id", "username", "password_hash", "email", "role", "FK stall_id", "is_active"]);
  const stalls = table(40, 330, "stalls", ["PK id", "name", "description", "is_active", "is_open", "discount_percent"], "#2e8b57");
  const menu = table(300, 330, "menu_items", ["PK id", "FK stall_id", "name", "price", "image_url", "is_available", "stock"], "#2e8b57");
  const cart = table(560, 70, "cart_items", ["PK id", "FK user_id", "FK menu_item_id", "quantity"], "#2f6fb0");
  const orders = table(560, 330, "orders", ["PK id", "FK user_id", "subtotal", "discount_total", "total_amount", "payment_method", "status"], RED);
  const oitems = table(810, 330, "order_items", ["PK id", "FK order_id", "FK menu_item_id", "item_name", "unit_price", "quantity"], RED);
  const slog = table(810, 70, "order_status_logs", ["PK id", "FK order_id", "status", "changed_at"], RED);
  const rev = table(300, 70, "reviews", ["PK id", "FK menu_item_id", "FK user_id", "rating", "comment"], YELLOW);
  const va = table(40, 560, "vendor_applications", ["PK id", "FK user_id", "stall_name", "status", "reject_reason"]);
  const disc = table(810, 560, "discounts", ["PK id", "name", "discount_percent", "is_active"]);

  [users, stalls, menu, cart, orders, oitems, slog, rev, va, disc].forEach(t => s += t.svg);

  // connections (approximate anchor points)
  s += conn(230, 130, 560, 110, "1..N");          // users-cart
  s += conn(140, 200, 90, 330, "N..1");           // users-stalls (stall_id)
  s += conn(230, 360, 300, 380, "1..N");          // stalls-menu
  s += conn(560, 200, 470, 360, "1..N");          // users-orders (user_id)
  s += conn(750, 420, 810, 420, "1..N");          // orders-order_items
  s += conn(400, 420, 470, 410, "N..1");          // menu-order_items
  s += conn(700, 360, 880, 150, "1..N");          // orders-status_logs
  s += conn(400, 200, 400, 330, "N..1");          // reviews-menu
  s += conn(120, 560, 120, 200, "N..1");          // vendor_app-users
  await save("d_er", s, W, H);
}

/* ---------- 4. CLASS DIAGRAM ---------- */
async function classDiagram() {
  const W = 1000, H = 640;
  let s = "";
  s += `<text x='${W / 2}' y='30' ${FONT} font-size='20' font-weight='700' fill='${INK}' text-anchor='middle'>Class Diagram (key classes by layer)</text>`;
  function cls(x, y, name, attrs, methods, head = INK, w = 210) {
    const ah = attrs.length * 16, mh = methods.length * 16, hh = 26;
    const h = hh + 6 + ah + 6 + mh + 6;
    let t = `<rect x='${x}' y='${y}' width='${w}' height='${h}' rx='4' fill='#fff' stroke='${INK}' stroke-width='1.3'/>`;
    t += `<rect x='${x}' y='${y}' width='${w}' height='${hh}' fill='${head}'/>`;
    t += `<text x='${x + w / 2}' y='${y + 18}' ${FONT} font-size='12.5' font-weight='700' fill='#fff' text-anchor='middle'>${name}</text>`;
    let yy = y + hh + 15;
    attrs.forEach(a => { t += `<text x='${x + 8}' y='${yy}' ${FONT} font-size='10.5' fill='${INK}'>${a}</text>`; yy += 16; });
    t += `<line x1='${x}' y1='${y + hh + 6 + ah}' x2='${x + w}' y2='${y + hh + 6 + ah}' stroke='${LINE}'/>`;
    yy = y + hh + ah + 21;
    methods.forEach(m => { t += `<text x='${x + 8}' y='${yy}' ${FONT} font-size='10.5' fill='${GREY}'>${m}</text>`; yy += 16; });
    return { svg: t, x, y, w, h, bottom: y + h, right: x + w, cx: x + w / 2 };
  }
  const ac = cls(40, 70, "AuthController", ["- userService", "- logService"], ["+ login()", "+ register()", "+ logout()"], RED);
  const oc = cls(40, 250, "OrderController", ["- orderService", "- cartService"], ["+ checkout()", "+ place()", "+ detail()"], RED);
  const us = cls(330, 70, "UserService", ["- userMapper", "- passwordEncoder"], ["+ register()", "+ login()", "+ changePassword()"], "#2e8b57");
  const os = cls(330, 250, "OrderService", ["- orderMapper", "- discountMapper", "- cartService"], ["+ checkout()", "+ advanceStatus()", "+ calcCheckoutTotals()"], "#2e8b57");
  const um = cls(640, 70, "«interface» UserMapper", ["(SQL in XML)"], ["+ findByUsername()", "+ insertUser()", "+ updatePassword()"], "#2f6fb0");
  const om = cls(640, 250, "«interface» OrderMapper", ["(SQL in XML)"], ["+ insertOrder()", "+ updateStatus()", "+ salesByStall()"], "#2f6fb0");
  const user = cls(640, 460, "User (entity)", ["- id, username", "- passwordHash", "- role, stallId"], ["+ getters/setters"], INK, 200);
  const order = cls(330, 460, "Order (entity)", ["- id, userId", "- subtotal, total", "- status"], ["+ getters/setters"], INK, 200);

  [ac, oc, us, os, um, om, user, order].forEach(c => s += c.svg);
  s += arrow(ac.right, ac.y + 40, us.x, us.y + 40, GREY, "uses");
  s += arrow(oc.right, oc.y + 50, os.x, os.y + 50, GREY, "uses");
  s += arrow(us.right, us.y + 40, um.x, um.y + 40, GREY, "uses");
  s += arrow(os.right, os.y + 50, om.x, om.y + 50, GREY, "uses");
  s += arrow(os.cx, os.bottom, order.cx, order.y, LINE, "");
  s += arrow(om.cx, om.bottom, user.cx, user.y, LINE, "");
  await save("d_class", s, W, H);
}

/* ---------- 5. SEQUENCE (place order) ---------- */
async function sequence() {
  const W = 1000, H = 640;
  let s = "";
  s += `<text x='${W / 2}' y='30' ${FONT} font-size='20' font-weight='700' fill='${INK}' text-anchor='middle'>Sequence Diagram — Student places an order</text>`;
  const cols = [["Student", 90, RED], ["OrderController", 270, RED], ["OrderService", 470, "#2e8b57"], ["Mapper / XML", 680, "#2f6fb0"], ["MySQL", 880, "#444"]];
  const top = 60, bottom = 600;
  cols.forEach(c => {
    s += box(c[1] - 75, top, 150, 34, c[0], "#fff", c[2], c[2] === RED ? RED : INK, 12);
    s += `<line x1='${c[1]}' y1='${top + 34}' x2='${c[1]}' y2='${bottom}' stroke='${LINE}' stroke-dasharray='4,4'/>`;
  });
  function msg(fromX, toX, y, label, ret = false) {
    return arrow(fromX, y, toX, y, ret ? GREY : INK, "") +
      `<text x='${(fromX + toX) / 2}' y='${y - 6}' ${FONT} font-size='11' fill='${ret ? GREY : INK}' text-anchor='middle' ${ret ? "font-style='italic'" : ""}>${label}</text>`;
  }
  let y = 130;
  const step = 52;
  s += msg(90, 270, y, "POST /order/place"); y += step;
  s += msg(270, 470, y, "checkout(userId, payment)"); y += step;
  s += msg(470, 680, y, "getCartView() / findActiveDiscount()"); y += step;
  s += msg(680, 880, y, "SELECT ..."); y += step;
  s += msg(880, 680, y, "rows", true); y += step;
  // self call for BigDecimal
  s += `<rect x='455' y='${y - 14}' width='200' height='26' rx='5' fill='${SOFT}' stroke='${YELLOW}'/>` +
    `<text x='555' y='${y + 3}' ${FONT} font-size='10.5' fill='${INK}' text-anchor='middle'>compute discount (BigDecimal)</text>`; y += step;
  s += msg(470, 680, y, "insertOrder() / insertOrderItem() / decreaseStock()"); y += step;
  s += msg(680, 880, y, "INSERT / UPDATE"); y += step;
  s += msg(470, 270, y, "return orderId", true); y += step;
  s += msg(270, 90, y, "redirect to confirmation", true);
  await save("d_sequence", s, W, H);
}

/* ---------- 6. WEBSITE FLOW ---------- */
async function flow() {
  const W = 1000, H = 660;
  let s = "";
  s += `<text x='${W / 2}' y='30' ${FONT} font-size='20' font-weight='700' fill='${INK}' text-anchor='middle'>Website Flow — role-based navigation</text>`;
  // login hub
  s += box(410, 60, 180, 46, "Login / Register", "#fff", INK, INK, 14);
  s += box(410, 130, 180, 40, "Role check (session)", SOFT, YELLOW, INK, 12);
  s += arrow(500, 106, 500, 130, GREY);

  // four columns
  function col(x, color, head, steps) {
    let t = box(x, 200, 200, 40, head, color, color, color === YELLOW ? INK : "#ffffff", 13);
    t += arrow(500, 170, x + 100, 200, LINE);
    let y = 258;
    let prevY = 240;
    steps.forEach(st => {
      t += box(x, y, 200, 38, st, "#fff", GREY, INK, 11.5, false);
      t += arrow(x + 100, prevY, x + 100, y, LINE);
      prevY = y + 38;
      y += 58;
    });
    return t;
  }
  s += col(40, RED, "STUDENT", ["Browse menu /menu", "Cart /cart", "Checkout /order", "History & review"]);
  s += col(280, INK, "VENDOR (pending)", ["Apply to open stall", "Wait for approval"]);
  s += col(520, "#2f6fb0", "ADMIN (vendor)", ["Dashboard /admin", "Manage menu & orders", "Discount & report"]);
  s += col(760, YELLOW, "SUPERADMIN", ["Approve vendors", "Manage users/stalls", "Global discount & logs"]);
  // approval arrow vendor->admin
  s += arrow(380, 320, 760, 280, RED, "approved");
  await save("d_flow", s, W, H);
}

(async () => {
  await architecture();
  await useCase();
  await er();
  await classDiagram();
  await sequence();
  await flow();
  console.log("ALL DIAGRAMS DONE");
})();
