import zipfile
import re
from pathlib import Path

path = Path(r"c:\Users\USER\Downloads\Scaler Neovarsity  Academy Project Report Template (Backend Specialization).docx")
out = Path(__file__).parent.parent / "docs" / "TEMPLATE_STRUCTURE.md"

with zipfile.ZipFile(path) as z:
    xml = z.read("word/document.xml").decode("utf-8")

text = re.sub(r"</w:p>", "\n", xml)
text = re.sub(r"<[^>]+>", "", text)
for old, new in [("&quot;", '"'), ("&amp;", "&"), ("&lt;", "<"), ("&gt;", ">")]:
    text = text.replace(old, new)

lines = [l.strip() for l in text.split("\n") if l.strip()]
out.write_text("\n".join(f"{i+1}. {line}" for i, line in enumerate(lines)), encoding="utf-8")
print(f"Wrote {len(lines)} lines to {out}")
