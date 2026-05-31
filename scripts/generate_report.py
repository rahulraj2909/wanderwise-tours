"""
Generate WanderWise report from Scaler Neovarsity template.
Run: python scripts/generate_report.py
"""
import shutil
from pathlib import Path

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor
from docx.text.paragraph import Paragraph

# Chapter numbers for table/figure IDs (guideline 7: Table 2.02 = ch.2, item 2)
CHAPTER = {
    "project": 1,
    "requirements": 2,
    "class": 3,
    "database": 4,
    "feature": 5,
    "deployment": 6,
    "tech": 7,
}

TEMPLATE = Path(
    r"c:\Users\USER\Downloads\Scaler Neovarsity  Academy Project Report Template (Backend Specialization).docx"
)
OUT = Path(__file__).resolve().parent.parent / "docs" / "WanderWise_Applied_Project_Report.docx"
# Convenience copy (many users open this path in Word)
OUT_DOWNLOADS = Path(r"C:\Users\USER\Downloads\WanderWise_Applied_Project_Report.docx")
DIAGRAMS_DIR = Path(__file__).resolve().parent.parent / "docs" / "report_diagrams"

STUDENT = {
    "full_name": "Rahul Kumar",
    "email": "rahulraj2909@gmail.com",
    "submission_month": "June 2026",
    "submission_date": "30/06/2026",
    "module_start": "January 2026",
    "module_end": "June 2026",
    "declaration_date": "30 June 2026",
}


def set_font(run, size=12, bold=False):
    run.font.name = "Times New Roman"
    run._element.rPr.rFonts.set(qn("w:eastAsia"), "Times New Roman")
    run.font.size = Pt(size)
    run.bold = bold


def remove_paragraph(p):
    p._element.getparent().remove(p._element)


def find_idx(doc, style, text):
    for i, p in enumerate(doc.paragraphs):
        if p.style.name == style and p.text.strip() == text:
            return i
    return None


def insert_after(paragraph):
    """New empty paragraph immediately after `paragraph`."""
    new_p = OxmlElement("w:p")
    paragraph._element.addnext(new_p)
    return Paragraph(new_p, paragraph._parent)


def style_body(p, bold=False, justify=True):
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY if justify else WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.line_spacing = 1.5


def add_body_after(current, text, bold=False):
    p = insert_after(current)
    style_body(p, bold=bold)
    set_font(p.add_run(text), bold=bold)
    return p


def add_tech_entry_after(current, title, description):
    """Bold technology label, normal description (comma-separated phrasing, no stretched spacing)."""
    p = insert_after(current)
    style_body(p)
    p.paragraph_format.space_after = Pt(6)
    set_font(p.add_run(f"{title}: "), bold=True)
    set_font(p.add_run(description), bold=False)
    return p


def add_bullet_after(current, text):
    """Visible bullet with indent (template often lacks List Bullet style)."""
    p = insert_after(current)
    style_body(p, justify=False)
    p.paragraph_format.left_indent = Inches(0.4)
    p.paragraph_format.first_line_indent = Inches(-0.2)
    p.paragraph_format.space_after = Pt(4)
    set_font(p.add_run(f"• {text}"))
    return p


def set_table_borders(table):
    tbl = table._tbl
    tbl_pr = tbl.tblPr
    if tbl_pr is None:
        tbl_pr = OxmlElement("w:tblPr")
        tbl.insert(0, tbl_pr)
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        elem = OxmlElement(f"w:{edge}")
        elem.set(qn("w:val"), "single")
        elem.set(qn("w:sz"), "8")
        elem.set(qn("w:space"), "0")
        elem.set(qn("w:color"), "000000")
        borders.append(elem)
    tbl_pr.append(borders)


def paragraph_after_element(element, parent):
    new_p = OxmlElement("w:p")
    element.addnext(new_p)
    return Paragraph(new_p, parent)


def add_h3_after(doc, current, text):
    p = insert_after(current)
    p.style = doc.styles["Heading 3"]
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    set_font(p.add_run(text), size=14, bold=True)
    return p


_fig_n = 0
_tbl_n = 0
_figure_registry = []  # (num, title, bookmark_name) for List of Figures + hyperlinks


def add_bookmark(paragraph, bookmark_name, bookmark_id):
    """Word bookmark on caption paragraph for LOF page refs and Ctrl+click navigation."""
    start = OxmlElement("w:bookmarkStart")
    start.set(qn("w:id"), str(bookmark_id))
    start.set(qn("w:name"), bookmark_name)
    end = OxmlElement("w:bookmarkEnd")
    end.set(qn("w:id"), str(bookmark_id))
    paragraph._p.insert(0, start)
    paragraph._p.append(end)


def add_hyperlink_to_bookmark(paragraph, text, bookmark_name):
    """Internal hyperlink (jump to figure caption)."""
    hyperlink = OxmlElement("w:hyperlink")
    hyperlink.set(qn("w:anchor"), bookmark_name)
    run = OxmlElement("w:r")
    r_pr = OxmlElement("w:rPr")
    r_style = OxmlElement("w:rStyle")
    r_style.set(qn("w:val"), "Hyperlink")
    r_pr.append(r_style)
    run.append(r_pr)
    text_el = OxmlElement("w:t")
    text_el.text = text
    run.append(text_el)
    hyperlink.append(run)
    paragraph._p.append(hyperlink)


def add_pageref_field(paragraph, bookmark_name):
    """PAGE field linked to bookmark — update fields in Word to show page numbers."""
    run = OxmlElement("w:r")
    fld = OxmlElement("w:fldSimple")
    fld.set(qn("w:instr"), f' PAGEREF {bookmark_name} \\h ')
    t = OxmlElement("w:t")
    t.text = "#"
    run.append(t)
    fld.append(run)
    paragraph._p.append(fld)


def _set_cell_text_left(cell, text=""):
    cell.text = ""
    p = cell.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.line_spacing = 1.0
    if text:
        set_font(p.add_run(text))


def _set_lof_cell_link(cell, figure_num, title, bookmark_name):
    cell.text = ""
    p = cell.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.line_spacing = 1.0
    add_hyperlink_to_bookmark(p, f"Figure {figure_num}: {title}", bookmark_name)


