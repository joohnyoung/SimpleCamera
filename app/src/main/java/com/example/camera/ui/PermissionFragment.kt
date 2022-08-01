package com.example.camera.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.camera.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PermissionFragment: Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasPermissions(requireContext())) {
            navigateToCamera()
        } else {
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        }
    }

    /** 授权回调 */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size == permissions.size) {
                navigateToCamera()
            } else {
                Toast.makeText(requireContext(), "未授予相关权限", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToCamera() {
        lifecycleScope.launch(Dispatchers.Main) {
            Navigation.findNavController(requireActivity(), R.id.fragmentContainer).navigate(
                R.id.action_permissionFragment_to_cameraFragment)
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 10
        private val PERMISSIONS_REQUIRED = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
        /** 检查相关权限是否被授予 */
        private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}