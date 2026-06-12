package com.github.berserkr2k.coreplugin.api.core.message

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder

class PlaceholderContext private constructor(val resolver: TagResolver) {
    companion object {
        private val EMPTY = PlaceholderContext(TagResolver.empty())

        fun empty(): PlaceholderContext = EMPTY

        fun of(resolver: TagResolver): PlaceholderContext = PlaceholderContext(resolver)

        fun of(vararg pairs: Pair<String, String>): PlaceholderContext {
            val resolvers = pairs.map { Placeholder.parsed(it.first, it.second) }
            return PlaceholderContext(TagResolver.resolver(resolvers))
        }
    }
}
