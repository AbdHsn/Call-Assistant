package com.callassistant.util

import android.net.Uri

/**
 * Builds a `tel:` [Uri] that preserves USSD/MMI characters such as `*` and `#`.
 *
 * [Uri.parse] treats `#` as the start of a URI fragment, so `tel:*566#` would dial
 * only `*566`. [Uri.fromParts] keeps the full number as typed.
 */
fun telCallUri(number: String): Uri = Uri.fromParts("tel", number, null)
