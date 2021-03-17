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
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.SendRequest
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.io.File
import java.security.Provider
import java.security.Security
import java.util.*


class MultiSigWalletFragment : BaseFragment(R.layout.fragment_pool_multi_sig_wallet) {

    var debugLogLineNumber = 0
    lateinit var provider1AppKit: WalletAppKit
    lateinit var provider2AppKit: WalletAppKit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenStarted { // Startup.

            //bitcoinDemo()
            ethereumDemo()

            while (isActive) { // Update loop
                //debugText.text = provider1Wallet.toString()
                delay(500)
            }
        }
    }

    // Bitcoin multi-sig wallet demo.
    private suspend fun bitcoinDemo() {
        // Get the directory where wallets can be stored.
        val walletDir = context?.cacheDir ?: throw Error("CacheDir not found")

        // Create a personal wallet for provider 1.
        var provider1AppKit = WalletService.createWallet(walletDir, randomString())
        val provider1Wallet = provider1AppKit.wallet()
        val provider1Key = ECKey()
        provider1Wallet.importKey(provider1Key)
        debugLog("Created a personal BTC wallet for provider 1.")
        delay(500)

        // Create a personal wallet for provider 2.
        var provider2AppKit = WalletService.createWallet(walletDir, randomString())
        val provider2Wallet = provider2AppKit.wallet()
        val provider2Key = ECKey()
        provider2Wallet.importKey(provider2Key)
        debugLog("Created a personal BTC wallet for provider 2.")
        delay(500)

        // Create a multi-sig wallet (script) using both providers' keys.
        val keys = listOf(provider1Key, provider2Key)
        val multiSigOutputScript = ScriptBuilder.createMultiSigOutputScript(2, keys)
        debugLog("Created a multi-sig BTC wallet using both providers' keys.")
        delay(500)

        provider1Wallet.addCoinsReceivedEventListener { _, _, _, newBalance ->
            val balance = newBalance.toFriendlyString()
            debugLog("Provider 1 balance changed, new balance is $balance.")
        }

        provider2Wallet.addCoinsReceivedEventListener { _, _, _, newBalance ->
            val balance = newBalance.toFriendlyString()
            debugLog("Provider 2 balance changed, new balance is $balance.")
        }

        // Wait until provider 1 has received some BTC.
        while(provider1Wallet.balance.isZero) {
            delay(500)
        }

        // Provider 1 provides 2 cents of BTC for the multi-sig wallet.
        val provideMultiSigTx = Transaction(provider1AppKit.params())
        val provideValue = Coin.valueOf(0, 2)
        val provideMultiSigOutput = provideMultiSigTx.addOutput(provideValue, multiSigOutputScript)
        val request = SendRequest.forTx(provideMultiSigTx)
        provider1Wallet.completeTx(request);
        var peerGroup = provider1AppKit.peerGroup()
        peerGroup.broadcastTransaction(request.tx).broadcast().get();
        debugLog("Provider 1 broadcast a 0.02 BTC provide transaction for the multi-sig wallet.")
        delay(500)

        // Provider 2 creates a redeem transaction for 0.01 BTC from the multi-sig wallet to
        // its own personal wallet, and signs it with their own key.
        val redeemMultiSigTx2 = Transaction(provider2AppKit.params())
        val redeemMultiSigInput = redeemMultiSigTx2.addInput(provideMultiSigOutput)
        val redeemValue = Coin.valueOf(0, 1)
        redeemMultiSigTx2.addOutput(redeemValue, provider2Wallet.currentReceiveAddress())
        val provideMultiSigOutputScriptPubKey = provideMultiSigOutput.scriptPubKey
        val redeemMultiSigTx2Hash = redeemMultiSigTx2.hashForSignature(
            0,
            provideMultiSigOutputScriptPubKey,
            Transaction.SigHash.ALL,
            false
        )
        val provider2Signature = provider2Key.sign(redeemMultiSigTx2Hash)
        debugLog("Provider 2 signed a 0.01 BTC redeem transaction from the multi-sig wallet to itself.")
        delay(500)

        // TODO: Provider 2 broadcasts its redeem transaction over the trustchain.
        // TODO: Provider 1 sees the redeem transaction from provider 2.

        // Provider 1 creates the same redeem transaction for 0.01 BTC from the multi-sig
        // wallet to the personal wallet of provider 2, but signs it with their own key.
        val redeemMultiSigTx1 = Transaction(provider1AppKit.params())
        redeemMultiSigTx1.addInput(provideMultiSigOutput)
        redeemMultiSigTx1.addOutput(redeemValue, provider2Wallet.currentReceiveAddress())
        val redeemMultiSigTx1Hash = redeemMultiSigTx1.hashForSignature(
            0,
            provideMultiSigOutputScriptPubKey,
            Transaction.SigHash.ALL,
            false
        )
        val provider1Signature = provider1Key.sign(redeemMultiSigTx1Hash)
        debugLog("Provider 1 signed a 0.01 BTC redeem transaction from the multi-sig wallet to provider 2.")
        delay(500)

        // Provider 2 broadcasts the signed redeem transaction.
        val signature1 = TransactionSignature(provider1Signature, Transaction.SigHash.ALL, false)
        val signature2 = TransactionSignature(provider2Signature, Transaction.SigHash.ALL, false)
        val multiSigInputScript = ScriptBuilder.createMultiSigInputScript(signature1, signature2)
        redeemMultiSigInput.scriptSig = multiSigInputScript
        redeemMultiSigInput.verify(provideMultiSigOutput)
        peerGroup = provider2AppKit.peerGroup()
        peerGroup.broadcastTransaction(redeemMultiSigTx2).broadcast().get()
        debugLog("Provider 2 broadcast a signed 0.01 BTC redeem transaction from the multi-sig wallet to itself.")
        delay(500)
    }

    // Ethereum multi-sig wallet demo.
    private fun ethereumDemo() {
        // Need to manually insert provider to be able to generate key pairs.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)

        val web3j = Web3j.build(HttpService("https://rinkeby.infura.io/v3/496ed2a73f4845978f0062d91bc53999"))
        val clientVersion = web3j.web3ClientVersion().sendAsync().get()
        if (clientVersion.hasError()) throw Error("Failed to connect to node.")

        val password = randomString()
        var walletDir = context?.cacheDir?: throw Error("CacheDir not found")
        val fileName =  WalletUtils.generateLightNewWalletFile(password, walletDir)
        walletDir = File(walletDir.absolutePath + "/" + fileName)
        val credentials = WalletUtils.loadCredentials(password, walletDir)
        debugLog("Created a personal ETH wallet for provider 1: ${credentials.address}.")
    }

    private fun randomString(): String {
        val uuid = UUID.randomUUID().toString()
        return "uuid = $uuid"
    }

    private fun debugLog(line: String) {
        debugText.post {
            debugLogLineNumber++
            var previousLines = debugText.text.toString()
            debugText.text = "$previousLines \n $debugLogLineNumber.  $line"
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        provider1AppKit.stopAsync()
        provider2AppKit.stopAsync()
    }
}
