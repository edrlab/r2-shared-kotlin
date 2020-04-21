/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import android.app.Activity
import android.content.Intent
import org.readium.r2.shared.BuildConfig
import timber.log.Timber
import java.lang.IllegalArgumentException
import java.util.*

/**
 * [Publication] is too big to be sent through an Intent extra, which can produce a
 * TransactionTooLargeException exception when starting an Activity.
 * See this issue for more information: https://github.com/readium/r2-testapp-kotlin/issues/286
 *
 * To circumvent the problem, you can use this shared [PublicationRepository] to store a
 * [Publication] meant to be shared between activities.
 *
 * This PR shows all the changes to do in the app:
 * https://github.com/readium/r2-testapp-kotlin/pull/303
 *
 * In the source Activity, do:
 * intent.putPublication(publication)
 *
 * And in the target Activity, in onCreate():
 * val publication = intent.getPublication(this)
 *
 * Don't forget to call this in Activity.onDestroy(), to release the Publication:
 * intent.destroyPublication()
 */
private object PublicationRepository {

    private val publications = mutableMapOf<String, Publication>()

    /** Gets the [Publication] stored in the repository with the given ID. */
    fun get(id: String): Publication? = publications[id]

    /** Adds a [Publication] to the repository and returns its ID. */
    fun add(publication: Publication): String {
        var id = publication.metadata.identifier ?: UUID.randomUUID().toString()
        if (publications.containsKey(id)) {
            id = UUID.randomUUID().toString()
        }
        publications[id] = publication
        return id
    }

    /** Removes the [Publication] with the given [id] from the repository. */
    fun remove(id: String) {
        publications.remove(id)
    }

    /** Removes all the [Publication] in the repository. */
    fun clear() {
        publications.clear()
    }

}

private val extraKey = "publicationId"
private val deprecationException = IllegalArgumentException("The [publication] intent extra is not supported anymore. Use the shared [PublicationRepository] instead.")

fun Intent.putPublication(publication: Publication) {
    val id = PublicationRepository.add(publication)
    putExtra(extraKey, id)
}

fun Intent.putPublicationFrom(intent: Intent) {
    putExtra(extraKey, intent.getStringExtra(extraKey))
}

fun Intent.getPublication(activity: Activity): Publication {
    if (hasExtra("publication")) {
        if (BuildConfig.DEBUG) {
            throw deprecationException
        } else {
            Timber.e(deprecationException)
        }
        activity.finish()
    }

    val publication = getStringExtra(extraKey)?.let { PublicationRepository.get(it) }
    if (publication == null) {
        activity.finish()
    }
    // Fallbacks on a dummy Publication to avoid crashing the app until the Activity finishes.
    return publication ?: Publication(metadata = Metadata(identifier = "dummy", localizedTitle = LocalizedString("")))
}

fun Intent.destroyPublication(activity: Activity) {
    if (activity.isFinishing) {
        getStringExtra(extraKey)?.let {
            PublicationRepository.remove(it)
        }
    }
}
