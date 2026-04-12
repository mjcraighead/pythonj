# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

TEST_NAMES = [
    'builtins_smoke',
    'fib',
    'mandelbrot',
]

def rules(ctx):
    python = 'py' if ctx.host.os == 'windows' else 'python3'

    spec_json = '../_out/spec.json'
    semantics_json = '../_out/pythonj_semantics.json'
    pythonj_jar = '../_out/pythonj.jar'
    pythonj_script = '../pythonj.py'
    pythonj_script_deps = ['../extract_spec.py', '../ir.py']
    jar_script = '../tools/build_jar.py'

    build_outputs = []
    for name in TEST_NAMES:
        py_path = f'{name}.py'
        java_path = f'_out/{name}.java'
        ctx.rule(java_path, [pythonj_script, *pythonj_script_deps, spec_json, semantics_json, py_path],
            cmd=[python, pythonj_script, 'translate', '--spec', spec_json, '--semantics', semantics_json, py_path, '-o', java_path])

        jar_path = f'_out/{name}.jar'
        ctx.rule(jar_path, [jar_script, pythonj_jar, java_path],
            cmd=[python, jar_script, '--classpath', pythonj_jar, '-o', jar_path, java_path])
        build_outputs.append(jar_path)

    ctx.rule(':build', build_outputs)
