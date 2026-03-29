#!/usr/bin/env python3
# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

import argparse
import ast
import difflib
import os
import shutil
import subprocess
import sys
import time

import extract_spec
import pythonj

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('py_names', nargs='*', help='names of tests to run')
    args = parser.parse_args()

    py_names = args.py_names
    if not py_names:
        py_names = sorted(x[:-3] for x in os.listdir('tests') if x.endswith('.py'))

    start = time.perf_counter()
    if os.path.exists('runtime/_out'):
        shutil.rmtree('runtime/_out')
    os.mkdir('runtime/_out')
    extract_spec.gen_spec('runtime/_out/spec.json')
    pythonj.gen_code('runtime/_out/spec.json', 'runtime/_out/PyGenerated.java')
    codegen_time = time.perf_counter() - start
    print(f'{codegen_time=:.3f}')

    start = time.perf_counter()
    if os.path.exists('tests/_out'):
        shutil.rmtree('tests/_out')
    subprocess.check_call(['javac', '-d', '_out', *pythonj.RUNTIME_JAVA_FILES, '_out/PyGenerated.java'], cwd='runtime')
    os.makedirs('tests/_out', exist_ok=True)
    subprocess.check_call(['jar', '--create', '--file', 'tests/_out/pythonj.jar', '--date=1980-01-01T00:00:02Z', '-C', 'runtime/_out', '.'])
    initial_javac_time = time.perf_counter() - start
    print(f'initial_javac_time={initial_javac_time:.3f}')

    start = time.perf_counter()
    for py_name in py_names:
        py_path = f'tests/{py_name}.py'
        with open(py_path, encoding='utf-8') as f:
            node = ast.parse(f.read())
        analyzer = pythonj.ScopeAnalyzer()
        analyzer.visit(node)
        visitor = pythonj.PythonjVisitor(py_path, analyzer.scope_infos, analyzer.scope_infos[node])
        visitor.visit(node)
        if visitor.n_errors:
            print(f'Translation failed: {visitor.n_errors} errors')
            raise SystemExit(1)
        with open(f'tests/_out/{py_name}.java', 'w') as f:
            visitor.write_java(f, py_name)
    translate_time = time.perf_counter() - start
    print(f'translate_time={translate_time:5.3f}')

    start = time.perf_counter()
    subprocess.check_call(['javac', '-cp', 'pythonj.jar', *(f'{py_name}.java' for py_name in py_names)], cwd='tests/_out')
    javac_time = time.perf_counter() - start
    print(f'javac_time={javac_time:5.3f}')

    for py_name in py_names:
        start = time.perf_counter()
        sep = ';' if os.name == 'nt' else ':'
        j_output = subprocess.check_output(['java', '-cp', f'_out/pythonj.jar{sep}_out', py_name], cwd='tests')
        jexec_time = time.perf_counter() - start

        start = time.perf_counter()
        c_output = subprocess.check_output([sys.executable, f'{py_name}.py'], cwd='tests')
        pyexec_time = time.perf_counter() - start

        if j_output == c_output:
            print(f'{py_name:>15}: jexec_time={jexec_time:5.3f} pyexec_time={pyexec_time:5.3f} ({pyexec_time / jexec_time:5.2f}x)')
        else:
            print()
            print(f'ERROR: output mismatched on test {py_name!r}:')
            j_output_lines = j_output.decode(errors='surrogateescape').splitlines()
            c_output_lines = c_output.decode(errors='surrogateescape').splitlines()
            for line in difflib.unified_diff(j_output_lines, c_output_lines, 'pythonj output', 'cpython output', lineterm=''):
                print(line)
            raise SystemExit(1)

if __name__ == '__main__':
    main()
