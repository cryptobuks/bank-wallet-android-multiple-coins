package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo.CoinValueInfo
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo.CurrencyValueInfo
import java.math.BigDecimal


class SendFeePresenter(
        val view: SendFeeModule.IView,
        private val interactor: SendFeeModule.IInteractor,
        private val helper: SendFeePresenterHelper,
        private val baseCoin: Coin,
        private val baseCurrency: Currency,
        private val feeCoinData: Pair<Coin, String>?)
    : ViewModel(), SendFeeModule.IViewDelegate, SendFeeModule.IFeeModule {

    var moduleDelegate: SendFeeModule.IFeeModuleDelegate? = null

    private var xRate: BigDecimal? = null
    private var inputType = SendModule.InputType.COIN

    private var fee: BigDecimal = BigDecimal.ZERO
    private var availableFeeBalance: BigDecimal? = null

    private var feeRates: List<FeeRateInfo>? = null
    private var feeRateInfo = FeeRateInfo(FeeRatePriority.MEDIUM, 0, 0)

    private val coin: Coin
        get() = feeCoinData?.first ?: baseCoin

    private fun syncError() {
        try {
            validate()
            view.setInsufficientFeeBalanceError(null)
        } catch (e: SendFeeModule.InsufficientFeeBalance) {
            view.setInsufficientFeeBalanceError(e)
        }
    }

    private fun syncFeeLabels() {
        view.setPrimaryFee(helper.feeAmount(fee, SendModule.InputType.COIN, xRate))
        view.setSecondaryFee(helper.feeAmount(fee, SendModule.InputType.CURRENCY, xRate))
    }

    private fun syncFeeRateLabels() {
        view.setDuration(feeRateInfo.duration)
        view.setFeePriority(feeRateInfo.priority)
    }

    private fun validate() {
        val (feeCoin, coinProtocol) = feeCoinData ?: return
        val availableFeeBalance = availableFeeBalance ?: return

        if (availableFeeBalance < fee) {
            throw SendFeeModule.InsufficientFeeBalance(baseCoin, coinProtocol, feeCoin, CoinValue(feeCoin, fee))
        }
    }

    // SendFeeModule.IFeeModule

    override val isValid: Boolean
        get() = try {
            validate()
            true
        } catch (e: Exception) {
            false
        }

    override val feeRate
        get() = feeRateInfo.feeRate

    override val primaryAmountInfo: AmountInfo
        get() {
            return when (inputType) {
                SendModule.InputType.COIN -> CoinValueInfo(CoinValue(coin, fee))
                SendModule.InputType.CURRENCY -> {
                    this.xRate?.let { xRate ->
                        CurrencyValueInfo(CurrencyValue(baseCurrency, fee * xRate))
                    } ?: throw Exception("Invalid state")
                }
            }
        }

    override val secondaryAmountInfo: AmountInfo?
        get() {
            return when (inputType.reversed()) {
                SendModule.InputType.COIN -> CoinValueInfo(CoinValue(coin, fee))
                SendModule.InputType.CURRENCY -> {
                    this.xRate?.let { xRate ->
                        CurrencyValueInfo(CurrencyValue(baseCurrency, fee * xRate))
                    }
                }
            }
        }

    override val duration: Long?
        get() = feeRateInfo.duration

    override fun setFee(fee: BigDecimal) {
        this.fee = fee
        syncFeeLabels()
        syncError()
    }

    override fun setAvailableFeeBalance(availableFeeBalance: BigDecimal) {
        this.availableFeeBalance = availableFeeBalance
        syncError()
    }

    override fun setInputType(inputType: SendModule.InputType) {
        this.inputType = inputType
    }

    // SendFeeModule.IViewDelegate

    override fun onViewDidLoad() {
        xRate = interactor.getRate(coin.code)

        feeRates = interactor.getFeeRates()

        feeRates?.find { it.priority == FeeRatePriority.MEDIUM }?.let {
            feeRateInfo = it
        }

        syncFeeRateLabels()
        syncFeeLabels()
        syncError()
    }

    override fun onClickFeeRatePriority() {
        feeRates?.let {
            view.showFeeRatePrioritySelector(it.map { rateInfo ->
                feeRateInfoViewItem(rateInfo)
            })
        }
    }

    private fun feeRateInfoViewItem(rateInfo: FeeRateInfo): SendFeeModule.FeeRateInfoViewItem {
        return SendFeeModule.FeeRateInfoViewItem(feeRateInfo = rateInfo,
                selected = rateInfo.priority == feeRateInfo.priority)
    }

    override fun onChangeFeeRate(feeRateInfo: FeeRateInfo) {
        this.feeRateInfo = feeRateInfo

        syncFeeRateLabels()

        moduleDelegate?.onUpdateFeeRate(feeRate)
    }
}
