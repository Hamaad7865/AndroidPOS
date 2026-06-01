import glob, os
from PIL import Image
import pillow_heif

pillow_heif.register_heif_opener()

src_dir = os.path.dirname(os.path.abspath(__file__))
out_dir = os.path.join(src_dir, "converted")
os.makedirs(out_dir, exist_ok=True)

MAX_EDGE = 1400

for path in sorted(glob.glob(os.path.join(src_dir, "*.HEIC"))):
    name = os.path.splitext(os.path.basename(path))[0]
    img = Image.open(path).convert("RGB")
    w, h = img.size
    scale = min(1.0, MAX_EDGE / max(w, h))
    if scale < 1.0:
        img = img.resize((int(w * scale), int(h * scale)), Image.LANCZOS)
    out = os.path.join(out_dir, f"{name}.jpg")
    img.save(out, "JPEG", quality=82)
    print(f"{name}.jpg  {img.size[0]}x{img.size[1]}  {os.path.getsize(out)//1024}KB")
print("DONE")
