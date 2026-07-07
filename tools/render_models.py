#!/usr/bin/env python3
"""Render Program 7's entity models to PNG previews (no Minecraft needed).

Mirrors the cuboid definitions from the Java model classes and rasterizes
them with the real textures into docs/previews/*.png so model changes can
be reviewed straight from the repo. Keep the PART specs in sync with the
model classes when geometry changes.

Usage: python3 tools/render_models.py
"""
import math
import os
import struct
import sys
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "common/src/main/resources/assets/program7/textures")
OUT = os.path.join(ROOT, "docs/previews")

CANVAS = 560
MARGIN = 48
BG = (30, 33, 37, 255)
CAM_YAW = math.radians(-35.0)
CAM_PITCH = math.radians(25.0)
# Spin the model so its front (-z, where the sensor strips live) faces camera.
MODEL_YAW = math.radians(160.0)
# Minecraft-style face lighting, biased bright like an entity in daylight.
FACE_LIGHT = {"up": 1.3, "down": 0.5, "north": 1.05, "south": 0.85, "west": 0.75, "east": 0.75}


# ---------------------------------------------------------------- PNG I/O

def read_png(path):
    """Minimal reader for the RGBA8, filter-0 PNGs this repo generates."""
    with open(path, "rb") as f:
        data = f.read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    pos, width, height, idat = 8, 0, 0, b""
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        tag = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        if tag == b"IHDR":
            width, height, depth, color = struct.unpack(">IIBB", body[:10])
            assert depth == 8 and color == 6, f"unsupported PNG format: {path}"
        elif tag == b"IDAT":
            idat += body
        pos += 12 + length
    raw = zlib.decompress(idat)
    stride = width * 4 + 1
    pixels = []
    for y in range(height):
        row = raw[y * stride:(y + 1) * stride]
        assert row[0] == 0, f"unsupported PNG filter in {path}"
        pixels.append([tuple(row[1 + x * 4:5 + x * 4]) for x in range(width)])
    return pixels


def write_png(path, w, h, px):
    raw = b""
    for y in range(h):
        raw += b"\x00" + b"".join(struct.pack("4B", *px[y][x]) for x in range(w))
    def chunk(tag, body):
        c = tag + body
        return struct.pack(">I", len(body)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n"
                + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw))
                + chunk(b"IEND", b""))
    print("wrote", os.path.relpath(path, ROOT))


# ---------------------------------------------------------------- geometry

def rot_y(p, angle):
    x, y, z = p
    c, s = math.cos(angle), math.sin(angle)
    return (x * c - z * s, y, x * s + z * c)


def rot_x(p, angle):
    x, y, z = p
    c, s = math.cos(angle), math.sin(angle)
    return (x, y * c - z * s, y * s + z * c)




def rot_z(p, angle):
    x, y, z = p
    c, s = math.cos(angle), math.sin(angle)
    return (x * c - y * s, x * s + y * c, z)
def box_faces(part):
    """Faces of one cuboid: (name, corner_fn(a,b)->model xyz, uv rect)."""
    ox, oy, oz = part["origin"]
    w, h, d = part["size"]
    x1, y1, z1 = ox + w, oy + h, oz + d
    if "uv_all" in part:
        u0, v0, u1, v1 = part["uv_all"]
        rects = {name: (u0, v0, u1, v1) for name in FACE_LIGHT}
    else:
        u, v = part["uv"]
        rects = {
            "up": (u + d, v, u + d + w, v + d),
            "down": (u + d + w, v, u + d + 2 * w, v + d),
            "west": (u, v + d, u + d, v + d + h),
            "north": (u + d, v + d, u + d + w, v + d + h),
            "east": (u + d + w, v + d, u + 2 * d + w, v + d + h),
            "south": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
        }
    corner = {
        # a = horizontal texture axis (0..1), b = vertical texture axis (down).
        "up": lambda a, b: (ox + a * w, oy, oz + b * d),
        "down": lambda a, b: (ox + a * w, y1, oz + b * d),
        "west": lambda a, b: (ox, oy + b * h, oz + a * d),
        "north": lambda a, b: (ox + a * w, oy + b * h, oz),
        "east": lambda a, b: (x1, oy + b * h, oz + a * d),
        "south": lambda a, b: (ox + a * w, oy + b * h, z1),
    }
    return [(name, corner[name], rects[name]) for name in corner]


def project(point):
    """Model space (y down) -> camera space; returns (sx, sy_down, depth)."""
    x, y, z = point
    p = (x, -y, z)  # world: y up
    p = rot_y(p, MODEL_YAW + CAM_YAW)
    p = rot_x(p, CAM_PITCH)
    return (p[0], -p[1], p[2])


