#!/usr/bin/env python3
# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

import argparse
import difflib
import os
import subprocess
import sys
import time

python = 'py' if os.name == 'nt' else 'python3'
PYTHONJ_ONLY_TESTS = {'pythonj'}

def get_default_test_names() -> list[str]:
    return sorted(x[:-3] for x in os.listdir('tests') if x.endswith('.py') and x != 'rules.py')

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('-c', '--clean', action='store_true', help='clean build outputs first')
    parser.add_argument('-j', '--jobs', type=int, help='number of parallel build jobs')
    parser.add_argument('-v', '--verbose-build', action='store_true', help='show verbose build commands')
    parser.add_argument('py_names', nargs='*', help='names of tests to run')
    args = parser.parse_args()

    py_names = args.py_names
    if not py_names:
        py_names = get_default_test_names()

    start = time.perf_counter()
    make_cmd = [python, 'make.py']
    if args.clean:
        make_cmd.append('--clean')
    if args.jobs is not None:
        make_cmd.extend(['--jobs', str(args.jobs)])
    if args.verbose_build:
        make_cmd.append('--verbose')
    make_cmd.extend(f'tests/_out/{py_name}.jar' for py_name in py_names)
    subprocess.check_call(make_cmd)
    build_time = time.perf_counter() - start
    print(f'{build_time=:5.3f}')

    for py_name in py_names:
        start = time.perf_counter()
        sep = ';' if os.name == 'nt' else ':'
        j_output = subprocess.check_output(['java', '-cp', f'../_out/pythonj.jar{sep}_out/{py_name}.jar', py_name], cwd='tests')
        jexec_time = time.perf_counter() - start

        if py_name in PYTHONJ_ONLY_TESTS:
            print(f'{py_name:>15}: jexec_time={jexec_time:5.3f} pyexec_time=  n/a (pythonj-only)')
            continue

        start = time.perf_counter()
        c_output = subprocess.check_output([sys.executable, f'{py_name}.py'], cwd='tests')
        pyexec_time = time.perf_counter() - start

        if c_output == j_output:
            print(f'{py_name:>15}: jexec_time={jexec_time:5.3f} pyexec_time={pyexec_time:5.3f} ({pyexec_time / jexec_time:5.2f}x)')
        else:
            print()
            print(f'ERROR: output mismatched on test {py_name!r}:')
            c_output_lines = c_output.decode(errors='surrogateescape').splitlines()
            j_output_lines = j_output.decode(errors='surrogateescape').splitlines()
            for line in difflib.unified_diff(c_output_lines, j_output_lines, 'cpython output', 'pythonj output', lineterm=''):
                print(line)
            raise SystemExit(1)

if __name__ == '__main__':
    main()
