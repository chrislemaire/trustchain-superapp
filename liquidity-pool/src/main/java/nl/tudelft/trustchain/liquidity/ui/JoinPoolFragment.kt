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
import kotlinx.android.synthetic.main.fragment_pool_wallet.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.liquidity.R
import nl.tudelft.trustchain.liquidity.data.EuroTokenWallet
import nl.tudelft.trustchain.liquidity.service.WalletService
import org.bitcoinj.core.*
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.RegTestParams
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
    lateinit var sharedPreference: SharedPreferences
    lateinit var  euroWallet: EuroTokenWallet
    val euroLiqWallet = "4c69624e61434c504b3ad9f961188f9f894a19fae40e6e174c0faf762cbcacd20d2e74a7c85b88a1026c99f67f4cca144de24b702b25bce12228e592b033579b639bf87a68389a00d267"
    var status: MutableMap<String, Status> = mutableMapOf("btc" to Status.NOTSEND, "euro" to Status.NOTSEND)
    var btcTransaction: Transaction? = null
    var euroTokenTransaction: TrustChainBlock? = null
    var joinTransaction: TrustChainBlock? = null


    private val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, GatewayStore.getInstance(requireContext()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        val walletDir = context?.cacheDir ?: throw Error("CacheDir not found")
        app = WalletService.createPersonalWallet(walletDir)
        btcWallet = app.wallet()

        val app2 = WalletService.createWallet(walletDir, "Alo?")
        btcLiqWallet = app2.wallet()

        sharedPreference = this.requireActivity().getSharedPreferences("transactions", Context.MODE_PRIVATE)

        euroWallet = EuroTokenWallet(transactionRepository, getIpv8().myPeer.publicKey);

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenStarted {
            val btcTXID = sharedPreference.getString("btcTXID", null)
            if (btcTXID != null) {
                btcTransaction = btcWallet.getTransaction(Sha256Hash.wrap(btcTXID))
                checkBtcStatus()
            }
            val euroTXID = sharedPreference.getString("euroTXID", null)
            if (euroTXID != null) {
                euroTokenTransaction = transactionRepository.getTransactionWithHash(euroTXID.hexToBytes())
                checkEuroStatus()
            }
            val joinTXID = sharedPreference.getString("joinTXID", null)
            if (joinTXID != null) {
                joinTransaction = transactionRepository.getTransactionWithHash(joinTXID.hexToBytes())
                join_pool.isEnabled = false
            }

            val params = RegTestParams.get()
            sendBtc.setOnClickListener {
                sendBtc.isEnabled = false
                val sendRequest = SendRequest.to(Address.fromString(params, "myWTUN68XYcXXE1w7JSHJp2ouTuzHs7xqp"), Coin.valueOf(10000000))
                val sendRes = btcWallet.sendCoins(sendRequest)
                status["btc"] = Status.SEND
                Futures.addCallback(sendRes.broadcastComplete, object : FutureCallback<Transaction> {
                    override fun onSuccess(result: Transaction?) {
                        Log.d("BitcoinTransaction", "Transaction success")
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

            sendEuro.setOnClickListener {
                sendEuro.isEnabled = false
                status["euro"] = Status.SEND
                val proposalBlock: TrustChainBlock? = euroWallet.sendTokens(euroLiqWallet.hexToBytes(), 0L)
                if(proposalBlock != null) {
                    status["euro"] = Status.PENDING
                    euroTokenTransaction = proposalBlock
                    val txid = euroTokenTransaction!!.calculateHash()
                    val editor = sharedPreference.edit()
                    editor.putString("euroTXID", txid.toHex())
                    editor.apply()
                    checkEuroStatus()
                }

            }


            join_pool.setOnClickListener {
                if(btcTransaction != null && euroTokenTransaction != null) {
                    join_pool.isEnabled = false
                    val joinProposalBlock = euroWallet.joinPool(euroLiqWallet.hexToBytes(), btcTransaction!!.txId.toString(), euroTokenTransaction!!.calculateHash().toHex())
                    if(joinProposalBlock != null) {
                        joinTransaction = joinProposalBlock
                        val txid = joinTransaction!!.calculateHash()
                        val editor = sharedPreference.edit()
                        editor.putString("joinTXID", txid.toHex())
                        editor.apply()
                    }
                }
            }

            while (isActive) {
                checkBtcStatus()
                checkEuroStatus()
                join_pool.isEnabled = status["btc"] == Status.VERIFIED && status["euro"] == Status.VERIFIED  && joinTransaction == null
//                join_pool.isEnabled = true
                btc_status.text = getString(R.string.transaction_status, getStatusString("btc"))
                eurotoken_status.text = getString(R.string.transaction_status, getStatusString("euro"))
                join_status.text = getString(R.string.transaction_status, getPoolStatus())
                delay(5000)
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

    // TODO Beautify this code
    private fun getPoolStatus(): String {
        if(joinTransaction != null) {
            if (transactionRepository.trustChainCommunity.database.getLinked(joinTransaction!!) != null) {
                return "Joined"
            }
            if (joinTransaction!!.isProposal) {
                return "Pending"
            } else {
                return "Unknown"
            }
        } else {
            return "Not joined"
        }
    }

    private fun checkBtcStatus() {
        if (btcTransaction != null) {
            sendBtc.isEnabled = false

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
                else -> {
                    status["btc"] = Status.UNKNOWN
                }
            }
        } else {
            status["btc"] = Status.NOTSEND
        }

    }

    private fun checkEuroStatus() {
        if (euroTokenTransaction != null) {
            sendEuro.isEnabled = false
            if (transactionRepository.trustChainCommunity.database.getLinked(euroTokenTransaction!!) != null) {
                status["euro"] = Status.VERIFIED
            }
            else if (euroTokenTransaction!!.isProposal) {
                status["euro"] = Status.PENDING
            }
        } else {
            status["euro"] = Status.NOTSEND
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