def render(name, texture_path, parts):
    texture = read_png(texture_path)
    tex_h = len(texture)
    tex_w = len(texture[0])

    # Gather projected faces.
    faces = []
    for part in parts:
        ops = part.get("ops", [])
        for face_name, corner, (u0, v0, u1, v1) in box_faces(part):
            pts = []
            for a, b in ((0, 0), (1, 0), (0, 1)):
                p = corner(a, b)
                for op in ops:
                    if op[0] == "roty":
                        p = rot_y(p, math.radians(op[1]))
                    elif op[0] == "rotx":
                        p = rot_x(p, math.radians(op[1]))
                    elif op[0] == "rotz":
                        p = rot_z(p, math.radians(op[1]))
                    elif op[0] == "move":
                        p = (p[0] + op[1][0], p[1] + op[1][1], p[2] + op[1][2])
                pts.append(project(p))
            faces.append((face_name, pts, (u0, v0, u1, v1)))

    # Fit to canvas (also consider the fourth corner of each parallelogram).
    xs, ys = [], []
    for _, (p00, p10, p01), _ in faces:
        p11 = (p10[0] + p01[0] - p00[0], p10[1] + p01[1] - p00[1])
        for px_, py_ in (p00[:2], p10[:2], p01[:2], p11):
            xs.append(px_)
            ys.append(py_)
    span = max(max(xs) - min(xs), max(ys) - min(ys)) or 1.0
    scale = (CANVAS - 2 * MARGIN) / span
    off_x = (CANVAS - (max(xs) + min(xs)) * scale) / 2.0
    off_y = (CANVAS - (max(ys) + min(ys)) * scale) / 2.0

    canvas = [[BG for _ in range(CANVAS)] for _ in range(CANVAS)]
    zbuf = [[-1e9 for _ in range(CANVAS)] for _ in range(CANVAS)]

    for face_name, (p00, p10, p01), (u0, v0, u1, v1) in faces:
        c00 = (p00[0] * scale + off_x, p00[1] * scale + off_y, p00[2])
        c10 = (p10[0] * scale + off_x, p10[1] * scale + off_y, p10[2])
        c01 = (p01[0] * scale + off_x, p01[1] * scale + off_y, p01[2])
        ux, uy, uz = c10[0] - c00[0], c10[1] - c00[1], c10[2] - c00[2]
        vx, vy, vz = c01[0] - c00[0], c01[1] - c00[1], c01[2] - c00[2]
        det = ux * vy - uy * vx
        if abs(det) < 1e-6:
            continue  # edge-on
        min_x = max(0, int(min(c00[0], c10[0], c01[0], c10[0] + vx)) - 1)
        max_x = min(CANVAS - 1, int(max(c00[0], c10[0], c01[0], c10[0] + vx)) + 1)
        min_y = max(0, int(min(c00[1], c10[1], c01[1], c10[1] + vy)) - 1)
        max_y = min(CANVAS - 1, int(max(c00[1], c10[1], c01[1], c10[1] + vy)) + 1)
        light = FACE_LIGHT[face_name]
        for py_ in range(min_y, max_y + 1):
            for px_ in range(min_x, max_x + 1):
                sx, sy = px_ + 0.5 - c00[0], py_ + 0.5 - c00[1]
                a = (sx * vy - sy * vx) / det
                b = (ux * sy - uy * sx) / det
                if a < 0.0 or a >= 1.0 or b < 0.0 or b >= 1.0:
                    continue
                depth = c00[2] + a * uz + b * vz
                if depth <= zbuf[py_][px_]:
                    continue
                tx = min(tex_w - 1, int(u0 + a * (u1 - u0)))
                ty = min(tex_h - 1, int(v0 + b * (v1 - v0)))
                r, g, bl, alpha = texture[ty][tx]
                if alpha == 0:
                    continue
                zbuf[py_][px_] = depth
                canvas[py_][px_] = (min(255, int(r * light)), min(255, int(g * light)),
                        min(255, int(bl * light)), 255)

    os.makedirs(OUT, exist_ok=True)
    write_png(os.path.join(OUT, f"{name}.png"), CANVAS, CANVAS, canvas)


# ------------------------------------------------------- model definitions
# Keep in sync with the Java model classes. Each part carries an "ops" list
# applied in order to its local-space corners: ("roty", deg), ("rotz", deg),
# ("move", (x, y, z)). This supports rotated arms with spinning rotor
# children and rolled armor panels.

