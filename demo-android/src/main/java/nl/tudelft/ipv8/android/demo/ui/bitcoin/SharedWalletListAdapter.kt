package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.coin.CoinUtil
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex

class SharedWalletListAdapter(private val view: View, private val items: List<TrustChainBlock>): BaseAdapter() {
    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val parsedTransaction = CoinUtil.parseTransaction(items[p0].transaction)
        val publicKeyTextView = view.findViewById<TextView>(R.id.sw_id_item_t)
        val votingThreshold = view.findViewById<TextView>(R.id.sw_threshold_vt)
        val entranceFee = view.findViewById<TextView>(R.id.sw_entrance_fee_vt)
        val nrOfUsers = view.findViewById<TextView>(R.id.nr_of_users_tv)

        val votingThresholdText = "${parsedTransaction.getInt(CoinCommunity.SW_VOTING_THRESHOLD)} %"
        val entranceFeeText = "${parsedTransaction.getDouble(CoinCommunity.SW_ENTRANCE_FEE)} BTC"
        val users = "${parsedTransaction.getJSONArray(CoinCommunity.SW_TRUSTCHAIN_PKS).length()} user(s) in this shared wallet"
        publicKeyTextView.text = items[p0].publicKey.toHex()
        votingThreshold.text = votingThresholdText
        entranceFee.text = entranceFeeText
        nrOfUsers.text = users
        return view
    }

    override fun getItem(p0: Int): Any {
        return items[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getCount(): Int {
        return items.size
    }
}

