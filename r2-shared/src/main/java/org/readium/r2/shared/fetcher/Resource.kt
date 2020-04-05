/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.publication.Link
import java.io.InputStream
import java.lang.IllegalArgumentException

interface Resource {
    val link: Link

    fun read(range: LongRange? = null): ByteArray?

    /** An estimate of data length. */
    val length: Long?

    fun close()
}

class NullResource(override val link: Link) : Resource {

    override fun read(range: LongRange?): ByteArray? = null

    override val length: Long? = null

    override fun close() {}
}

internal abstract class StreamResource : Resource {

    protected abstract fun stream(): InputStream

    /** An estimate of data length from metadata */
    protected open val metadataLength: Long? = null

    override fun read(range: LongRange?): ByteArray? {
        return try {
            if (range == null)
                readFully()
            else
                readRange(range)
        } catch (e: Exception) {
            null
        }
    }

    private fun readFully(): ByteArray = stream().use { it.readBytes() }

    private fun readRange(range: LongRange): ByteArray {
        checkRange(range)

        stream().use {
            val skipped = it.skip(range.first)
            if (skipped != range.first) throw Exception("Unable to skip enough bytes")
            val length = range.last - range.first + 1
            return it.read(length)
        }
    }

    override val length: Long?
        get() =
            try {
                metadataLength
            } catch (e: Exception) {
                readFully().size.toLong()
            }
}

internal class BytesResource(override val link: Link, private val bytes: ByteArray) : Resource {

    override fun read(range: LongRange?): ByteArray? {
        if (range == null)
            return bytes.copyOf()

        try {
            checkRange(range)
        } catch (e: Exception) {
            return null
        }

        return bytes.sliceArray(range.map(Long::toInt))
    }

    override val length: Long? = bytes.size.toLong()

    override fun close() {}
}

private fun checkRange(range: LongRange) {
    if (range.first > range.last || range.first < 0)
        throw IllegalArgumentException("Invalid range $range")
    else if (range.last - range.first + 1 > Int.MAX_VALUE)
        throw IllegalArgumentException("Range length greater than Int.MAX_VALUE")
}