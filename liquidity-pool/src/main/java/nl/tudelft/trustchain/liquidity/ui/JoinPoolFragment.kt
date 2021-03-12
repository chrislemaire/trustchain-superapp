package nl.tudelft.trustchain.liquidity.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import kotlinx.android.synthetic.main.fragment_pool_join.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.liquidity.R
import nl.tudelft.trustchain.liquidity.service.WalletService
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionConfidence
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet


enum class Status {
    UNKNOWN, SEND, NOTSEND, VERIFIED, PENDING
}

class JoinPoolFragment : BaseFragment(R.layout.fragment_pool_join) {

    lateinit var app: WalletAppKit
    lateinit var btcWallet: Wallet
    lateinit var btcLiqWallet: Wallet
    var status: MutableMap<String, Status> = mutableMapOf("btc" to Status.NOTSEND, "euro" to Status.NOTSEND)
    var btcTransaction: Transaction? = null
    lateinit var sharedPreference: SharedPreferences
//    private val transactionRepository by lazy {
//        TransactionRepository(getIpv8().getOverlay()!!, GatewayStore.getInstance(requireContext()))
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        val walletDir = context?.cacheDir ?: throw Error("CacheDir not found")
        app = WalletService.createPersonalWallet(walletDir)
        btcWallet = app.wallet()

        val app2 = WalletService.createWallet(walletDir, "Alo?")
        btcLiqWallet = app2.wallet()

        sharedPreference = this.requireActivity().getSharedPreferences("transactions", Context.MODE_PRIVATE)


    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenStarted {
            val btcTXID = sharedPreference.getString("btcTXID", null)
            if (btcTXID != null) {
                btcTransaction = btcWallet.getTransaction(Sha256Hash.wrap(btcTXID))
                checkBtcStatus()
            }

            join_pool.isEnabled = status["btc"] == Status.VERIFIED && status["euro"] == Status.VERIFIED

            sendBtc.setOnClickListener {
                sendBtc.isEnabled = false
                val sendRequest = SendRequest.to(btcLiqWallet.currentReceiveAddress(), Coin.valueOf(10000000))
                val sendRes = btcWallet.sendCoins(sendRequest)
                status["btc"] = Status.SEND

                Futures.addCallback(sendRes.broadcastComplete, object : FutureCallback<Transaction> {
                    override fun onSuccess(result: Transaction?) {
                        if (result != null) {
                            status["btc"] = Status.PENDING
                            btcTransaction = result
                            val txid = btcTransaction!!.txId.toString()
                            Log.d("LiquidityPool", "TXID: $txid")
                            val editor = sharedPreference.edit()
                            editor.putString("btcTXID", btcTransaction!!.txId.toString())
                            editor.apply()
                            checkBtcStatus()
                        }
                    }

                    override fun onFailure(t: Throwable) {
                        Log.d("LiquidityPool", "Broadcasting BTC transaction failed")
                    }
                }, Threading.USER_THREAD)
            }

            join_pool.setOnClickListener {

            }

            while (isActive) {
                btc_status.text = getString(R.string.transaction_status, getStatusString("btc"))
                delay(1000)
            }
        }
    }


    private fun getStatusString(cointype: String): String = when (status.get(cointype)) {
        Status.UNKNOWN -> "Unknown"
        Status.NOTSEND -> "Not send yet"
        Status.SEND -> "Send"
        Status.PENDING -> "Pending"
        Status.VERIFIED -> "Verified"
        else -> ""
    }

    private fun checkBtcStatus() {
        if (btcTransaction != null) {
            sendBtc.isEnabled = false
            Log.d("LiquidityPool", "Transaction Status ${btcTransaction!!.confidence}")
            when (btcTransaction!!.confidence.confidenceType) {
                TransactionConfidence.ConfidenceType.BUILDING -> {
                    status["btc"] = if (btcTransaction!!.confidence.depthInBlocks >= 1) Status.VERIFIED else Status.PENDING
                }
                TransactionConfidence.ConfidenceType.PENDING -> {
                    status["btc"] = Status.PENDING;
                }
                TransactionConfidence.ConfidenceType.DEAD -> {
                    status["btc"] = Status.NOTSEND; sendBtc.isEnabled = true
                }
                TransactionConfidence.ConfidenceType.UNKNOWN -> {
                    status["btc"] = Status.UNKNOWN
                }
                TransactionConfidence.ConfidenceType.IN_CONFLICT -> {
                    status["btc"] = Status.UNKNOWN
                }
                else -> {
                    status["btc"] = Status.UNKNOWN
                }
            }
        } else {
            status["btc"] = Status.NOTSEND
        }

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pool_join, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()

        app.stopAsync()
    }
}