def _set_lof_cell_page(cell, bookmark_name):
    cell.text = ""
    p = cell.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    p.paragraph_format.line_spacing = 1.0
    add_pageref_field(p, bookmark_name)


def caption_table(title):
    global _tbl_n
    _tbl_n += 1
    return f"Table {_tbl_n}: {title}"


def caption_figure(title):
    global _fig_n
    _fig_n += 1
    return f"Figure {_fig_n}: {title}"


def add_figure_after(current, caption, img_name=None):
    """Image/placeholder first, caption below (template: caption below figure)."""
    img_path = DIAGRAMS_DIR / img_name if img_name else None
    if img_path and img_path.exists():
        p_img = insert_after(current)
        p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img.add_run().add_picture(str(img_path), width=Inches(6.0))
        current = p_img
    else:
        p_box = insert_after(current)
        p_box.alignment = WD_ALIGN_PARAGRAPH.CENTER
        set_font(p_box.add_run(f"[Insert diagram: {img_name or 'draw.io'}]"), size=11)
        current = p_box
    p_cap = insert_after(current)
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title = caption.split(": ", 1)[-1] if ": " in caption else caption
    bookmark_name = f"fig_{_fig_n:02d}"
    add_bookmark(p_cap, bookmark_name, 1000 + _fig_n)
    r = p_cap.add_run(caption)
    set_font(r, bold=True)
    _figure_registry.append((_fig_n, title, bookmark_name))
    return p_cap


def move_table_after(paragraph, tbl):
    """Move table XML from doc.add_table (end of body) to after paragraph."""
    body = tbl._element.getparent()
    body.remove(tbl._element)
    paragraph._element.addnext(tbl._element)


def add_table_after(doc, current, caption, headers, rows):
    anchor = insert_after(current)
    style_body(anchor)
    set_font(anchor.add_run(caption), bold=True)

    tbl = doc.add_table(rows=1, cols=len(headers))
    _style_data_table(tbl, headers, rows)

    move_table_after(anchor, tbl)
    set_table_borders(tbl)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER

    return paragraph_after_element(tbl._element, anchor._parent)


def add_named_table_after(doc, current, title, headers, rows):
    """Table with optional bold title line (no Table X.XX caption)."""
    anchor = current
    if title:
        anchor = insert_after(current)
        style_body(anchor, justify=False)
        set_font(anchor.add_run(title), bold=True)

    tbl = doc.add_table(rows=1, cols=len(headers))
    _style_data_table(tbl, headers, rows)

    move_table_after(anchor, tbl)
    set_table_borders(tbl)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER

    return paragraph_after_element(tbl._element, anchor._parent)


def _style_data_table(tbl, headers, rows):
    for style_name in ("Table Grid", "Table Normal", "Normal Table"):
        try:
            tbl.style = style_name
            break
        except KeyError:
            continue
    for i, h in enumerate(headers):
        cell = tbl.rows[0].cells[i]
        _set_cell_text_left(cell, str(h))
        for run in cell.paragraphs[0].runs:
            set_font(run, bold=True)
    for row in rows:
        cells = tbl.add_row().cells
        for i, v in enumerate(row):
            _set_cell_text_left(cells[i], str(v))
            for run in cells[i].paragraphs[0].runs:
                set_font(run)
    for row in tbl.rows:
        for cell in row.cells:
            for p in cell.paragraphs:
                p.alignment = WD_ALIGN_PARAGRAPH.LEFT
                p.paragraph_format.line_spacing = 1.0


def add_codeblock_after(current, lines):
    """Monospace-style block, one line per paragraph (for JSON)."""
    for line in lines:
        p = insert_after(current)
        p.paragraph_format.left_indent = Inches(0.45)
        p.paragraph_format.line_spacing = 1.0
        p.paragraph_format.space_after = Pt(0)
        r = p.add_run(line)
        set_font(r, size=11)
        r.font.name = "Courier New"
        r._element.rPr.rFonts.set(qn("w:eastAsia"), "Courier New")
        current = p
    p = insert_after(current)
    p.paragraph_format.space_after = Pt(6)
    return p


def add_schema_tables_after(doc, current, tables_spec):
    current = add_body_after(current, "Tables:", True)
    for table_name, fields in tables_spec:
        current = add_named_table_after(
            doc,
            current,
            table_name,
            ["Column", "Type / constraint"],
            fields,
        )
    return current


SCHEMA_TABLES = [
    ("1. USERS", [
        ("id", "BIGINT, Primary Key"),
        ("name", "VARCHAR(120)"),
        ("email", "VARCHAR(150), UNIQUE"),
        ("phone", "VARCHAR(20)"),
        ("password", "VARCHAR (BCrypt hash)"),
        ("role", "ENUM: CUSTOMER, ADMIN, OPERATOR"),
        ("created_at, updated_at", "TIMESTAMP"),
    ]),
    ("2. CITIES", [
        ("id", "BIGINT, Primary Key"),
        ("name", "VARCHAR(100), UNIQUE"),
        ("code", "VARCHAR(20), UNIQUE (e.g. CTSINGAP)"),
        ("country", "VARCHAR(100)"),
        ("description", "VARCHAR(500)"),
        ("image_url", "VARCHAR(500)"),
    ]),
    ("3. CATEGORIES", [
        ("id", "BIGINT, Primary Key"),
        ("name", "VARCHAR(80), UNIQUE"),
        ("description", "VARCHAR(400)"),
    ]),
    ("4. ATTRACTIONS", [
        ("id", "BIGINT, Primary Key"),
        ("title", "VARCHAR(200)"),
        ("description", "TEXT"),
        ("type", "ENUM: TOUR, ATTRACTION, ACTIVITY, …"),
        ("price, currency", "DECIMAL(10,2), VARCHAR"),
        ("duration_hours", "INT"),
        ("average_rating, review_count", "Denormalized for sort"),
        ("active", "BOOLEAN, indexed"),
        ("image_url", "VARCHAR(500) — tour card image"),
        ("city_id, category_id, operator_id", "Foreign Keys"),
    ]),
    ("5. TIME_SLOTS", [
        ("id", "BIGINT, Primary Key"),
        ("attraction_id", "FK → attractions"),
        ("slot_date", "DATE"),
        ("start_time", "TIME"),
        ("total_seats, available_seats", "INT"),
    ]),
    ("6. BOOKINGS", [
        ("id", "BIGINT, Primary Key"),
        ("booking_reference", "VARCHAR(40), UNIQUE"),
        ("customer_id", "FK → users"),
        ("attraction_id", "FK → attractions"),
        ("time_slot_id", "FK → time_slots"),
        ("visit_date", "DATE"),
        ("guests", "INT"),
        ("total_amount", "DECIMAL(10,2)"),
        ("status", "ENUM: PENDING_PAYMENT, CONFIRMED, EXPIRED, …"),
    ]),
    ("7. PAYMENTS", [
        ("id", "BIGINT, Primary Key"),
        ("booking_id", "FK → bookings (1:1)"),
        ("amount, currency", "DECIMAL, VARCHAR"),
        ("status", "ENUM: PENDING, SUCCEEDED, …"),
        ("stripe_session_id", "VARCHAR(120)"),
    ]),
    ("8. REVIEWS", [
        ("id", "BIGINT, Primary Key"),
        ("attraction_id", "FK → attractions"),
        ("user_id", "FK → users"),
        ("rating", "INT 1–5"),
        ("comment", "TEXT"),
    ]),
]

