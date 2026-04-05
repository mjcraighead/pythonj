# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

PALETTE = ' .:-=+*#%@'
WIDTH = 100
HEIGHT = 60
MAX_ITER = 100
SUBPIXEL_OFFSETS = (0.125, 0.375, 0.625, 0.875)
REAL_CENTER = -0.6
IMAG_CENTER = 0.0
IMAG_SPAN = 2.4
CHAR_ASPECT = 0.64 # adjust for typical terminal character aspect ratio
REAL_SPAN = IMAG_SPAN * WIDTH / HEIGHT * CHAR_ASPECT

for y in range(HEIGHT):
    line = []
    for x in range(WIDTH):
        total_i = 0
        for yoff in SUBPIXEL_OFFSETS:
            imag = ((((y + yoff) / HEIGHT) * IMAG_SPAN) + (IMAG_CENTER - (IMAG_SPAN / 2.0)))
            for xoff in SUBPIXEL_OFFSETS:
                real = ((((x + xoff) / WIDTH) * REAL_SPAN) + (REAL_CENTER - (REAL_SPAN / 2.0)))
                zr = 0.0
                zi = 0.0
                i = 0
                while i < MAX_ITER:
                    zr2 = zr * zr
                    zi2 = zi * zi
                    if zr2 + zi2 > 4.0:
                        break
                    zi = 2.0*zr*zi + imag
                    zr = zr2 - zi2 + real
                    i += 1
                total_i += i
        shade = ((total_i // 16) * (len(PALETTE) - 1)) // MAX_ITER
        line.append(PALETTE[shade])
    print(''.join(line))
