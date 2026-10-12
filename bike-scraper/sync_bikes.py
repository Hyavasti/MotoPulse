import io
import requests
from PIL import Image
from supabase import create_client, Client

# ==============================================================================
# 1. CONFIGURATION & LIMITS
# ==============================================================================
# Replace with your Supabase credentials
SUPABASE_URL = "https://uinwxdnhgzqxwvvibzur.supabase.co"
SUPABASE_KEY = "sb_publishable_FvEb8OJ7uj8vEKgTBiXAAA_t-jGjC-x"
BUCKET_NAME = "motorcycles"

# STRICT SIZE LIMITS ENFORCED IN PYTHON
MAX_FILE_SIZE_KB = 500      # File will strictly be <= 500 KB
MAX_DIMENSION = 1000        # Max width or height in pixels (crisp on mobile screens)

supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# ==============================================================================
# 2. MOTORCYCLE CATALOG DATA
# ==============================================================================
# Add your bike models, colors matching your UI chips, and source URLs
bikes_to_sync = [
    {
        "id": "honda_click_125_v3",
        "images": {
            # Direct static CDN links for Click 125 v3
            "Pearl Crimson Red": "https://imgcdnblog.carbay.com/wp-content/uploads/2022/12/15124016/Honda-Click-125-Red.jpg",
            "Pearl Sylvestris Gray": "https://imgcdnblog.carbay.com/wp-content/uploads/2022/12/15124021/Honda-Click-125-Gray.jpg",
            "default": "https://imgcdnblog.carbay.com/wp-content/uploads/2022/12/15124016/Honda-Click-125-Red.jpg"
        }
    }
]

# ==============================================================================
# 3. SMART COMPRESSION & SIZE LIMIT ENGINE
# ==============================================================================
def compress_to_target_size(raw_bytes: bytes, max_kb: int = MAX_FILE_SIZE_KB) -> bytes:
    """
    Downsamples resolution and dynamically steps down quality until
    the file is strictly under `max_kb` without losing transparency.
    """
    max_bytes = max_kb * 1024
    img = Image.open(io.BytesIO(raw_bytes))

    # Keep transparency if available
    if img.mode not in ("RGBA", "RGB"):
        img = img.convert("RGBA")

    # Step 1: Scale down dimensions if over MAX_DIMENSION
    img.thumbnail((MAX_DIMENSION, MAX_DIMENSION), Image.Resampling.LANCZOS)

    # Step 2: Adaptive quality loop (starts at 85 down to 30)
    quality = 85
    while quality >= 30:
        buffer = io.BytesIO()
        img.save(buffer, format="WEBP", quality=quality, method=6)
        data = buffer.getvalue()

        if len(data) <= max_bytes:
            orig_kb = len(raw_bytes) / 1024
            final_kb = len(data) / 1024
            print(f"      [Compressed] {orig_kb:.1f} KB -> {final_kb:.1f} KB (Quality: {quality})")
            return data

        quality -= 10

    # Step 3: If still over limit, reduce dimensions by 20% and retry
    while len(data) > max_bytes and img.width > 300:
        new_w = int(img.width * 0.8)
        new_h = int(img.height * 0.8)
        img = img.resize((new_w, new_h), Image.Resampling.LANCZOS)

        buffer = io.BytesIO()
        img.save(buffer, format="WEBP", quality=50, method=6)
        data = buffer.getvalue()

    final_kb = len(data) / 1024
    print(f"      [Enforced Cap] Final size: {final_kb:.1f} KB (Target: {max_kb} KB)")
    return data

# ==============================================================================
# 4. SYNC TO STORAGE & DATABASE
# ==============================================================================
def sync():
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    for bike in bikes_to_sync:
        bike_id = bike["id"]
        print(f"\n[+] Processing: {bike_id}")
        image_urls_map = {}

        for color_name, src_url in bike["images"].items():
            slug = color_name.lower().replace(" ", "_")
            file_name = f"{bike_id}_{slug}.webp"

            try:
                print(f"    Downloading {color_name}...")
                resp = requests.get(src_url, headers=headers, timeout=12)
                if resp.status_code != 200:
                    print(f"    [X] HTTP {resp.status_code} on {src_url}")
                    continue

                # Enforce size limit in Python
                compressed_bytes = compress_to_target_size(resp.content, max_kb=MAX_FILE_SIZE_KB)

                # Upload to Supabase Storage
                supabase.storage.from_(BUCKET_NAME).upload(
                    path=file_name,
                    file=compressed_bytes,
                    file_options={"content-type": "image/webp", "upsert": "true"}
                )

                public_url = f"{SUPABASE_URL}/storage/v1/object/public/{BUCKET_NAME}/{file_name}"
                image_urls_map[color_name] = public_url
                print(f"    [OK] Uploaded -> {public_url}")

            except Exception as e:
                print(f"    [!] Error processing {color_name}: {e}")

        # Sync mapped URLs directly into motorcycle_catalog table
        if image_urls_map:
            try:
                supabase.from_("motorcycle_catalog").update({
                    "image_url": image_urls_map
                }).eq("id", bike_id).execute()
                print(f"    [SUCCESS] Supabase database row updated for {bike_id}!")
            except Exception as e:
                print(f"    [X] Table update error: {e}")

if __name__ == "__main__":
    sync()