def quad_rotor_parts():
    parts = [
        {"origin": (-5, -1.5, -4), "size": (10, 3, 8), "uv": (0, 0), "ops": [("move", (0, 18, 0))]},
        {"origin": (-4, -3.5, -3), "size": (8, 2, 6), "uv": (0, 12), "ops": [("move", (0, 18, 0))]},
        {"origin": (-3, -1, -6), "size": (6, 2, 2), "uv": (0, 21), "ops": [("move", (0, 18, 0))]},
        {"origin": (-3, 1.5, -3), "size": (6, 2, 6), "uv": (0, 26), "ops": [("move", (0, 18, 0))]},
        # rolled armor cheeks (roll = z rotation, matches ModelTransform.of roll 0.30 rad)
        {"origin": (-1, -1.5, -3.5), "size": (1, 3, 7), "uv": (29, 12),
         "ops": [("rotz", 17.2), ("move", (-5, 18, 0))]},
        {"origin": (0, -1.5, -3.5), "size": (1, 3, 7), "uv": (29, 12),
         "ops": [("rotz", -17.2), ("move", (5, 18, 0))]},
    ]
    arms = (((-3.5, -2.5), -45, 20), ((3.5, -2.5), 45, -35), ((-3.5, 2.5), -135, 50), ((3.5, 2.5), 135, -10))
    for (ax, az), yaw, spin in arms:
        base = [("roty", yaw), ("move", (ax, 16.5, az))]
        parts.append({"origin": (-0.5, -0.5, -5), "size": (1, 1, 5), "uv": (37, 0), "ops": list(base)})
        parts.append({"origin": (-1, -1.5, -6), "size": (2, 2, 2), "uv": (37, 7), "ops": list(base)})
        parts.append({"origin": (-3.5, -0.5, -3.5), "size": (7, 1, 7), "uv": (0, 35),
                      "ops": [("roty", spin), ("move", (0, -2, -5))] + list(base)})
    return parts


HARVESTER_PARTS = [
    {"origin": (-5, -2.5, -4), "size": (10, 5, 8), "uv": (0, 0), "ops": [("move", (0, 19.5, 0))]},
    {"origin": (-2, -1, -5), "size": (4, 2, 1), "uv": (0, 14), "ops": [("move", (0, 19.5, 0))]},
    {"origin": (-3, -4.5, -2), "size": (6, 2, 4), "uv": (0, 18), "ops": [("move", (0, 19.5, 0))]},
] + [
    {"origin": (-1.5, -1.5, -1.5), "size": (3, 3, 3), "uv": (37, 0), "ops": [("move", (x, 22.5, z))]}
    for x in (-5, 5) for z in (-2.5, 2.5)
]

PROBE_CORE_PARTS = [
    {"origin": (-8, 0, -8), "size": (16, 16, 16), "uv_all": (0, 0, 16, 16), "ops": []},
]

GROUND_DRONE_PARTS = [
    # body: hull + forward sensor strip (pivot 0,19,0)
    {"origin": (-4.5, -2, -6.5), "size": (9, 4, 13), "uv": (0, 0), "ops": [("move", (0, 19, 0))]},
    {"origin": (-2, -1.5, -7), "size": (4, 1, 1), "uv": (24, 25), "ops": [("move", (0, 19, 0))]},
    # angled side skirts (Java roll -0.20/+0.20 rad at body-relative pivots ±4.5,0.5,0)
    {"origin": (-0.5, -1.5, -5.5), "size": (1, 3, 11), "uv": (0, 17),
     "ops": [("rotz", -11.5), ("move", (4.5, 19.5, 0))]},
    {"origin": (-0.5, -1.5, -5.5), "size": (1, 3, 11), "uv": (0, 17),
     "ops": [("rotz", 11.5), ("move", (-4.5, 19.5, 0))]},
    # turret (body child at 0,-2,-1), posed slightly traversed for the preview
    {"origin": (-2.5, -3, -2.5), "size": (5, 3, 5), "uv": (24, 17),
     "ops": [("roty", -8), ("move", (0, 17, -1))]},
    {"origin": (-0.5, -2.5, -7.5), "size": (1, 1, 5), "uv": (44, 17),
     "ops": [("roty", -8), ("move", (0, 17, -1))]},
] + [
    {"origin": (-1.5, -1.5, -1.5), "size": (3, 3, 3), "uv": (46, 25), "ops": [("move", (x, 22.5, z))]}
    for x in (-4.5, 4.5) for z in (-4, 4)
]

