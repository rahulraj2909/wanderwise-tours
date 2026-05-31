"""
Generate report diagram PNGs from Mermaid sources + UI screenshots.

Mermaid sources: docs/report_diagrams/mermaid/*.mmd
Rendered with Playwright (pip install playwright && playwright install chromium).

Run: python scripts/create_report_assets.py
Then: python scripts/generate_report.py
"""
from __future__ import annotations

import shutil
import subprocess
import tempfile
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "docs" / "report_diagrams"
MERMAID_DIR = OUT / "mermaid"

# Mermaid file → PNG consumed by generate_report.py
DIAGRAMS = [
    ("01_dev_process.mmd", "fig_1_1_dev_process.png"),
    ("02_architecture.mmd", "fig_1_2_architecture.png"),
    ("03_use_case.mmd", "fig_2_1_use_case.png"),
    ("04_class_booking.mmd", "fig_3_1_class_booking.png"),
    ("04_class_ingestion.mmd", "fig_3_2_class_ingestion.png"),
    ("05_er_catalog_booking.mmd", "fig_4_1_er.png"),
    ("08_ingestion_sequence.mmd", "fig_4_2_ingestion_sequence.png"),
    ("06_booking_sequence.mmd", "fig_5_1_sequence.png"),
    ("07_aws.mmd", "fig_6_1_aws.png"),
]

