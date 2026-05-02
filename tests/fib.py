# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

import sys

def fib(x: int) -> int:
    if x <= 1:
        return 1
    return fib(x-1) + fib(x-2)

n = 30 if len(sys.argv) <= 1 else int(sys.argv[1])
for i in range(n):
    print(fib(i))