AUTOGUN_PARTS = [
    # v3 open frame: ground pad + central column (pivot 0,24,0)
    {"origin": (-3, -1, -3), "size": (6, 1, 6), "uv": (0, 0), "ops": [("move", (0, 24, 0))]},
    {"origin": (-1, -10, -1), "size": (2, 9, 2), "uv": (0, 8), "ops": [("move", (0, 24, 0))]},
] + [
    # four legs splayed from one point (Java pitch 0.40, yaw pi/4 + i*pi/2)
    {"origin": (-0.5, 0, -0.5), "size": (1, 7, 1), "uv": (9, 8),
     "ops": [("rotx", 22.9), ("roty", 45 + i * 90), ("move", (0, 16, 0))]}
    for i in range(4)
] + [
    # head (base child at 0,-11,0 -> absolute y 13), posed mid-pan
    {"origin": origin, "size": size, "uv": uv, "ops": [("roty", 25), ("move", (0, 13, 0))]}
    for origin, size, uv in (
        ((-2.5, -1.5, -1), (1, 3, 2), (14, 8)),
        ((1.5, -1.5, -1), (1, 3, 2), (14, 8)),
        ((-1.5, -1.5, -4.5), (3, 3, 7), (20, 8)),
        ((-0.5, -0.5, -10.5), (1, 1, 6), (40, 8)),
        ((-1, -1, -11.5), (2, 2, 1), (40, 16)),
        ((1.5, -0.5, -2), (2, 4, 3), (46, 16)),
        ((-1, -3.5, -2.5), (2, 1, 2), (0, 20)),
    )
]


def quad_arms(pivots, arm_len, arm_uv, pod_uv, rotor_size, rotor_uv):
    """Four corner rotor arms matching the shared Java construction."""
    parts = []
    yaw_for = {(-1, -1): -45, (1, -1): 45, (-1, 1): -135, (1, 1): 135}
    spins = (20, -35, 50, -10)
    for i, (ax, ay, az) in enumerate(pivots):
        yaw = yaw_for[(1 if ax > 0 else -1, 1 if az > 0 else -1)]
        base = [("roty", yaw), ("move", (ax, ay, az))]
        parts.append({"origin": (-0.5, -0.5, -arm_len), "size": (1, 1, arm_len),
                      "uv": arm_uv, "ops": list(base)})
        parts.append({"origin": (-1, -1.5, -arm_len - 1), "size": (2, 2, 2),
                      "uv": pod_uv, "ops": list(base)})
        half = rotor_size / 2.0
        parts.append({"origin": (-half, -0.5, -half), "size": (rotor_size, 1, rotor_size),
                      "uv": rotor_uv,
                      "ops": [("roty", spins[i]), ("move", (0, -2, -arm_len))] + list(base)})
    return parts


LOGISTICS_PARTS = [
    {"origin": (-3.5, -1.5, -3), "size": (7, 3, 6), "uv": (0, 0), "ops": [("move", (0, 16, 0))]},
    {"origin": (-1.5, -0.5, -4), "size": (3, 1, 1), "uv": (28, 0), "ops": [("move", (0, 16, 0))]},
    # underslung cargo crate (body child at 0,1.5,0)
    {"origin": (-2.5, 0, -2.5), "size": (5, 4, 5), "uv": (0, 10), "ops": [("move", (0, 17.5, 0))]},
] + quad_arms([(x, 15, z) for x in (-3, 3) for z in (-2.5, 2.5)], 4, (37, 0), (37, 7), 7, (0, 20))

HAULER_PARTS = [
    {"origin": (-4, -2, -5), "size": (8, 4, 10), "uv": (0, 0), "ops": [("move", (0, 18.5, 0))]},
    {"origin": (-2.5, -6, -1), "size": (5, 4, 5), "uv": (0, 15), "ops": [("move", (0, 18.5, 0))]},
    {"origin": (-2, -1, -5.5), "size": (4, 2, 1), "uv": (21, 15), "ops": [("move", (0, 18.5, 0))]},
] + [
    {"origin": (-1.5, -1.5, -1.5), "size": (3, 3, 3), "uv": (37, 0), "ops": [("move", (x, 21.5, z))]}
    for x in (-4, 4) for z in (-3.5, 3.5)
]

MEDIUM_ATTACK_PARTS = [
    {"origin": (-4.5, -2, -5), "size": (9, 4, 10), "uv": (0, 0), "ops": [("move", (0, 17, 0))]},
    {"origin": (-1.5, 0, -7), "size": (3, 2, 4), "uv": (0, 15), "ops": [("move", (0, 17, 0))]},
    {"origin": (-0.5, 0.5, -10), "size": (1, 1, 3), "uv": (15, 15), "ops": [("move", (0, 17, 0))]},
    {"origin": (-2.5, -1.5, -5.5), "size": (5, 1, 1), "uv": (24, 15), "ops": [("move", (0, 17, 0))]},
] + quad_arms([(x, 15, z) for x in (-4, 4) for z in (-4, 4)], 5, (38, 19), (38, 26), 8, (0, 32))

