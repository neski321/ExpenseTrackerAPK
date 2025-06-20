package com.neski.pennypincher.data.models

data class Category(
    val id: String = "",
    val name: String = "",
    val parentId: String? = null,
    val icon: String = "",
    val color: String = ""
)
