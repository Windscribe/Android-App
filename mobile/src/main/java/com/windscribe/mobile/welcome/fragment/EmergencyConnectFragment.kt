package com.windscribe.mobile.welcome.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.windscribe.mobile.R
import com.windscribe.mobile.databinding.FragmentEmergencyConnectBinding
import com.windscribe.mobile.welcome.WelcomeActivity
import com.windscribe.mobile.welcome.state.EmergencyConnectUIState
import com.windscribe.mobile.welcome.viewmodal.EmergencyConnectViewModal
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EmergencyConnectFragment : Fragment() {
    private var _binding: FragmentEmergencyConnectBinding? = null
    private val viewModal: EmergencyConnectViewModal? by lazy {
        return@lazy (activity as? WelcomeActivity)?.emergencyConnectViewModal?.value
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEmergencyConnectBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews()
        bindState()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun bindState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModal?.uiState?.collectLatest {
                    when (it) {
                        EmergencyConnectUIState.Disconnected -> {
                            _binding?.tvDescription?.visibility = View.VISIBLE
                            _binding?.tvStatus?.visibility = View.INVISIBLE
                            _binding?.progressBar?.visibility = View.INVISIBLE
                            _binding?.tvDescription?.text =
                                getString(R.string.emergency_connect_description)
                            _binding?.ok?.text = getString(R.string.connect)

                        }
                        EmergencyConnectUIState.Connecting -> {
                            _binding?.tvDescription?.visibility = View.INVISIBLE
                            _binding?.tvStatus?.visibility = View.VISIBLE
                            _binding?.progressBar?.visibility = View.VISIBLE
                            _binding?.ok?.text = getString(R.string.disconnect)
                        }
                        EmergencyConnectUIState.Connected -> {
                            _binding?.tvDescription?.visibility = View.VISIBLE
                            _binding?.tvStatus?.visibility = View.INVISIBLE
                            _binding?.progressBar?.visibility = View.INVISIBLE
                            _binding?.tvDescription?.text =
                                getString(R.string.emergency_connected_description)
                            _binding?.ok?.text = getString(R.string.disconnect)
                        }
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModal?.connectionProgressText?.collect {
                    _binding?.tvStatus?.text = it
                }
            }
        }
    }

    private fun bindViews() {
        _binding?.ok?.setOnClickListener {
            viewModal?.connectButtonClick()
        }
        _binding?.cancel?.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }
        _binding?.closeIcon?.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    companion object {
        private const val backStackKey = "EmergencyConnectFragment"
        fun show(manager: FragmentManager, container: Int) {
            val fragment = EmergencyConnectFragment()
            manager.beginTransaction().addToBackStack(backStackKey).add(container, fragment)
                .commit()
        }
    }
}