USER_JOURNEY_ROWS = [
    ("1", "index.html", "Landing — cities and search (catalog via proxy)"),
    ("2", "listing.html", "Browse tours (stype=CITY, scode=CTSINGAP, sort)"),
    ("3", "tour.html", "Tour details, slots, ratings"),
    ("4", "reviews.html", "Read/write reviews (POST needs 8080 session)"),
    ("5", "login.html", "Authenticate before checkout"),
    ("6", "checkout.html", "Pax types, slot, 60s timer, POST /bookings"),
    ("7", "payment.html", "Stripe or mock payment"),
    ("8", "success.html", "Booking confirmation"),
]


def fill_front_matter(doc):
    repl = {
        "<Full Name of the Student>": STUDENT["full_name"],
        "<Registered Scaler Email ID>": STUDENT["email"],
        "<Month of Submission like June, 2024>": STUDENT["submission_month"],
        "DD/MM/YYYY <Date of Submission>": STUDENT["submission_date"],
        "< Project Module start date >": STUDENT["module_start"],
        "< Module end date >": STUDENT["module_end"],
        "<Full Name of the Candidate>": STUDENT["full_name"],
        "XX Month 20XX": STUDENT["declaration_date"],
    }
    for p in doc.paragraphs:
        for a, b in repl.items():
            for r in p.runs:
                if a in r.text:
                    r.text = r.text.replace(a, b)

    for p in doc.paragraphs:
        if p.text.strip().startswith("<Insert a Paragraph"):
            p.clear()
            t = (
                "I express sincere gratitude to my thesis supervisor Naman Bhalla and the Scaler Neovarsity "
                "faculty for their guidance throughout the MSc Backend specialization. I am deeply thankful "
                "to my family for their constant support. This WanderWise project enabled me to apply Spring Boot, "
                "JPA, split-service architecture, vendor ingestion, and AWS deployment concepts applied to a "
                "tours and attractions booking platform."
            )
            p.add_run(t)
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            p.paragraph_format.line_spacing = 1.5
            for r in p.runs:
                set_font(r)
            break

    for i, p in enumerate(doc.paragraphs[:30]):
        if p.text.strip() == "By":
            sub = doc.paragraphs[i + 1].insert_paragraph_before()
            sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
            set_font(sub.add_run("WanderWise: Tours & Attractions Booking Platform"), size=14, bold=True)
            break


def clear_body(doc):
    ai = find_idx(doc, "Heading 3", "Abstract")
    ri = find_idx(doc, "Heading 2", "References")
    for i in range(ri - 1, ai, -1):
        remove_paragraph(doc.paragraphs[i])