SNIPER_PARTS = [
    {"origin": (-2.5, -2, -3.5), "size": (5, 4, 7), "uv": (0, 0), "ops": [("move", (0, 14, 0))]},
    {"origin": (-0.5, -0.5, -13), "size": (1, 1, 10), "uv": (0, 12), "ops": [("move", (0, 14, 0))]},
    {"origin": (-0.5, -2, -8), "size": (1, 1, 3), "uv": (24, 12), "ops": [("move", (0, 14, 0))]},
    # stabilizer fins (Java roll -0.45/+0.45 at body-relative pivots ±2.5,0.5,2)
    {"origin": (-0.5, -1.5, -1.5), "size": (1, 3, 3), "uv": (33, 12),
     "ops": [("rotz", -25.8), ("move", (2.5, 14.5, 2))]},
    {"origin": (-0.5, -1.5, -1.5), "size": (1, 3, 3), "uv": (33, 12),
     "ops": [("rotz", 25.8), ("move", (-2.5, 14.5, 2))]},
] + quad_arms([(x, 12, z) for x in (-2.5, 2.5) for z in (-2.5, 2.5)], 4, (42, 0), (42, 6), 7, (0, 24))

# carriage children live at absolute (0,23,0); posed traversed 15deg for the preview
_MORTAR_CARRIAGE = [("roty", 15), ("move", (0, 23, 0))]
MORTAR_PARTS = [
    {"origin": (-3.5, -1, -3.5), "size": (7, 1, 7), "uv": (0, 0), "ops": [("move", (0, 24, 0))]},
    {"origin": (2.5, -2, -2), "size": (2, 2, 4), "uv": (18, 10), "ops": list(_MORTAR_CARRIAGE)},
    {"origin": (3, -4, -1), "size": (1, 2, 1), "uv": (31, 10), "ops": list(_MORTAR_CARRIAGE)},
    {"origin": (-3, -1, 2), "size": (2, 1, 2), "uv": (36, 10), "ops": list(_MORTAR_CARRIAGE)},
    # tube leaned back for high-angle fire (Java pitch -0.5 at carriage-child pivot 0,0,1)
    {"origin": (-1.5, -9, -1.5), "size": (3, 9, 3), "uv": (0, 10),
     "ops": [("rotx", -28.6), ("move", (0, 0, 1))] + _MORTAR_CARRIAGE},
    {"origin": (-0.5, -5, -0.5), "size": (1, 5, 1), "uv": (13, 10),
     "ops": [("rotx", -20), ("move", (1.5, 0, -1.5))] + _MORTAR_CARRIAGE},
    {"origin": (-0.5, -5, -0.5), "size": (1, 5, 1), "uv": (13, 10),
     "ops": [("rotx", -20), ("move", (-1.5, 0, -1.5))] + _MORTAR_CARRIAGE},
]

SHELL_PARTS = [
    {"origin": (-1, -2, -1), "size": (2, 3, 2), "uv": (0, 0), "ops": [("move", (0, 8, 0))]},
    {"origin": (-0.5, 1, -0.5), "size": (1, 1, 1), "uv": (8, 0), "ops": [("move", (0, 8, 0))]},
]

MINING_PARTS = [
    {"origin": (-4, -2, -4), "size": (8, 4, 8), "uv": (0, 0), "ops": [("move", (0, 17, 0))]},
    {"origin": (-1.5, -1, -7), "size": (3, 2, 3), "uv": (0, 15), "ops": [("move", (0, 17, 0))]},
    {"origin": (-0.5, -0.5, -9), "size": (1, 1, 2), "uv": (15, 15), "ops": [("move", (0, 17, 0))]},
    {"origin": (-2, 2, -2), "size": (4, 2, 4), "uv": (24, 15), "ops": [("move", (0, 17, 0))]},
    {"origin": (-1.5, -0.5, -4.5), "size": (3, 1, 1), "uv": (42, 15), "ops": [("move", (0, 17, 0))]},
] + quad_arms([(x, 15, z) for x in (-3.5, 3.5) for z in (-3.5, 3.5)], 5, (40, 0), (40, 7), 8, (0, 32))

TRANSPORT_PARTS = [
    {"origin": (-4, -1.5, -3.5), "size": (8, 3, 7), "uv": (0, 0), "ops": [("move", (0, 16.5, 0))]},
    {"origin": (-1.5, -0.5, -4.5), "size": (3, 1, 1), "uv": (31, 0), "ops": [("move", (0, 16.5, 0))]},
    {"origin": (-2, 0, -2), "size": (4, 4, 4), "uv": (0, 11), "ops": [("move", (2.25, 18, 0))]},
    {"origin": (-2, 0, -2), "size": (4, 4, 4), "uv": (0, 11), "ops": [("move", (-2.25, 18, 0))]},
] + quad_arms([(x, 15.5, z) for x in (-3.5, 3.5) for z in (-3, 3)], 5, (40, 3), (40, 11), 8, (0, 20))

