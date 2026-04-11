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
REAL_SPAN: float = IMAG_SPAN * WIDTH / HEIGHT * CHAR_ASPECT

def main():
    y: int
    for y in range(HEIGHT):
        line = []
        x: int
        for x in range(WIDTH):
            total_i: int = 0
            yoff: float
            for yoff in SUBPIXEL_OFFSETS:
                imag: float = ((((y + yoff) / HEIGHT) * IMAG_SPAN) + (IMAG_CENTER - (IMAG_SPAN / 2.0)))
                xoff: float
                for xoff in SUBPIXEL_OFFSETS:
                    real: float = ((((x + xoff) / WIDTH) * REAL_SPAN) + (REAL_CENTER - (REAL_SPAN / 2.0)))
                    zr: float = 0.0
                    zi: float = 0.0
                    i: int = 0
                    while i < MAX_ITER:
                        zr2: float = zr * zr
                        zi2: float = zi * zi
                        if zr2 + zi2 > 4.0:
                            break
                        zi = 2.0*zr*zi + imag
                        zr = zr2 - zi2 + real
                        i += 1
                    total_i += i
            shade: int = ((total_i // 16) * (len(PALETTE) - 1)) // MAX_ITER
            line.append(PALETTE[shade])
        print(''.join(line))

main()