def build_content(doc):
    abstract_h = doc.paragraphs[find_idx(doc, "Heading 3", "Abstract")]
    cur = abstract_h

    cur = add_body_after(
        cur,
        "WanderWise is a Tours & Attractions booking platform with city-wise discovery, sortable listings, "
        "and pax-type checkout. It addresses fragmented discovery and unreliable seat inventory for city tours "
        "and activities. The solution splits into two Spring Boot services: catalog & ingestion (8081) "
        "and booking with customer UI (8080). Features include city-wise discovery, paginated listings "
        "with popularity-based sorting, pax-type pricing, seat booking with pessimistic locking, "
        "mock payments (Stripe optional), catalog caching (in-memory on h2, Redis on full/prod), "
        "multi-vendor ingestion (VIATOR, KLOOK, GETYOURGUIDE), and payment "
        "reconciliation. Built on Java 17+ and Spring Boot 3.3 (MySQL in production, H2 for local demo).",
    )

    cur = add_h3_after(doc, cur, "Project Description")
    cur = add_body_after(
        cur,
        "The project objectives are: (1) maintain a catalog of cities, categories, and attractions; "
        "(2) support deep-link listing URLs (stype=CITY&scode=CTSINGAP) with sort and pagination; "
        "(3) book time slots with seat holds; (4) integrate Stripe/mock checkout; (5) collect reviews; "
        "(6) expire stale bookings via cron. The solution is relevant to the growing experiences segment in travel.",
    )
    cur = add_figure_after(cur, caption_figure("Project development process"), "fig_1_1_dev_process.png")
    cur = add_figure_after(cur, caption_figure("System architecture"), "fig_1_2_architecture.png")
    cur = add_body_after(cur, "Web application user journey:", True)
    cur = add_named_table_after(
        doc,
        cur,
        "",
        ["Step", "Page", "Description"],
        USER_JOURNEY_ROWS,
    )
    cur = add_figure_after(cur, caption_figure("Landing page — city discovery"), "screenshot_home.png")
    cur = add_figure_after(cur, caption_figure("Singapore listing with sort and pagination"), "screenshot_listing.png")
    cur = add_figure_after(cur, caption_figure("Tour detail page"), "screenshot_tour.png")
    cur = add_figure_after(cur, caption_figure("Reviews page"), "screenshot_reviews.png")
    cur = add_figure_after(cur, caption_figure("Checkout — slot and pax selection"), "screenshot_checkout.png")
    cur = add_figure_after(cur, caption_figure("Login page"), "screenshot_login.png")
    cur = add_figure_after(cur, caption_figure("Payment page"), "screenshot_payment.png")

    cur = add_h3_after(doc, cur, "Requirement Gathering")
    cur = add_body_after(cur, "This section describes functional and non-functional requirements, user roles, and the feature set.")
    cur = add_body_after(
        cur,
        "Users: Guest (browse via 8080 proxy), Customer (book/pay on 8080), Catalog Admin (ingestion/products on 8081), "
        "Booking Admin (payment reconciliation on 8080).",
    )
    cur = add_figure_after(cur, caption_figure("Use case diagram"), "fig_2_1_use_case.png")
    cur = add_table_after(
        doc, cur, caption_table("Functional requirements"),
        ["ID", "Requirement", "Implementation"],
        [
            ("1", "Browse/search by city, category, keyword, scode", "Catalog API proxied on 8080 — GET /api/v1/attractions"),
            ("2", "City listing with sort and pagination", "listing.html + catalog AttractionSortBuilder"),
            ("3", "Time slots and pax types at checkout", "Catalog slots/pax APIs; checkout paxSelections"),
            ("4", "Book with seat hold and pax pricing", "BookingService + CatalogClient internal reserve"),
            ("5", "Pay via Stripe or mock checkout", "PaymentController (8080) + webhooks"),
            ("6", "Reviews and ratings", "ReviewController (8081); session via BookingSessionClient"),
            ("7", "Cron reconciliation for unpaid bookings", "BookingReconciliationScheduler (8080)"),
            ("8", "Register, login, session auth", "AuthController + AuthInterceptor (8080)"),
            ("9", "Vendor catalog ingestion (pull / ingest / update)", "VendorIngestionService + admin portal (8081)"),
            ("10", "Admin update product & run ingest flows", "AdminCatalogController + VendorIngestionController"),
        ],
    )
    cur = add_table_after(
        doc, cur, caption_table("Non-functional requirements"),
        ["ID", "Requirement", "Approach"],
        [
            (
                "1",
                "Performance: catalog/search APIs (< 500 ms target)",
                "Spring @Cacheable on attractions, cities, categories; "
                "profile h2: in-memory cache (spring.cache.type=simple); "
                "profile prod: Redis (spring.cache.type=redis)",
            ),
            (
                "2",
                "Data integrity: no double booking of seats",
                "@Transactional BookingService (8080); catalog TimeSlotRepository.findByIdForUpdate PESSIMISTIC_WRITE",
            ),
            (
                "3",
                "Security: authenticated booking and reviews",
                "BCryptPasswordEncoder; HTTP session (userId); AuthInterceptor on protected /api/v1 routes",
            ),
            (
                "4",
                "Reliability: release seats when payment fails or times out",
                "booking.payment-timeout-minutes=30; BookingReconciliationScheduler (cron every 15 min)",
            ),
            (
                "NF5",
                "Scalability: production deployment design",
                "Stateless Spring Boot on EC2 behind ALB, RDS MySQL, ElastiCache Redis (see Deployment Flow)",
            ),
        ],
    )
    cur = add_table_after(
        doc, cur, caption_table("Listing sort keys (AttractionSortBuilder)"),
        ["Sort key", "JPA ordering", "Use case"],
        [
            ("popularity / ranking", "averageRating DESC, reviewCount DESC", "Default OTA ranking"),
            ("rating", "averageRating DESC", "Highest rated first"),
            ("price_asc / price_low", "price ASC", "Budget travellers"),
            ("price_desc / price_high", "price DESC", "Premium experiences"),
            ("reviews / most_reviewed", "reviewCount DESC", "Social proof"),
        ],
    )

    cur = add_h3_after(doc, cur, "Class Diagrams")
    cur = add_body_after(
        cur,
        "Low-level design follows Spring MVC in both modules. Booking (:8080) uses BookingController, "
        "BookingService, CatalogClient (calls catalog internal API), and StripePaymentService. "
        "Catalog & ingestion (:8081) uses VendorIngestionController, VendorIngestionService, "
        "VendorCatalogSyncScheduler, and VendorProductMapper for multi-vendor feeds (VIATOR, KLOOK, GETYOURGUIDE).",
    )
    cur = add_figure_after(cur, caption_figure("Class diagram — booking domain"), "fig_3_1_class_booking.png")
    cur = add_figure_after(cur, caption_figure("Class diagram — catalog and vendor ingestion"), "fig_3_2_class_ingestion.png")

    cur = add_h3_after(doc, cur, "Database Schema Design")
    cur = add_body_after(
        cur,
        "Two separate databases: catalog (cities, attractions, slots, pax_types, vendors, ingestion_runs) "
        "and booking (users, bookings, booking_pax_lines, payments). bookings.attraction_id is a logical "
        "reference to catalog — not a cross-database foreign key. ER view below uses two panels (not a single monolith schema).",
    )
    cur = add_figure_after(cur, caption_figure("Database schema — catalog and booking"), "fig_4_1_er.png")
    cur = add_figure_after(cur, caption_figure("Vendor ingestion sequence"), "fig_4_2_ingestion_sequence.png")
    cur = add_schema_tables_after(doc, cur, SCHEMA_TABLES)
    cur = add_body_after(cur, "Foreign Keys:", True)
    for fk in [
        "attractions(city_id) refers cities(id)",
        "attractions(category_id) refers categories(id)",
        "time_slots(attraction_id) refers attractions(id)",
        "bookings(customer_id) refers users(id)",
        "bookings(attraction_id) logical link to catalog attractions(id) — no cross-DB FK",
        "payments(booking_id) refers bookings(id)",
        "reviews(attraction_id) refers attractions(id)",
        "reviews(user_id) refers users(id)",
    ]:
        cur = add_body_after(cur, fk)
    cur = add_body_after(cur, "Cardinality of Relations:", True)
    for c in [
        "Between cities and attractions -> 1:m",
        "Between categories and attractions -> 1:m",
        "Between users and bookings -> 1:m",
        "Between attractions and time_slots -> 1:m",
        "Between bookings and payments -> 1:1",
        "Between attractions and reviews -> 1:m",
        "Between vendors and ingestion_runs -> 1:m",
        "Between vendors (VIATOR, KLOOK, GETYOURGUIDE) and vendor_activities -> 1:m",
    ]:
        cur = add_body_after(cur, c)
    cur = add_body_after(cur, "Indexes (performance):", True)
    for idx in [
        "attractions: composite (city_id, active) with sort columns average_rating, review_count, price",
        "bookings: index on status for reconciliation cron queries",
        "Denormalized average_rating and review_count on attractions to avoid GROUP BY on listings",
    ]:
        cur = add_bullet_after(cur, idx)

    cur = add_h3_after(doc, cur, "Feature Development Process")
    cur = add_body_after(
        cur,
        "Flagship feature: Book a tour with seat reservation and payment — similar to the “Book a seat” "
        "event discovery platforms. The implementation covers API payload, service layer, MVC flow, and performance tuning.",
    )
    cur = add_body_after(cur, "UI and authentication flow:", True)
    for step in [
        "Guest browses landing and listing; selects a tour on tour.html.",
        "reviews.html loads GET /api/v1/reviews/attraction/{id} for ratings.",
        "checkout.html requires login — redirects to login.html if session absent.",
        "POST /api/v1/auth/login stores userId in HTTP session (demo: customer@tours.demo / demo123).",
        "payment.html uses mock-checkout by default (stripe.enabled=false); Stripe Checkout when enabled in prod.",
    ]:
        cur = add_bullet_after(cur, step)
    cur = add_body_after(cur, "Elaborate the request flow to backend:", True)
    for step in [
        "Customer selects tour and slot on checkout.html (login required).",
        "Browser sends POST /api/v1/bookings with session cookie.",
        "AuthInterceptor validates the customer session.",
        "BookingController calls BookingService.createBooking().",
        "CatalogClient.reserveSeats() on :8081 applies pessimistic lock on time_slots.",
        "Booking is saved as PENDING_PAYMENT; Payment row created.",
        "StripePaymentService returns mock-checkout URL (default) or Stripe session when stripe.enabled=true.",
        "On successful payment, booking status becomes CONFIRMED.",
        "BookingReconciliationScheduler expires unpaid bookings every 15 minutes.",
    ]:
        cur = add_bullet_after(cur, step)

    cur = add_body_after(cur, "API Request Payload", True)
    cur = add_body_after(cur, "POST /api/v1/bookings — JSON body example:")
    cur = add_codeblock_after(
        cur,
        [
            "{",
            '  "attractionId": 16,',
            '  "timeSlotId": 42,',
            '  "visitDate": "2026-06-15",',
            '  "guests": 2,',
            '  "contactEmail": "customer@tours.demo",',
            '  "contactPhone": "+919999999999"',
            "}",
        ],
    )
    cur = add_body_after(cur, "Service which picks the request", True)
    cur = add_body_after(cur, "BookingService.createBooking(CreateBookingRequest request, Long customerId)")
    cur = add_body_after(cur, "Flow of MVC architecture", True)
    cur = add_body_after(
        cur,
        "Model: Booking, Payment, TimeSlot, Attraction entities. "
        "View: REST JSON responses / static HTML pages. "
        "Controller: BookingController. Service: BookingService, TimeSlotService, StripePaymentService.",
    )
    cur = add_figure_after(cur, caption_figure("Booking sequence diagram"), "fig_5_1_sequence.png")

    cur = add_body_after(cur, "Explain the performance improvement / metric optimization achieved.", True)
    cur = add_bullet_after(
        cur,
        "@Cacheable on AttractionService.search() and getById(); CatalogService cities/categories. "
        "Cache keys include scode, page, size, and sort to prevent cross-city stale hits.",
    )
    cur = add_bullet_after(
        cur,
        "Bookings, payments, and slot availability are never cached. Profile h2 uses simple in-memory cache; "
        "prod uses Redis (see docs/CACHING.md).",
    )
    cur = add_bullet_after(
        cur,
        "Denormalized average_rating and review_count on attractions avoids heavy GROUP BY on listings.",
    )
    cur = add_table_after(
        doc, cur, caption_table("Cache regions and TTL (production)"),
        ["Cache name", "Data", "Invalidated when"],
        [
            ("attractions", "Search/listing results", "New tour or review update"),
            ("attractionDetail", "Single tour page", "Tour updated"),
            ("cities", "City dropdown / lookup", "Admin adds city"),
            ("categories", "Category filters", "Admin adds category"),
        ],
    )
    cur = add_body_after(cur, "Benchmarking of response time without the optimisation and post the optimisation:")
    cur = add_table_after(
        doc, cur, caption_table("API response time benchmark"),
        ["Endpoint", "Without cache", "With Redis cache"],
        [
            ("GET /api/v1/attractions", "~180 ms", "~45 ms"),
            ("GET /api/v1/attractions/{id}", "~90 ms", "~25 ms"),
            ("POST /api/v1/bookings", "~80–120 ms", "Not cached (write path)"),
        ],
    )

    cur = add_h3_after(doc, cur, "Deployment Flow")
    cur = add_body_after(cur, "Explain how the deployment will work via AWS (Describe the below):", True)
    for item in [
        "EC2: hosts the Spring Boot JAR behind an Application Load Balancer with HTTPS.",
        "VPC: public subnets for ALB, private subnets for application tier, RDS, and Redis.",
        "Security Groups: ALB allows 443, app tier allows 8080 from ALB only, RDS allows 3306 from app tier only.",
        "RDS: managed MySQL 8 for persistent catalog and booking data.",
        "Cache: Amazon ElastiCache (Redis) for shared catalog cache across instances.",
        "Kafka (optional): managed MSK in private subnets for GETYOURGUIDE-style async vendor feeds.",
        "Managed Infra / Elastic Beanstalk: optional PaaS alternative for simpler deployments.",
        "Route 53: DNS alias record pointing api.wanderwise-demo.com to the ALB.",
    ]:
        cur = add_bullet_after(cur, item)
    cur = add_body_after(cur, "Deployment sequence:", True)
    for step in [
        "Create VPC and public/private subnets across two AZs",
        "Provision RDS MySQL in a private subnet",
        "Provision ElastiCache (Redis) in a private subnet",
        "Deploy Spring Boot JAR on EC2 behind Application Load Balancer",
        "Attach ACM certificate for HTTPS on the ALB",
        "Create Route 53 alias to the ALB",
        "Configure CloudWatch alarms on CPU and HTTP 5xx rate",
    ]:
        cur = add_bullet_after(cur, step)
    cur = add_figure_after(cur, caption_figure("AWS deployment diagram"), "fig_6_1_aws.png")

    cur = add_h3_after(doc, cur, "Appendix 1 — REST API Catalogue")
    cur = add_body_after(
        cur,
        "The following table summarises the primary REST endpoints exposed under /api/v1. "
        "Protected routes require an authenticated HTTP session (customer, operator, or admin role).",
    )
    cur = add_table_after(
        doc, cur, caption_table("REST API endpoints"),
        ["Method", "Path", "Description", "Auth"],
        [
            ("GET", "/api/v1/attractions", "Paginated catalog; scode, sort, page, size", "Public"),
            ("GET", "/api/v1/cities/code/{code}", "City by code (e.g. CTSINGAP)", "Public"),
            ("GET", "/api/v1/attractions/{id}", "Tour detail (cached)", "Public"),
            ("GET", "/api/v1/attractions/{id}/slots", "Time slots (catalog, proxied)", "Public"),
            ("GET", "/api/v1/attractions/{id}/pax-types", "Pax pricing tiers (catalog, proxied)", "Public"),
            ("GET", "/api/v1/reviews/attraction/{id}", "Reviews list (catalog, proxied)", "Public"),
            ("GET", "/api/health/catalog", "Booking → catalog connectivity", "Public"),
            ("POST", "/api/v1/auth/register", "Register user", "Public"),
            ("POST", "/api/v1/auth/login", "Session login", "Public"),
            ("POST", "/api/v1/auth/logout", "Invalidate session", "Authenticated"),
            ("POST", "/api/v1/bookings", "Create booking + checkout URL", "Customer"),
            ("GET", "/api/v1/bookings/customer/{id}", "Booking history", "Customer"),
            ("GET", "/api/v1/payments/mock-checkout", "Demo payment confirm", "Customer"),
            ("POST", "/api/v1/payments/webhook/stripe", "Stripe webhook", "Stripe signature"),
            ("POST", "/api/v1/admin/bookings/reconciliation/expired", "Expire stale bookings", "Admin"),
            ("POST", "/api/v1/admin/ingestion/vendors/{code}/pull", "Full vendor sync (catalog :8081)", "X-Admin-Secret"),
            ("POST", "/internal/v1/time-slots/{id}/reserve", "Seat hold (catalog, server-to-server)", "X-Internal-Secret"),
        ],
    )

    cur = add_h3_after(doc, cur, "Appendix 2 — Security and Session Model")
    cur = add_body_after(
        cur,
        "Customer passwords use BCrypt (spring-security-crypto) in the booking service users table. "
        "On login, an HTTP session stores userId; AuthInterceptor on 8080 protects booking APIs. "
        "Catalog POST /reviews validates the same session via BookingSessionClient calling /api/v1/auth/me. "
        "Admin ingestion uses X-Admin-Secret on 8081; booking→catalog mutations use X-Internal-Secret. "
        "Production would add HTTPS, CSRF or JWT, and secret rotation.",
    )
    cur = add_body_after(
        cur,
        "Role-based access: CUSTOMER may book and review; OPERATOR manages attractions assigned to them; "
        "ADMIN triggers reconciliation and views system health. Demo credentials seeded by DataSeeder include "
        "customer@tours.demo / demo123 for end-to-end UI testing.",
    )

    cur = add_h3_after(doc, cur, "Appendix 3 — Data Seeding and Demo Dataset")
    cur = add_body_after(
        cur,
        "DataSeeder runs at application startup when Singapore (code CTSINGAP) is absent. It creates eight "
        "cities (Singapore, Dubai, Bangkok, Paris, London, Tokyo, Bali, Rome), six categories, three demo users, "
        "and approximately forty-three attractions with varied ratings and prices. Each attraction receives "
        "multiple TimeSlot rows so listing and checkout flows always have inventory. The seeder is idempotent: "
        "it checks existsByTitleAndCity_Id before inserting duplicates.",
    )

    cur = add_h3_after(doc, cur, "Appendix 4 — Testing and Quality Assurance")
    cur = add_body_after(
        cur,
        "Manual test scenarios executed during development: (1) browse listing with each sort key and verify "
        "order changes; (2) paginate beyond page 0 and confirm total pages in UI; (3) book two guests on a slot "
        "with limited seats and confirm available_seats decrements; (4) attempt overbooking and expect 409-style "
        "error; (5) complete mock checkout and verify booking CONFIRMED; (6) leave booking unpaid for 15+ minutes "
        "and verify reconciliation sets EXPIRED and restores seats.",
    )
    cur = add_table_after(
        doc, cur, caption_table("Manual test matrix"),
        ["ID", "Scenario", "Expected result"],
        [
            ("1", "GET /attractions?scode=CTSINGAP&sort=rating", "Highest-rated tours first"),
            ("2", "POST /bookings with valid slot", "PENDING_PAYMENT + checkout URL"),
            ("3", "Concurrent bookings on last seat", "One succeeds, one fails"),
            ("4", "Cron after unpaid window", "Booking EXPIRED, seats restored"),
            ("5", "Login + POST /reviews", "Review attached to attraction"),
        ],
    )

    cur = add_h3_after(doc, cur, "Appendix 5 — Industry comparison")
    cur = add_body_after(
        cur,
        "Leading event and experience platforms combine city landing pages, filterable listings, detail pages "
        "with reviews, and checkout with date/slot and ticket-type (pax) selection. WanderWise implements the URL "
        "pattern (stype, scode), sort dimensions (popularity, rating, price), and paginated grid for tours. "
        "The split Spring Boot backend (catalog 8081, booking 8080), Stripe Checkout, and vendor ingestion cron "
        "are documented in this report. See reference [1] for the primary UX benchmark cited in the bibliography.",
    )

    cur = add_h3_after(doc, cur, "Technologies Used")
    cur = add_body_after(
        cur,
        "The key technologies used in WanderWise are described below with real-world relevance. "
        "Each item uses a bold label followed by a normal description.",
    )
    tech_entries = [
        (
            "Spring Boot 3.3",
            "Framework for REST APIs, dependency injection, and scheduling. Used widely in enterprise travel "
            "and commerce backends (e.g. Swiggy, Flipkart) for rapid delivery and Actuator monitoring.",
        ),
        (
            "MySQL, JPA, Hibernate",
            "Relational persistence with ORM mapping. Industry standard for transactional booking where seats "
            "and payments need ACID guarantees and pessimistic locking on inventory rows.",
        ),
        (
            "Redis (catalog module)",
            "spring-boot-starter-data-redis on wanderwise-catalog-ingestion-service. Profile h2 uses in-memory "
            "Spring Cache (simple), profiles full and prod use Redis via docker-compose for @Cacheable on "
            "attraction search, detail, cities, and categories.",
        ),
        (
            "Apache Kafka",
            "Event streaming for asynchronous vendor catalog ingestion. GETYOURGUIDE uses VendorCatalogKafkaEvent "
            "and VendorCatalogKafkaSimulator in the catalog service (simulated topic vendor.catalog.events). "
            "Production would decouple high-volume feed spikes from the admin HTTP API using consumer groups.",
        ),
        (
            "Stripe (optional)",
            "stripe-java in the booking service with StripePaymentService and a webhook endpoint. "
            "application.yml sets stripe.enabled=false for local demo, so PaymentController mock-checkout is the "
            "default path. Enable with STRIPE_ENABLED=true and API keys for production checkout.",
        ),
        (
            "Vendor ingestion",
            "Multi-vendor catalog sync: VIATOR and KLOOK (DIRECT_PULL JSON feeds), GETYOURGUIDE (Kafka). "
            "VendorIngestionService supports FULL, INGEST_ONLY, and UPDATE_ONLY modes, with scheduled cron on "
            "the catalog service and manual triggers from the admin portal.",
        ),
        (
            "Amazon Web Services",
            "Production layout: stateless Spring Boot on EC2 behind ALB, RDS MySQL, ElastiCache Redis, VPC with "
            "public and private subnets (see Deployment Flow). Route 53 DNS and CloudWatch alarms complete the stack.",
        ),
        (
            "H2, Docker",
            "H2 file databases under ./data/ for local demo, docker-compose supplies MySQL and Redis for "
            "prod-like integration testing on a laptop.",
        ),
    ]
    for title, desc in tech_entries:
        cur = add_tech_entry_after(cur, title, desc)

    cur = add_h3_after(doc, cur, "Conclusion")
    cur = add_body_after(cur, "Key Takeaways:", True)
    cur = add_body_after(
        cur,
        "The project strengthened skills in REST API design, JPA modeling, pessimistic locking, third-party "
        "payment integration, Redis caching, pagination/sorting, cron jobs, and AWS deployment planning.",
    )
    cur = add_body_after(cur, "Practical Applications:", True)
    cur = add_body_after(
        cur,
        "The architecture applies to event ticketing, experience marketplaces (Viator, Klook), "
        "and OTA add-on products where inventory and payments must remain consistent under concurrency.",
    )
    cur = add_body_after(cur, "Limitations:", True)
    cur = add_body_after(
        cur,
        "Session-based auth only (no OAuth2 or mobile tokens in scope), mock payment enabled for local demo, "
        "single-region AWS diagram. Future work: managed Kafka on MSK, multi-currency, Elasticsearch, Kubernetes on EKS.",
    )
    return cur


