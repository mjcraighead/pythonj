# pythonj (https://github.com/mjcraighead/pythonj)
# Copyright (c) 2012-2026 Matt Craighead
# SPDX-License-Identifier: MIT

RUNTIME_JAVA_FILES = [
    'runtime/PyBool.java',
    'runtime/PyBuiltinFunctions.java',
    'runtime/PyByteArray.java',
    'runtime/PyBytes.java',
    'runtime/PyDict.java',
    'runtime/PyEnumerate.java',
    'runtime/PyExceptions.java',
    'runtime/PyFile.java',
    'runtime/PyFloat.java',
    'runtime/PyInt.java',
    'runtime/PyList.java',
    'runtime/PyMappingProxy.java',
    'runtime/PyNone.java',
    'runtime/PyObject.java',
    'runtime/PyRange.java',
    'runtime/PyReversed.java',
    'runtime/PySet.java',
    'runtime/PySlice.java',
    'runtime/PyString.java',
    'runtime/PyTuple.java',
    'runtime/PyZip.java',
    'runtime/Runtime.java',
]

def rules(ctx):
    python = 'py' if ctx.host.os == 'windows' else 'python3'

    spec_json = '_out/spec.json'
    script = 'extract_spec.py'
    ctx.rule(spec_json, script, cmd=[python, script, spec_json])

    pyruntime_java = '_out/PyRuntime.java'
    semantics_json = '_out/pythonj_semantics.json'
    script = 'gen_runtime.py'
    script_deps = ['extract_spec.py', 'ir.py', 'pythonj.py', 'pythonj_builtins.py', 'pythonj_runtime.py', 'pythonj__operator.py']
    ctx.rule([pyruntime_java, semantics_json], [script, *script_deps, spec_json],
        cmd=[python, script, '--spec', spec_json, '--semantics', semantics_json, '-o', pyruntime_java])

    pythonj_jar = '_out/pythonj.jar'
    script = 'tools/build_jar.py'
    java_files = [pyruntime_java, *RUNTIME_JAVA_FILES]
    ctx.rule(pythonj_jar, [script, *java_files], cmd=[python, script, '-o', pythonj_jar, *java_files])
