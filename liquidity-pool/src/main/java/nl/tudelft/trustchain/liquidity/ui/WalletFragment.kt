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
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.liquidity.R
import nl.tudelft.trustchain.liquidity.data.EuroTokenWallet
import nl.tudelft.trustchain.liquidity.service.WalletService
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet

class WalletFragment : BaseFragment(R.layout.fragment_pool_wallet) {
    /**
     * The wallet app kit used to get a running bitcoin wallet.
     */
    lateinit var app: WalletAppKit
    lateinit var app2: WalletAppKit

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

        val params = RegTestParams.get()
        // Create the wallets for bitcoin and euro token.
        app = WalletService.createPersonalWallet(walletDir)
        val btwWallet = app.wallet()

        val euroWallet = EuroTokenWallet(transactionRepository, getIpv8().myPeer.publicKey);

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
            tempButtonJoin.setOnClickListener {
                euroWallet.joinPool("4c69624e61434c504b3a8d0911792d223e3ee823aa592b010d1ffb1e554edb5d0791148f58675f78d56e80b9b7d689565b20a21d6b8ca97fc9354ab9c7f276572e6d0833a99964bf2a81".hexToBytes(), 0L)
                val sendRequest = SendRequest.to(Address.fromString(params, "mkvdunYqLX8i51qft4mF5opWbQpb6RQHeD"), Coin.valueOf(10000000))
                try {
                    btwWallet.sendCoins(sendRequest)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error in transaction!" + e, Toast.LENGTH_SHORT).show()
                }
            }
            tempButtonSend.setOnClickListener {
                euroWallet.sendTokens("4c69624e61434c504b3a8d0911792d223e3ee823aa592b010d1ffb1e554edb5d0791148f58675f78d56e80b9b7d689565b20a21d6b8ca97fc9354ab9c7f276572e6d0833a99964bf2a81".hexToBytes(), 0L)
            }
            while (isActive) {
                bitCoinAddress.text = btwWallet.currentReceiveAddress().toString()
                bitcoinBalance.text = getString(R.string.wallet_balance_conf_est,
                    btwWallet.balance.toFriendlyString(),
                    btwWallet.getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString())

                euroTokenAddress.text = euroWallet.getWalletAddress()
                euroTokenBalance.text = getString(R.string.wallet_balance_conf,
                    TransactionRepository.prettyAmount(euroWallet.getBalance()))

                tempText.text = "oboi5 " + euroWallet.getPoolOwners().toString()


                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        app.stopAsync()
        app2.stopAsync()
    }
}