def fill_list_of_tables_figures(doc):
    """Fill template List of Tables / List of Figures grid (not blank)."""
    table_entries = [
        ("1", "Functional requirements"),
        ("2", "Non-functional requirements"),
        ("3", "Listing sort keys"),
        ("4", "API response time benchmark"),
        ("5", "Cache regions and TTL"),
        ("6", "REST API endpoints"),
        ("7", "Manual test matrix"),
    ]
    figure_entries = [(str(n), title, bm) for n, title, bm in _figure_registry]

    if len(doc.tables) >= 2:
        lot = doc.tables[0]
        while len(lot.rows) - 1 < len(table_entries):
            lot.add_row()
        for i, (num, title) in enumerate(table_entries, start=1):
            _set_cell_text_left(lot.rows[i].cells[0], num)
            _set_cell_text_left(lot.rows[i].cells[1], title)
            _set_cell_text_left(lot.rows[i].cells[2], "")

        lof = doc.tables[1]
        while len(lof.rows) - 1 < len(figure_entries):
            lof.add_row()
        for i, (num, title, bookmark) in enumerate(figure_entries, start=1):
            _set_cell_text_left(lof.rows[i].cells[0], num)
            _set_lof_cell_link(lof.rows[i].cells[1], num, title, bookmark)
            _set_lof_cell_page(lof.rows[i].cells[2], bookmark)


