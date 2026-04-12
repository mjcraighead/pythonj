#!/usr/bin/env python3
# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

import argparse
import os

import pythonj

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--spec', required=True)
    parser.add_argument('--semantics', required=True)
    parser.add_argument('-o', '--output', required=True)
    args = parser.parse_args()
    pythonj.gen_runtime_artifacts(
        os.path.abspath(args.spec),
        os.path.abspath(args.output),
        os.path.abspath(args.semantics),
    )

if __name__ == '__main__':
    main()