MERMAID_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.3/dist/mermaid.min.js"></script>
  <style>
    body {{ margin: 24px; background: #fff; }}
    #wrap {{ display: inline-block; }}
    .mermaid {{ font-family: "Segoe UI", Arial, sans-serif; }}
  </style>
</head>
<body>
  <div id="wrap"><pre class="mermaid">{source}</pre></div>
  <script>
    mermaid.initialize({{
      startOnLoad: false,
      theme: "neutral",
      securityLevel: "loose",
      flowchart: {{ useMaxWidth: true, htmlLabels: true }},
      sequence: {{ useMaxWidth: true, showSequenceNumbers: true }},
      er: {{ useMaxWidth: true }}
    }});
    mermaid.run({{ nodes: document.querySelectorAll(".mermaid") }});
  </script>
</body>
</html>
"""


def render_mermaid_playwright(source: str, out_path: Path) -> None:
    from playwright.sync_api import sync_playwright

    html = MERMAID_HTML.replace("{source}", source)

    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1600, "height": 1000}, device_scale_factor=2)
        page.set_content(html, wait_until="networkidle")
        page.wait_for_selector("#wrap svg", timeout=45000)
        page.wait_for_timeout(500)
        wrap = page.locator("#wrap")
        wrap.screenshot(path=str(out_path), omit_background=False)
        browser.close()


def render_mermaid_mmdc(source: str, out_path: Path) -> bool:
    """Optional: @mermaid-js/mermaid-cli if installed globally."""
    mmdc = shutil.which("mmdc")
    if not mmdc:
        return False
    with tempfile.NamedTemporaryFile(mode="w", suffix=".mmd", delete=False, encoding="utf-8") as f:
        f.write(source)
        mmd_path = Path(f.name)
    try:
        subprocess.run(
            [mmdc, "-i", str(mmd_path), "-o", str(out_path), "-b", "white", "-w", "1400"],
            check=True,
            capture_output=True,
            timeout=60,
        )
        return out_path.exists()
    except (subprocess.CalledProcessError, FileNotFoundError, subprocess.TimeoutExpired):
        return False
    finally:
        mmd_path.unlink(missing_ok=True)


def render_all_mermaid() -> None:
    MERMAID_DIR.mkdir(parents=True, exist_ok=True)
    OUT.mkdir(parents=True, exist_ok=True)

    for mmd_name, png_name in DIAGRAMS:
        mmd_path = MERMAID_DIR / mmd_name
        if not mmd_path.exists():
            print(f"  skip {png_name}: missing {mmd_path}")
            continue
        source = mmd_path.read_text(encoding="utf-8")
        out_path = OUT / png_name
        try:
            if render_mermaid_mmdc(source, out_path):
                print(f"  {png_name} (mmdc)")
                continue
        except Exception:
            pass
        try:
            render_mermaid_playwright(source, out_path)
            print(f"  {png_name} (mermaid + playwright)")
        except Exception as e:
            print(f"  FAILED {png_name}: {e}")


def _resolve_tour_id(page, base="http://localhost:8080"):
    for tid in (1, 16, 2, 3):
        try:
            page.goto(f"{base}/checkout.html?id={tid}", wait_until="domcontentloaded", timeout=12000)
            page.wait_for_selector("#book-form, .pax-list, .pax-qty, select", timeout=8000)
            return tid
        except Exception:
            continue
    return 1


def capture_screenshots() -> None:
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        print("  (skip screenshots: pip install playwright && playwright install chromium)")
        return

    base = "http://localhost:8080"
    static_pages = [
        ("screenshot_home.png", f"{base}/"),
        ("screenshot_listing.png", f"{base}/listing.html?stype=CITY&scode=CTSINGAP&sort=popularity"),
        ("screenshot_login.png", f"{base}/login.html"),
    ]

    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(viewport={"width": 1280, "height": 900})
        page = context.new_page()

        for name, url in static_pages:
            try:
                page.goto(url, wait_until="networkidle", timeout=20000)
                page.screenshot(path=str(OUT / name), full_page=True)
                print(f"  {name}")
            except Exception as e:
                print(f"  skip {name}: {e}")

        tour_id = 1
        for name, path_suffix in [
            ("screenshot_tour.png", "/tour.html?id="),
            ("screenshot_reviews.png", "/reviews.html?id="),
        ]:
            try:
                page.goto(f"{base}{path_suffix}{tour_id}", wait_until="networkidle", timeout=20000)
                page.screenshot(path=str(OUT / name), full_page=True)
                print(f"  {name}")
            except Exception as e:
                print(f"  skip {name}: {e}")

        try:
            page.goto(f"{base}/login.html", wait_until="networkidle", timeout=15000)
            page.fill("#email", "customer@tours.demo")
            page.fill("#password", "demo123")
            page.click("#login-form button[type=submit]")
            page.wait_for_timeout(1500)
            tour_id = _resolve_tour_id(page, base)
            page.goto(f"{base}/checkout.html?id={tour_id}", wait_until="networkidle", timeout=20000)
            page.wait_for_selector("#book-form, .pax-list, .pax-qty, select", timeout=12000)
            page.wait_for_timeout(800)
            page.screenshot(path=str(OUT / "screenshot_checkout.png"), full_page=True)
            print(f"  screenshot_checkout.png (tour id={tour_id})")
        except Exception as e:
            print(f"  skip screenshot_checkout.png: {e}")

        try:
            tour_id = _resolve_tour_id(page, base)
            page.goto(f"{base}/checkout.html?id={tour_id}", wait_until="networkidle", timeout=20000)
            page.wait_for_selector("#book-form, .pax-qty, select", timeout=12000)
            page.wait_for_timeout(500)
            # Select first slot if present
            slot_sel = page.query_selector("select[name=timeSlotId], #time-slot, select")
            if slot_sel:
                options = page.eval_on_selector_all(
                    "select option",
                    "els => els.map(e => e.value).filter(v => v && v !== '')",
                )
                if options:
                    slot_sel.select_option(options[0])
            page.click("#book-form button[type=submit], #btn-submit")
            page.wait_for_url("**/payment.html**", timeout=25000)
            page.wait_for_selector("h2, #btn-pay, #content", timeout=12000)
            page.wait_for_timeout(800)
            page.screenshot(path=str(OUT / "screenshot_payment.png"), full_page=False)
            print("  screenshot_payment.png (after checkout to payment.html)")
        except Exception as e:
            print(f"  skip screenshot_payment.png: {e}")

        browser.close()


def copy_attached_screenshots() -> None:
    """Use latest Cursor-attached UI captures when present."""
    assets = Path(r"C:\Users\USER\.cursor\projects\c-Users-USER-Downloads-SCALAR-PROJECT\assets")
    mapping = {
        "image-1019bac8-3128-4c12-888f-65c7b90c0884.png": "screenshot_home.png",
        "image-3d92bb17-6a24-4fa5-a02c-a5fc88dee2dc.png": "screenshot_listing.png",
    }
    for src_suffix, dest_name in mapping.items():
        matches = list(assets.glob(f"*{src_suffix}")) if assets.exists() else []
        if matches:
            shutil.copy2(matches[0], OUT / dest_name)
            print(f"  {dest_name} (from attached image)")


if __name__ == "__main__":
    print("Rendering Mermaid diagrams...")
    render_all_mermaid()
    copy_attached_screenshots()
    print("Capturing UI screenshots (catalog :8081 then booking :8080 must be running)...")
    try:
        capture_screenshots()
    except Exception as e:
        print(f"  (screenshots skipped: {e})")
    print("Done:", OUT)
    print("Edit .mmd files in docs/report_diagrams/mermaid/ then re-run this script.")
