package gex.com.masinqojava

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import gex.com.masinqojava.MainActivity
import gex.com.masinqojava.MainActivity.Companion.READ_STORAGE_PERMISSION
import gex.com.masinqojava.MainActivity.Companion.REQUEST_READ_STORAGE_PERMISSION
import gex.com.masinqojava.MainActivity.Companion.albums
import gex.com.masinqojava.MainActivity.Companion.artists
import gex.com.masinqojava.MainActivity.Companion.genres
import gex.com.masinqojava.MainActivity.Companion.songs
import gex.com.masinqojava.ViewPagerAdapter

class PermissionManager(private val activity: MainActivity, private val onSuccess: () -> Unit) {

    fun requestRuntimePermission() {

        if (ActivityCompat.checkSelfPermission(
                activity, READ_STORAGE_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            songs = MusicLoader.getSongs(activity)
            albums = MusicLoader.getAlbums(activity)
            artists = MusicLoader.getArtists(activity)
            genres = MusicLoader.getGenres(activity)
            onSuccess()
        } else {
            ActivityCompat.requestPermissions(
                activity, arrayOf(READ_STORAGE_PERMISSION), REQUEST_READ_STORAGE_PERMISSION
            )
        }
    }

    fun handlePermissionResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == REQUEST_READ_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                songs = MusicLoader.getSongs(activity)
                albums = MusicLoader.getAlbums(activity)
                artists = MusicLoader.getArtists(activity)
                genres = MusicLoader.getGenres(activity)
                onSuccess()
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, READ_STORAGE_PERMISSION
                )
            ) {
                showRationaleDialog()
            } else {
                showSettingDialog()
            }
        }
    }

    private fun showSettingDialog() {
        AlertDialog.Builder(activity)
            .setMessage("This feature is unavailable because it requires permissions that have been denied")
            .setTitle("Permission required")
            .setCancelable(false)
            .setPositiveButton("Settings") { dialog, which ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", activity.packageName, null)
                intent.data = uri
                activity.startActivity(intent)
                dialog.dismiss()
            }.show()
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(activity)
            .setMessage("This application really needs access to your storage in order to play music.")
            .setTitle("Permission required")
            .setCancelable(false)
            .setNegativeButton(
                "Cancel",
                (DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() })
            ).setPositiveButton("OK") { dialog, which ->
                requestRuntimePermission()
                dialog.dismiss()
            }.show()
    }

}


