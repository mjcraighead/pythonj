#!/usr/bin/env python3
# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

import argparse
import contextlib
import os
import shutil
import subprocess
import tempfile

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--classpath')
    parser.add_argument('-o', '--output', required=True)
    parser.add_argument('java_paths', nargs='+')
    args = parser.parse_args()

    jar_path = os.path.abspath(args.output)
    jar_dir = os.path.dirname(jar_path)
    tmp_jar_path = f'{jar_path}.tmp'
    classes_dir = tempfile.mkdtemp(prefix='.java-classes.', dir=jar_dir)
    java_paths = [os.path.abspath(path) for path in args.java_paths]

    javac_cmd = ['javac']
    if args.classpath is not None:
        javac_cmd.extend(['-cp', os.path.abspath(args.classpath)])
    javac_cmd.extend(['-d', classes_dir, *java_paths])

    try:
        subprocess.check_call(javac_cmd)
        subprocess.check_call([
            'jar',
            '--create',
            '--file',
            tmp_jar_path,
            '--date=1980-01-01T00:00:02Z',
            '-C',
            classes_dir,
            '.',
        ])
        os.replace(tmp_jar_path, jar_path)
    finally:
        shutil.rmtree(classes_dir, ignore_errors=True)
        with contextlib.suppress(FileNotFoundError):
            os.unlink(tmp_jar_path)

if __name__ == '__main__':
    main()
