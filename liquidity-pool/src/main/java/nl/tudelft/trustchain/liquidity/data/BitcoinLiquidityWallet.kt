package nl.tudelft.trustchain.liquidity.data

import android.util.Log
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.liquidity.service.WalletService
import org.bitcoinj.core.*
import org.bitcoinj.core.Coin.valueOf
import org.bitcoinj.core.listeners.TransactionReceivedInBlockListener
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.kits.WalletAppKit

class BitcoinLiquidityWallet(private val wallet: Wallet, private val app: WalletAppKit) : LiquidityWallet {

    private lateinit var pool: LiquidityPool
    override val coinName: String = "BTC"

    // TODO: Figure out who to send money to in a transaction response
    override fun initializePool(/*pool: LiquidityPool*/) {
/*
        this.pool = pool
        if (pool.wallet1 == this) {
            wallet.addCoinsReceivedEventListener { _, tx, _, _ ->
                pool.convert1To2(tx.outputSum.value.toDouble(), "me")
            }
        } else {
            wallet.addCoinsReceivedEventListener { _, tx, _, _ ->
                pool.convert2To1(tx.outputSum.value.toDouble(), "me")
            }
        }*/

        /**
         * Adds a transaction listener to the wallet's blockchain. Whenever a btc
         * transaction occurs, we must first check if there is a pending join request
         * and then if eurotokens have also been received from this request,
         * before adding the currencies to the pool. Otherwise, add the transaction to
         * the hashmap of btc transactions
         */
        app.chain().addTransactionReceivedListener (object : TransactionReceivedInBlockListener{
            override fun receiveFromBlock(tx: Transaction, block: StoredBlock,
                                          blockType: AbstractBlockChain.NewBlockType, relativityOffset: Int) {
                Log.d("BitcoinBlock", "Received a simple transaction, not committed yet: ${tx}")
                print("Received a block containing transaction ${tx}")
            }
            override fun notifyTransactionIsInBlock(txHash: Sha256Hash?, block: StoredBlock?, blockType: AbstractBlockChain.NewBlockType?, relativityOffset: Int): Boolean {
                TODO("Not yet implemented")
            }
        })
    }

    // TODO: Properly convert amount in double to long or the other way around
    override fun startTransaction(amount: Double, address: String) {
        wallet.sendCoins(
            SendRequest.to(
                Address.fromString(WalletService.params, address),
                valueOf(amount.toLong())
            )
        )
    }



}


