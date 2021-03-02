package nl.tudelft.trustchain.liquidity.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_pool_multi_sig_wallet.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.liquidity.R
import nl.tudelft.trustchain.liquidity.service.WalletService
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Transaction
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.SendRequest
import java.util.*

class MultiSigWalletFragment : BaseFragment(R.layout.fragment_pool_multi_sig_wallet) {

    lateinit var provider1AppKit: WalletAppKit
    lateinit var provider2AppKit: WalletAppKit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenStarted { // Startup.

            // Get the directory where wallets can be stored.
            val walletDir = context?.cacheDir ?: throw Error("CacheDir not found")

            // Create a personal wallet for provider 1.
            var provider1AppKit = WalletService.createWallet(walletDir, randomString())
            val provider1Wallet = provider1AppKit.wallet()
            val provider1Key = ECKey()
            provider1Wallet.importKey(provider1Key)

            // Create a personal wallet for provider 2.
            var provider2AppKit = WalletService.createWallet(walletDir, randomString())
            val provider2Wallet = provider2AppKit.wallet()
            val provider2Key = ECKey()
            provider2Wallet.importKey(provider2Key)

            // Create a multi-sig wallet (script) using both providers' keys.
            val keys = listOf(provider1Key, provider2Key)
            val multiSigOutputScript = ScriptBuilder.createMultiSigOutputScript(2, keys)

            provider1Wallet.addCoinsReceivedEventListener { _, _, _, _ ->
                // Provider 1 provides 2 cents of BTC for the multi-sig wallet.
                val provideMultiSigTx = Transaction(provider1AppKit.params())
                val value = Coin.valueOf(0, 1)
                provideMultiSigTx.addOutput(value, multiSigOutputScript)
                val request = SendRequest.forTx(provideMultiSigTx)
                provider1Wallet.completeTx(request);
                val peerGroup = provider1AppKit.peerGroup()
                peerGroup.broadcastTransaction(request.tx).broadcast().get();
            }

            while (isActive) { // Update loop
                debugText.text = provider1Wallet.toString()
                delay(500)
            }
        }
    }

    private fun randomString(): String {
        val uuid = UUID.randomUUID().toString()
        return "uuid = $uuid"
    }

    private fun debugLog(line: String) {
        var text = debugText.text.toString() + "\n" + line
        debugText.text = text
    }

    override fun onDestroy() {
        super.onDestroy()

        provider1AppKit.stopAsync()
        provider2AppKit.stopAsync()
    }
}
