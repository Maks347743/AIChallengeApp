package com.example.filesystemmcpserver.tools

fun sandboxedPath(projectDir: String, relativePath: String): java.io.File {
    val root = java.io.File(projectDir).canonicalFile
    val target = java.io.File(root, relativePath).canonicalFile
    require(target.path.startsWith(root.path + java.io.File.separator) || target.path == root.path) {
        "Path traversal not allowed: $relativePath"
    }
    return target
}
