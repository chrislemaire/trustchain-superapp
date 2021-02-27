package nl.tudelft.trustchain.liquidity.data

import androidx.core.content.ContentProviderCompat.requireContext
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment

/**
 * Wallet class representing digitally stored euro tokens.
 */
class EuroTokenWallet(private val transactionRepository: TransactionRepository) {
    /**
     * Gets the public key that can be used to perform transactions to this wallet.
     */
    fun getPublicKey(): PublicKey {
        return transactionRepository.trustChainCommunity.myPeer.publicKey
    }

    /**
     * Gets the address used to address transactions to this wallet.
     */
    fun getWalletAddress(): String {
        return getPublicKey().toString()
    }

    /**
     * Gets the current balance of this wallet.
     */
    fun getBalance(): Long {
        return transactionRepository.getMyVerifiedBalance()
    }
}