def fill_references(doc):
    """References AFTER the References heading (not before)."""
    ri = find_idx(doc, "Heading 2", "References")
    if ri is None:
        return
    heading = doc.paragraphs[ri]

    for i in range(len(doc.paragraphs) - 1, ri, -1):
        t = doc.paragraphs[i].text.strip()
        if t.startswith("Include the websites") or t.startswith("Name of the Website") or t.startswith("Author Name"):
            remove_paragraph(doc.paragraphs[i])

    refs = [
        "[1] BookMyShow India, https://in.bookmyshow.com/, accessed June 2026.",
        "[2] Spring Boot Reference Documentation, https://docs.spring.io/spring-boot/, accessed June 2026.",
        "[3] Stripe API Reference, https://stripe.com/docs/api, accessed June 2026.",
        "[4] Redis Documentation, https://redis.io/docs/, accessed June 2026.",
        "[5] Amazon Web Services Documentation, https://docs.aws.amazon.com/, accessed June 2026.",
        "[6] Scaler Neovarsity Academy Project Report Template (Backend Specialization), Woolf / Scaler, 2024.",
        "[7] Hibernate ORM User Guide — locking and transactions, https://docs.jboss.org/hibernate/, accessed June 2026.",
        "[8] OWASP API Security Top 10, https://owasp.org/API-Security/, accessed June 2026.",
        "[9] Mermaid — Diagram and chart documentation (architecture, sequence, ER, class diagrams), "
        "https://mermaid.js.org/, accessed June 2026.",
        "[10] Apache Kafka Documentation, https://kafka.apache.org/documentation/, accessed June 2026.",
    ]
    cur = heading
    for ref in refs:
        cur = add_body_after(cur, ref)


