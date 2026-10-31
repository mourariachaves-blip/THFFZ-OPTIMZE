package com.th.optimizer

object RootTools {
    fun run(cmd: String): Pair<String, Int> = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        val out = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
        Pair(out.trim(), p.waitFor())
    } catch (e: Exception) { Pair(e.message ?: "erro", -1) }

    fun hasRoot(): Boolean = run("id").first.contains("uid=0")
}
