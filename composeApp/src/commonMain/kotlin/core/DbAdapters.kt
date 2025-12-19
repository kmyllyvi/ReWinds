package core

import app.cash.sqldelight.ColumnAdapter

val listOfStringAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String) = databaseValue.split(",")
    override fun encode(value: List<String>) = value.joinToString(separator = ",")
}