/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.publication.Link

interface Fetcher {

    /** Return a handle to try to retrieve a `link`'s content. */
    fun get(link: Link): Resource

    /** Close resources associated with the fetcher if there's any. */
    fun close()
}
