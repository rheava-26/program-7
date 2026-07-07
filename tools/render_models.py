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
    # base plate + pedestal (pivot 0,24,0)
    {"origin": (-4, -2, -4), "size": (8, 2, 8), "uv": (0, 0), "ops": [("move", (0, 24, 0))]},
    {"origin": (-2, -8, -2), "size": (4, 6, 4), "uv": (0, 11), "ops": [("move", (0, 24, 0))]},
] + [
    # head (base child at 0,-8,0), posed mid-pan for the preview
    {"origin": origin, "size": size, "uv": uv, "ops": [("roty", 25), ("move", (0, 16, 0))]}
    for origin, size, uv in (
        ((-3, -2, -3), (6, 4, 6), (17, 11)),
        ((-2, -1, -9), (1, 1, 6), (42, 11)),
        ((1, -1, -9), (1, 1, 6), (42, 11)),
        ((-1, -3, -1), (2, 1, 2), (42, 19)),
        ((3, -1.5, -1), (2, 3, 4), (33, 22)),
    )
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


if __name__ == "__main__":
    sys.exit(main())
