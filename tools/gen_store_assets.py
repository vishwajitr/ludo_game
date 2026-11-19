#!/usr/bin/env python3
"""Builds Play Store graphics from raw emulator screenshots in store/raw (needs Pillow)."""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..", "store")
RAW, SHOTS = os.path.join(ROOT, "raw"), os.path.join(ROOT, "screenshots")
os.makedirs(SHOTS, exist_ok=True)
FONT = "/System/Library/Fonts/Helvetica.ttc"

# --- screenshots: Play wants the long side <= 2x the short side, so crop off the status/nav bars
def crop(name, out, top, height):
    im = Image.open(os.path.join(RAW, name + ".png")).convert("RGB")
    im.crop((0, top, im.width, top + height)).save(os.path.join(SHOTS, out + ".png"), optimize=True)
# Home and Setup have a banner ad at the bottom: keep the crop above it (store images must not show ads)
crop("1_home", "1-home", 100, 2040)
crop("2_setup", "2-setup", 100, 2040)
crop("3_game", "3-game", 100, 1920)
crop("4_game", "4-game", 100, 1920)

# --- 512x512 icon (same artwork as the adaptive launcher icon), drawn at 4x and downsampled
S = 2048
icon = Image.new("RGB", (S, S), "#2B1F5C")
d = ImageDraw.Draw(icon)
d.rounded_rectangle((S * .17, S * .21, S * .83, S * .79), radius=S * .09, fill="white")
for (cx, cy, col) in ((.36, .39, "#ED1C24"), (.64, .39, "#00A14B"), (.36, .61, "#29B0FF"), (.64, .61, "#FFDD1A")):
    d.ellipse((S * (cx - .11), S * (cy - .11), S * (cx + .11), S * (cy + .11)), fill=col)
icon.resize((512, 512), Image.LANCZOS).save(os.path.join(ROOT, "icon-512.png"), optimize=True)

# --- 1024x500 feature graphic: navy backdrop, title on the left, the board on the right
W, H = 1024, 500
fg = Image.new("RGB", (W, H), "#0B1F44")
g = ImageDraw.Draw(fg)
for y in range(H):  # subtle vertical gradient
    t = y / H
    g.line((0, y, W, y), fill=(int(11 + 10 * t), int(31 + 20 * t), int(68 + 30 * t)))
board = Image.open(os.path.join(RAW, "3_game.png")).convert("RGB").crop((42, 486, 1038, 1482)).resize((400, 400), Image.LANCZOS)
mask = Image.new("L", board.size, 0)
ImageDraw.Draw(mask).rounded_rectangle((0, 0, 400, 400), radius=24, fill=255)
shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ImageDraw.Draw(shadow).rounded_rectangle((570, 70, 970, 470), radius=24, fill=(0, 0, 0, 150))
fg.paste(shadow, (0, 0), shadow.filter(ImageFilter.GaussianBlur(14)))
fg.paste(board, (580, 50), mask)
g = ImageDraw.Draw(fg)
g.text((60, 130), "Ludo", font=ImageFont.truetype(FONT, 150, index=1), fill="white")
g.text((64, 310), "The classic board game", font=ImageFont.truetype(FONT, 38), fill="#BFD6FF")
g.text((64, 360), "2-4 players  |  Smart bots  |  Offline", font=ImageFont.truetype(FONT, 30), fill="#8FB0E8")
fg.save(os.path.join(ROOT, "feature-graphic.png"), optimize=True)
print("ok")
