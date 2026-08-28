package com.example.memory

import java.util.UUID

data class MemoryItem(
  val id: String = UUID.randomUUID().toString(),
  val key: String,
  val value: String,
  val category: String = "general",
  val timestamp: Long = System.currentTimeMillis()
)