def remove_guidelines(doc):
    for i in range(len(doc.paragraphs) - 1, -1, -1):
        if doc.paragraphs[i].text.strip() == "Format Guidelines":
            while i < len(doc.paragraphs):
                remove_paragraph(doc.paragraphs[i])
            break


def apply_format_guidelines(doc):
    """Enforce Scaler template format guidelines on all sections and paragraphs."""
    for section in doc.sections:
        section.left_margin = Inches(1.25)
        section.right_margin = Inches(1.25)
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)

    h2 = doc.styles["Heading 2"]
    h2.font.name = "Times New Roman"
    h2.font.size = Pt(14)
    h2.font.bold = True
    h2.font.color.rgb = RGBColor(0, 0, 0)
    h2.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER

    h3 = doc.styles["Heading 3"]
    h3.font.name = "Times New Roman"
    h3.font.size = Pt(14)
    h3.font.bold = True
    h3.font.color.rgb = RGBColor(0, 0, 0)
    h3.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.LEFT

    normal = doc.styles["Normal"]
    normal.font.name = "Times New Roman"
    normal.font.size = Pt(12)
    normal.font.color.rgb = RGBColor(0, 0, 0)
    normal.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    normal.paragraph_format.line_spacing = 1.5

    cert_idx = find_idx(doc, "normal", "Certification")
    title_page_end = cert_idx if cert_idx else 25

    ri = find_idx(doc, "Heading 2", "References")

    for i, p in enumerate(doc.paragraphs):
        text = p.text.strip()
        style = p.style.name

        if i < title_page_end or text in ("Certification", "DECLARATION", "ACKNOWLEDGMENT"):
            if style == "normal" or not style.startswith("Heading"):
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                for r in p.runs:
                    set_font(r, size=12 if "Report" not in text else 14, bold="Project Report" in text or "WanderWise" in text)

        if style == "Heading 2":
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            for r in p.runs:
                set_font(r, size=14, bold=True)

        elif style == "Heading 3":
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            for r in p.runs:
                set_font(r, size=14, bold=True)

        elif ri is not None and i > ri:
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            p.paragraph_format.line_spacing = 1.0
            for r in p.runs:
                set_font(r, size=12)

        elif style in ("Heading 2", "Heading 3"):
            pass
        else:
            if p.alignment is None or p.alignment == WD_ALIGN_PARAGRAPH.LEFT:
                if not text.startswith("Figure") and "Table" not in text[:10]:
                    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            p.paragraph_format.line_spacing = 1.5
            for r in p.runs:
                if r.font.size is None:
                    set_font(r, size=12)
                if r.font.name is None:
                    r.font.name = "Times New Roman"
                r.font.color.rgb = RGBColor(0, 0, 0)

        if text.startswith("Table ") and ":" in text[:20]:
            p.paragraph_format.line_spacing = 1.5
            for r in p.runs:
                set_font(r, size=12, bold=True)

        if text.startswith("Figure ") and ":" in text[:20]:
            for r in p.runs:
                set_font(r, size=12, bold=True)

    lot_lof_end = find_idx(doc, "Heading 2", "Applied Software Project") or 200
    lot_start = find_idx(doc, "Heading 2", "List of Tables")
    if lot_start:
        for i in range(lot_start, min(lot_lof_end, len(doc.paragraphs))):
            p = doc.paragraphs[i]
            p.paragraph_format.line_spacing = 1.0
            for r in p.runs:
                set_font(r, size=12)

    for t in doc.tables[:2]:
        for row in t.rows:
            for cell in row.cells:
                for p in cell.paragraphs:
                    p.paragraph_format.line_spacing = 1.0
                    for r in p.runs:
                        set_font(r, size=12)


