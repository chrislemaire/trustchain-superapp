package nl.tudelft.trustchain.liquidity.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_pool_wallet.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.liquidity.R
import nl.tudelft.trustchain.liquidity.service.WalletService
import org.bitcoinj.wallet.Wallet

class WalletFragment : BaseFragment(R.layout.fragment_pool_wallet) {
    override fun onCreate(savedInstanceState: Bundle?) {
        println("Hello World!")

        super.onCreate(savedInstanceState)

        val walletDir = context?.cacheDir ?: throw Error("CacheDir not found")
        val wallet = WalletService.createPersonalWallet(walletDir)

        lifecycleScope.launchWhenStarted {
//            btnCopyWalletLink.setOnClickListener {
//                clipboard?.setPrimaryClip(ClipData.newPlainText("Wallet Link", txtWalletLink.text))
//            }
            while (isActive) {
                bitCoinAddress.text = "Wallet public key: " + wallet.currentReceiveAddress().toString()
                bitCoinBalance.text = "Wallet balance (confirmed): ${wallet.balance.toFriendlyString()}\nWallet balance (estimated): ${wallet.getBalance(
                    Wallet.BalanceType.ESTIMATED).toFriendlyString()}"
                delay(1000)
            }
        }
    }
}