AA_TURRET_PARTS = [
    {"origin": (-3, -1, -3), "size": (6, 1, 6), "uv": (0, 0), "ops": [("move", (0, 24, 0))]},
    {"origin": (-1, -12, -1), "size": (2, 11, 2), "uv": (0, 8), "ops": [("move", (0, 24, 0))]},
] + [
    {"origin": (-0.5, 0, -0.5), "size": (1, 7, 1), "uv": (9, 8),
     "ops": [("rotx", 22.9), ("roty", 45 + i * 90), ("move", (0, 16, 0))]}
    for i in range(4)
] + [
    # head (base child at 0,-13,0 -> absolute y 11), posed panning + elevated
    {"origin": origin, "size": size, "uv": uv,
     "ops": [("rotx", -25), ("roty", 25), ("move", (0, 11, 0))]}
    for origin, size, uv in (
        ((-2, -1, -2), (4, 2, 4), (14, 8)),
        ((-1.5, -0.75, -9), (1, 1, 7), (30, 8)),
        ((0.5, -0.75, -9), (1, 1, 7), (30, 8)),
        ((-2, -4, 0.5), (4, 3, 1), (46, 8)),
        ((2, -1, -2), (2, 3, 3), (0, 24)),
        ((-4, -1, -2), (2, 3, 3), (0, 24)),
    )
]

AIR_UAV_PARTS = [
    # v2: modern military drone — long fuselage, satcom nose, V-tail, pusher prop
    {"origin": (-1.5, -1.5, -9), "size": (3, 3, 19), "uv": (0, 0), "ops": [("move", (0, 14, 0))]},
    {"origin": (-2, -3.5, -9), "size": (4, 2, 4), "uv": (0, 23), "ops": [("move", (0, 14, 0))]},
    {"origin": (-14, -0.5, -1), "size": (28, 1, 3), "uv": (0, 30), "ops": [("move", (0, 14, 0))]},
    {"origin": (-1, 1.5, -7), "size": (2, 1, 2), "uv": (44, 23), "ops": [("move", (0, 14, 0))]},
    # V-tail (Java roll -0.6/+0.6 at body-relative pivots ±1,0,8.5)
    {"origin": (-0.5, -5, -1), "size": (1, 5, 2), "uv": (20, 23),
     "ops": [("rotz", -34.4), ("move", (1, 14, 8.5))]},
    {"origin": (-0.5, -5, -1), "size": (1, 5, 2), "uv": (20, 23),
     "ops": [("rotz", 34.4), ("move", (-1, 14, 8.5))]},
    # rear pusher prop (body child at 0,0,10), posed mid-spin
    {"origin": (-2.5, -2.5, -0.5), "size": (5, 5, 1), "uv": (30, 23),
     "ops": [("rotz", 20), ("move", (0, 14, 10))]},
]

HEAVY_ATTACK_PARTS = [
    {"origin": (-12, -5, -14), "size": (24, 10, 28), "uv": (0, 0), "ops": [("move", (0, 8, 0))]},
    {"origin": (-4, 5, -10), "size": (8, 4, 6), "uv": (0, 40), "ops": [("move", (0, 8, 0))]},
    {"origin": (-3, 6, -16), "size": (1, 1, 6), "uv": (29, 40), "ops": [("move", (0, 8, 0))]},
    {"origin": (2, 6, -16), "size": (1, 1, 6), "uv": (29, 40), "ops": [("move", (0, 8, 0))]},
    {"origin": (-5, -3, -15), "size": (10, 3, 1), "uv": (44, 40), "ops": [("move", (0, 8, 0))]},
    {"origin": (-1, -4, -10), "size": (2, 8, 20), "uv": (0, 50),
     "ops": [("rotz", -6.9), ("move", (12, 9, 0))]},
    {"origin": (-1, -4, -10), "size": (2, 8, 20), "uv": (0, 50),
     "ops": [("rotz", 6.9), ("move", (-12, 9, 0))]},
] + [
    part
    for ax, az, yaw, spin in ((-10, -10, -45, 20), (10, -10, 45, -35),
                              (-10, 10, -135, 50), (10, 10, 135, -10))
    for part in (
        {"origin": (-1, -1, -10), "size": (2, 2, 10), "uv": (67, 40),
         "ops": [("roty", yaw), ("move", (ax, 2, az))]},
        {"origin": (-2, -3, -12), "size": (4, 4, 4), "uv": (67, 53),
         "ops": [("roty", yaw), ("move", (ax, 2, az))]},
        {"origin": (-7, -0.5, -7), "size": (14, 1, 14), "uv": (0, 80),
         "ops": [("roty", spin), ("move", (0, -3, -10)), ("roty", yaw), ("move", (ax, 2, az))]},
    )
]

