#!/usr/bin/env python3
"""Generates the tiny WAV effects and the music loop in app/src/main/res/raw (stdlib only)."""
import math, random, struct, wave, os

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
random.seed(7)

def write(name, samples, rate, bits=16):
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1); w.setsampwidth(bits // 8); w.setframerate(rate)
        if bits == 16:
            w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 30000)) for s in samples))
        else:
            w.writeframes(bytes(int(128 + max(-1, min(1, s)) * 100) for s in samples))

def tone(freq, dur, rate, decay=6.0, vol=1.0):
    return [vol * math.sin(2 * math.pi * freq * i / rate) * math.exp(-decay * i / (dur * rate)) for i in range(int(dur * rate))]

def silence(dur, rate): return [0.0] * int(dur * rate)

R = 22050
# roll: three dry rattles
roll = []
for _ in range(3):
    roll += [random.uniform(-1, 1) * math.exp(-30 * i / (0.07 * R)) * 0.6 for i in range(int(0.07 * R))] + silence(0.04, R)
write("roll", roll, R)
# step: short wooden tock
write("step", tone(520, 0.07, R, 40, 0.9), R)
# capture: falling sweep with noise
cap = [(math.sin(2 * math.pi * (700 - 500 * i / (0.3 * R)) * i / R) * 0.7 + random.uniform(-.3, .3)) * math.exp(-5 * i / (0.3 * R)) for i in range(int(0.3 * R))]
write("capture", cap, R)
# home: rising C-E-G
home = []
for f in (523.25, 659.25, 783.99): home += tone(f, 0.14, R, 5, 0.8)
write("home", home, R)
# win: little fanfare
win = []
for f, d in ((523.25, .15), (659.25, .15), (783.99, .15), (1046.5, .5)): win += tone(f, d, R, 3, 0.8)
write("win", win, R)
# music: 12 s pentatonic arpeggio over a soft pad, 8 kHz 8-bit
MR = 8000; notes = [261.63, 293.66, 329.63, 392.0, 440.0]; music = []
seq = [0, 2, 4, 2, 1, 3, 4, 3, 0, 2, 3, 2, 1, 2, 4, 1]
for n in seq:
    music += [s * .5 for s in tone(notes[n], 0.75, MR, 2.5)]
pad = [0.15 * math.sin(2 * math.pi * 130.81 * i / MR) + 0.1 * math.sin(2 * math.pi * 196.0 * i / MR) for i in range(len(music))]
write("music", [m + p for m, p in zip(music, pad)], MR, bits=8)
