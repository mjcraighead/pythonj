# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

def fib(x):
    if x <= 1:
        return 1
    return fib(x-1) + fib(x-2)

for i in range(30):
    print(fib(i))