IFV_PARTS = [
    {"origin": (-9, -4, -14), "size": (18, 9, 28), "uv": (0, 0), "ops": [("move", (0, 11, 0))]},
    {"origin": (-8, -12, -10), "size": (16, 8, 20), "uv": (0, 38), "ops": [("move", (0, 11, 0))]},
    # turret (body child at 0,-12,-2), posed traversed for the preview
    {"origin": (-5, -6, -5), "size": (10, 6, 10), "uv": (73, 38),
     "ops": [("roty", -12), ("move", (0, -1, -2))]},
    {"origin": (-1, -5, -16), "size": (2, 2, 11), "uv": (73, 55),
     "ops": [("roty", -12), ("move", (0, -1, -2))]},
] + [
    {"origin": (-2.5, -2.5, -2.5), "size": (5, 5, 5), "uv": (93, 0), "ops": [("move", (x, 21.5, z))]}
    for x in (-9, 9) for z in (-9, 0, 9)
]

GUNBOAT_PARTS = [
    {"origin": (-10, -4, -24), "size": (20, 8, 24), "uv": (0, 0), "ops": [("move", (0, 20, 0))]},
    {"origin": (-10, -4, 0), "size": (20, 8, 24), "uv": (0, 0), "ops": [("move", (0, 20, 0))]},
    {"origin": (-6, -10, -6), "size": (12, 6, 16), "uv": (0, 33), "ops": [("move", (0, 20, 0))]},
    {"origin": (-1, -16, 2), "size": (2, 6, 2), "uv": (90, 33), "ops": [("move", (0, 20, 0))]},
    {"origin": (-2, -14, 8), "size": (4, 5, 4), "uv": (90, 44), "ops": [("move", (0, 20, 0))]},
    # bow turret (body child at 0,-4,-16), posed traversed
    {"origin": (-4, -4, -4), "size": (8, 4, 8), "uv": (57, 33),
     "ops": [("roty", 18), ("move", (0, 16, -16))]},
    {"origin": (-2, -3, -12), "size": (1, 1, 8), "uv": (57, 46),
     "ops": [("roty", 18), ("move", (0, 16, -16))]},
    {"origin": (1, -3, -12), "size": (1, 1, 8), "uv": (57, 46),
     "ops": [("roty", 18), ("move", (0, 16, -16))]},
]

RECON_HELI_PARTS = [
    {"origin": (-5, -5, -12), "size": (10, 10, 20), "uv": (0, 0), "ops": [("move", (0, 12, 0))]},
    {"origin": (-1.5, -3, 8), "size": (3, 3, 14), "uv": (0, 31), "ops": [("move", (0, 12, 0))]},
    {"origin": (-0.5, -7, 20), "size": (1, 4, 3), "uv": (35, 31), "ops": [("move", (0, 12, 0))]},
    {"origin": (-1.5, 5, -10), "size": (3, 2, 3), "uv": (80, 0), "ops": [("move", (0, 12, 0))]},
    {"origin": (-0.5, 0, -8), "size": (1, 1, 16), "uv": (44, 31), "ops": [("move", (4, 17, 0))]},
    {"origin": (-0.5, 0, -8), "size": (1, 1, 16), "uv": (44, 31), "ops": [("move", (-4, 17, 0))]},
    # main rotor (body child at 0,-5.5,0) and tail rotor (0,-1.5,21.5), posed spinning
    {"origin": (-14, -0.5, -1.5), "size": (28, 1, 3), "uv": (0, 50),
     "ops": [("roty", 30), ("move", (0, 6.5, 0))]},
    {"origin": (-0.5, -3, -3), "size": (1, 6, 6), "uv": (64, 50),
     "ops": [("rotx", 30), ("move", (0, 10.5, 21.5))]},
]

BATTERY_CENTER_PARTS = [
    {"origin": (-9, -3, -13), "size": (18, 8, 26), "uv": (0, 0), "ops": [("move", (0, 11, 0))]},
    {"origin": (-7, -9, -9), "size": (14, 6, 8), "uv": (0, 35), "ops": [("move", (0, 11, 0))]},
    {"origin": (-7, -9, 1), "size": (14, 6, 8), "uv": (0, 35), "ops": [("move", (0, 11, 0))]},
    {"origin": (-6, -7, -13), "size": (12, 4, 4), "uv": (45, 35), "ops": [("move", (0, 11, 0))]},
    {"origin": (-1, -14, 6), "size": (2, 5, 2), "uv": (78, 35), "ops": [("move", (0, 11, 0))]},
] + [
    {"origin": (-2.5, -2.5, -2.5), "size": (5, 5, 5), "uv": (93, 0), "ops": [("move", (x, 21.5, z))]}
    for x in (-9, 9) for z in (-9, 0, 9)
]

