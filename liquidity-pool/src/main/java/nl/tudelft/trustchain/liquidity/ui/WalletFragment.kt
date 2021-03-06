package nl.tudelft.trustchain.liquidity.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_pool_wallet.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.liquidity.R
import nl.tudelft.trustchain.liquidity.data.EuroTokenWallet
import nl.tudelft.trustchain.liquidity.service.WalletService
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.wallet.Wallet

class WalletFragment : BaseFragment(R.layout.fragment_pool_wallet) {
    /**
     * The wallet app kit used to get a running bitcoin wallet.
     */
    lateinit var app: WalletAppKit

    /**
     * A repository for transactions in Euro Tokens.
     */
    private val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, GatewayStore.getInstance(requireContext()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the directory where wallets can be stored.
        val walletDir = context?.cacheDir ?: throw Error("CacheDir not found")

        // Create the wallets for bitcoin and euro token.
        app = WalletService.createPersonalWallet(walletDir)
        val btwWallet = app.wallet()
        val euroWallet = EuroTokenWallet(transactionRepository);

        val clipboard = getSystemService(requireContext(), ClipboardManager::class.java) as ClipboardManager

        // Initialize the button actions and update loop.
        lifecycleScope.launchWhenStarted {
            bitCoinCopyButton.setOnClickListener {
                clipboard.setPrimaryClip(ClipData.newPlainText("Wallet Link", bitCoinAddress.text))
                Toast.makeText(requireContext(), "Copied key to clipboard!", Toast.LENGTH_SHORT).show()
            }
            euroTokenCopyButton.setOnClickListener {
                clipboard.setPrimaryClip(ClipData.newPlainText("Wallet Link", euroTokenAddress.text))
                Toast.makeText(requireContext(), "Copied key to clipboard!", Toast.LENGTH_SHORT).show()
            }
            tempButton.setOnClickListener {
//                euroWallet.joinPool("2a4af164c695119e900242ddd691ee4e76b842e8".hexToBytes(), 1L)
                euroWallet.sendTokens("4c69624e614 34c504b3aa2a8ffb5bcd6171fb76d4e29853e47108c2cf062986b3dffa18ce0b4692aed075dc8e9528450f93045797cd1a3ac2a0b95d27ccbe27ab79756cabebe9d418295".hexToBytes(), 1L)
            }

            while (isActive) {
                bitCoinAddress.text = btwWallet.currentReceiveAddress().toString()
                bitcoinBalance.text = getString(R.string.wallet_balance_conf_est,
                    btwWallet.balance.toFriendlyString(),
                    btwWallet.getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString())

                euroTokenAddress.text = euroWallet.getWalletAddress()
                euroTokenBalance.text = getString(R.string.wallet_balance_conf,
                    TransactionRepository.prettyAmount(euroWallet.getBalance()))

                tempText.text = euroWallet.getPoolOwners().toString()


                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        app.stopAsync()
    }
}