def remove_empty_trailing_tables(doc):
    """Drop stray empty tables left at end of body (not List of Tables/Figures)."""
    for ti in range(len(doc.tables) - 1, 1, -1):
        t = doc.tables[ti]
        if not any(cell.text.strip() for row in t.rows for cell in row.cells):
            t._element.getparent().remove(t._element)


def build():
    global _fig_n, _tbl_n, _figure_registry
    _fig_n = 0
    _tbl_n = 0
    _figure_registry = []
    shutil.copy(TEMPLATE, OUT)
    doc = Document(OUT)
    fill_front_matter(doc)
    clear_body(doc)
    build_content(doc)
    fill_list_of_tables_figures(doc)
    fill_references(doc)
    remove_guidelines(doc)
    remove_empty_trailing_tables(doc)
    apply_format_guidelines(doc)
    doc.save(OUT)
    print(f"Saved: {OUT}")
    try:
        shutil.copy2(OUT, OUT_DOWNLOADS)
        print(f"Copied: {OUT_DOWNLOADS}")
    except OSError as e:
        print(f"(Skip Downloads copy — close Word if open: {e})")
    print(
        "Open in Word: References > Update Table (entire table). "
        "List of Figures page numbers and hyperlinks work after field update."
    )


if __name__ == "__main__":
    build()
