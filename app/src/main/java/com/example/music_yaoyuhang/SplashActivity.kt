package com.example.music_yaoyuhang;
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.music_yaoyuhang.MainActivity
import com.example.music_yaoyuhang.R


class SplashActivity : AppCompatActivity() {

        private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)

        if (isFirstLaunch) {
            showPrivacyPolicyDialog()
        } else {
            navigateToMainActivity()
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showPrivacyPolicyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_privacy_policy, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.btn_agree).setOnClickListener {
            sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
            alertDialog.dismiss()
            navigateToMainActivity()
        }

        dialogView.findViewById<TextView>(R.id.argee).setOnClickListener {
            finish() // 点击不同意时关闭应用
        }

        alertDialog.show()
    }

    private fun navigateToMainActivity() {
        Handler().postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }
}
