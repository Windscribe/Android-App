package com.windscribe.mobile.ui.upgrade

/**
 * UI model for a single selectable billing plan tile (monthly or yearly). Built by the
 * flavor-specific view model from the store's product details so the stateless
 * [UpgradeContent] never touches Google/Amazon billing types directly.
 */
data class PlanTile(
    val sku: String,
    val title: String,
    val price: String,
    /** Secondary "billed …" line. May contain a struck-through original price for promos. */
    val billedLabel: PlanBilledLabel,
    /** Small green discount chip next to the title (e.g. "-33%"); null hides it. */
    val discountLabel: String? = null,
    /** Promo sticker shown in the corner (e.g. "SAVE 33%"); null hides it. */
    val promoLabel: String? = null,
)

/**
 * The "billed" caption under a plan. For promos the original price is shown struck through
 * before the live price, so the renderer needs the parts split out rather than a single string.
 */
data class PlanBilledLabel(
    val strikeThroughPrice: String? = null,
    val text: String,
)

/**
 * Immutable snapshot the upgrade screen renders. The flavor-specific view model is the single
 * source of truth; the screen only reads this.
 */
data class UpgradeState(
    val loadingMessage: String? = "Loading Billing Plans...",
    val errorMessage: String? = null,
    val monthly: PlanTile? = null,
    val yearly: PlanTile? = null,
    /** True when a promo is active — selection radios are hidden and one plan is forced. */
    val isPromo: Boolean = false,
    val monthlySelected: Boolean = true,
    /** Amazon shows Restore; Google hides it. */
    val showRestore: Boolean = false,
) {
    val showProgress: Boolean get() = loadingMessage != null

    val selectedSku: String?
        get() = if (monthlySelected) monthly?.sku else yearly?.sku
}

/** One-shot navigation/side-effect signals raised by the view model. */
sealed class UpgradeEvent {
    data class Toast(
        val message: String,
    ) : UpgradeEvent()

    data class OpenUrl(
        val url: String,
    ) : UpgradeEvent()

    data class Success(
        val isGhostAccount: Boolean,
    ) : UpgradeEvent()

    /** Close the screen without success (cancelled / already-owned / invalid). */
    object Close : UpgradeEvent()
}
