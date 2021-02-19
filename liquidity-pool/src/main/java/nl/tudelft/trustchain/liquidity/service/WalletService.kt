package nl.tudelft.trustchain.liquidity.service

import android.content.ContextWrapper.*
import org.bitcoinj.core.ECKey
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.net.URL

object WalletService {
    private const val bitcoinFaucetEndpoint = "http://134.122.59.107:3000"
    val params = RegTestParams.get()

    fun createPersonalWallet(dir: File): Wallet =
        createWallet(dir, "personal")

    fun createMultiSigWallet(dir: File): Wallet =
        createWallet(dir, "multi-sig")

    fun createWallet(dir: File, name: String): Wallet =
        object : WalletAppKit(params, dir, name) {
            override fun onSetupCompleted() {
                if (wallet().keyChainGroupSize < 1) {
                    wallet().importKey(ECKey())
                }

                if (wallet().balance.isZero) {
                    val address = wallet().issuedReceiveAddresses.first().toString()
                    URL("$bitcoinFaucetEndpoint?id=$address").readBytes()
                }
            }
        }.wallet()
}
