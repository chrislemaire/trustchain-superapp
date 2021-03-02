package nl.tudelft.trustchain.liquidity.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.liquidity.R

class MultiSigWalletFragment : BaseFragment(R.layout.fragment_pool_multi_sig_wallet) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pool_multi_sig_wallet, container, false)
    }
}
