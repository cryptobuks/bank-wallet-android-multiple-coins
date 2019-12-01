package io.horizontalsystems.bankwallet.modules.settings.experimental

import io.horizontalsystems.bankwallet.SingleLiveEvent

class ExperimentalFeaturesRouter : ExperimentalFeaturesModule.IRouter {

    val showBitcoinHodlingLiveEvent = SingleLiveEvent<Unit>()

    override fun showBitcoinHodling() {
        showBitcoinHodlingLiveEvent.call()
    }

}
