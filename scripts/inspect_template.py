from docx import Document

path = r"c:\Users\USER\Downloads\Scaler Neovarsity  Academy Project Report Template (Backend Specialization).docx"
d = Document(path)
for i in range(180, 420):
    p = d.paragraphs[i]
    t = p.text.strip()
    if t:
        print(f"{i:3} [{p.style.name:12}] {t[:100]}")