# The catapult block preview uses the rail-top texture on all faces; in game
# the sides use launch_catapult_side via the cube_bottom_top model.
LAUNCH_CATAPULT_PARTS = [
    {"origin": (-8, 0, -8), "size": (16, 16, 16), "uv_all": (0, 0, 16, 16), "ops": []},
]

SCOUT_CAR_PARTS = [
    {"origin": (-4, -1.5, -6), "size": (8, 3, 12), "uv": (0, 0), "ops": [("move", (0, 19, 0))]},
    {"origin": (-2.5, -3.5, -2), "size": (5, 2, 5), "uv": (0, 16), "ops": [("move", (0, 19, 0))]},
    {"origin": (-2.5, -0.5, -6.5), "size": (5, 1, 1), "uv": (21, 16), "ops": [("move", (0, 19, 0))]},
    {"origin": (2.5, -7.5, 4.5), "size": (1, 6, 1), "uv": (35, 16), "ops": [("move", (0, 19, 0))]},
] + [
    {"origin": (-1.5, -1.5, -1.5), "size": (3, 3, 3), "uv": (40, 16), "ops": [("move", (x, 21.5, z))]}
    for x in (-4, 4) for z in (-4, 4)
]

ASSEMBLER_PARTS = [
    {"origin": (-8, 0, -8), "size": (16, 16, 16), "uv_all": (0, 0, 16, 16), "ops": []},
]


def main():
    render("surveyor_drone", os.path.join(TEX, "entity/surveyor_drone.png"), quad_rotor_parts())
    render("attack_drone", os.path.join(TEX, "entity/attack_drone.png"), quad_rotor_parts())
    render("harvester_drone", os.path.join(TEX, "entity/harvester_drone.png"), HARVESTER_PARTS)
    render("probe_core", os.path.join(TEX, "block/probe_core.png"), PROBE_CORE_PARTS)
    render("ground_drone", os.path.join(TEX, "entity/ground_drone.png"), GROUND_DRONE_PARTS)
    render("autogun_turret", os.path.join(TEX, "entity/autogun_turret.png"), AUTOGUN_PARTS)
    render("assembler", os.path.join(TEX, "block/assembler.png"), ASSEMBLER_PARTS)
    render("logistics_drone", os.path.join(TEX, "entity/logistics_drone.png"), LOGISTICS_PARTS)
    render("wheeled_hauler", os.path.join(TEX, "entity/wheeled_hauler.png"), HAULER_PARTS)
    render("medium_attack_drone", os.path.join(TEX, "entity/medium_attack_drone.png"),
           MEDIUM_ATTACK_PARTS)
    render("sniper_drone", os.path.join(TEX, "entity/sniper_drone.png"), SNIPER_PARTS)
    render("mortar_emplacement", os.path.join(TEX, "entity/mortar_emplacement.png"), MORTAR_PARTS)
    render("mortar_shell", os.path.join(TEX, "entity/mortar_shell.png"), SHELL_PARTS)
    render("medium_mining_drone", os.path.join(TEX, "entity/medium_mining_drone.png"), MINING_PARTS)
    render("transport_drone", os.path.join(TEX, "entity/transport_drone.png"), TRANSPORT_PARTS)
    render("anti_air_turret", os.path.join(TEX, "entity/anti_air_turret.png"), AA_TURRET_PARTS)
    render("scout_car", os.path.join(TEX, "entity/scout_car.png"), SCOUT_CAR_PARTS)
    render("air_uav", os.path.join(TEX, "entity/air_uav.png"), AIR_UAV_PARTS)
    render("launch_catapult", os.path.join(TEX, "block/launch_catapult_top.png"),
           LAUNCH_CATAPULT_PARTS)
    render("heavy_attack_drone", os.path.join(TEX, "entity/heavy_attack_drone.png"),
           HEAVY_ATTACK_PARTS)
    render("ifv", os.path.join(TEX, "entity/ifv.png"), IFV_PARTS)
    render("gunboat", os.path.join(TEX, "entity/gunboat.png"), GUNBOAT_PARTS)
    render("recon_helicopter", os.path.join(TEX, "entity/recon_helicopter.png"),
           RECON_HELI_PARTS)
    render("battery_center", os.path.join(TEX, "entity/battery_center.png"),
           BATTERY_CENTER_PARTS)


if __name__ == "__main__":
    sys.exit(main())
