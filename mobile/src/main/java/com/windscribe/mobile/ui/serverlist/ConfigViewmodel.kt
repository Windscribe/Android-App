package com.windscribe.mobile.ui.serverlist

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.backend.openvpn.OpenVPNConfigParser
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.serverlist.entity.ConfigFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject

abstract class ConfigViewmodel : ViewModel() {
    abstract fun loadConfigFile(context: Context, fileUri: Uri)
    abstract fun deleteCustomConfig(config: ConfigFile)
}

@HiltViewModel
class ConfigViewmodelImpl @Inject constructor(
    private val localDb: LocalDbInterface,
    private val latencyRepository: LatencyRepository
) : ConfigViewmodel() {

    private val logger = LoggerFactory.getLogger("ConfigViewmodel")

    override fun loadConfigFile(context: Context, fileUri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(fileUri)
            inputStream?.use {
                val documentFile = DocumentFile.fromSingleUri(context, fileUri)
                val fileName = validatedConfigFileName(context, documentFile) ?: return

                val content = it.bufferedReader().use { reader -> reader.readText() }
                var username = ""
                var password = ""

                try {
                    val configParser = OpenVPNConfigParser()
                    InputStreamReader(context.contentResolver.openInputStream(fileUri)!!).use { reader ->
                        username = configParser.getEmbeddedUsername(reader)
                        password = configParser.getEmbeddedPassword(reader)
                    }
                } catch (e: Exception) {
                    logger.warn("Error parsing OpenVPN config: ${e.message}")
                }

                logger.info("Successfully read file: $fileName")
                onConfigFileContentReceived(fileName, content, username, password)
            }
        } catch (e: IOException) {
            logger.error("Error reading file: ${e.message}")
        }
    }

    private fun validatedConfigFileName(context: Context, documentFile: DocumentFile?): String? {
        if (documentFile == null) {
            showToast(context, "Choose a valid config file")
            return null
        }
        if (documentFile.length() > 1024 * 12) {
            showToast(context, "File is larger than 12KB")
            return null
        }
        var fileName = documentFile.name ?: return null
        if (!fileName.endsWith(".conf") && !fileName.endsWith(".ovpn")) {
            showToast(context, "Choose a valid .ovpn or .conf file.")
            return null
        }
        val maxLength = 35
        if (fileName.length > maxLength) {
            val ext = fileName.substringAfterLast('.')
            val nameWithoutExt = fileName.substringBeforeLast('.')
            fileName = "${nameWithoutExt.take(maxLength - ext.length - 1)}.$ext"
        }
        return fileName
    }

    private fun onConfigFileContentReceived(
        name: String, content: String, username: String, password: String
    ) {
        val configFile = ConfigFile(0, name, content, username, password, true)
        addConfigFileToDatabase(configFile)
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun addConfigFileToDatabase(configFile: ConfigFile) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextPrimaryKey = runCatching { localDb.getMaxPrimaryKey() + 1 }.getOrElse {
                logger.warn("Error fetching max primary key, using default value.")
                20001
            }
            configFile.primaryKey = nextPrimaryKey
            runCatching {
                localDb.addConfigSync(configFile)
                logger.info("Config added successfully to database.")
                latencyRepository.updateConfigLatencies()
            }.onFailure {
                logger.error("Error adding config file: ${it.message}")
            }
        }
    }

    override fun deleteCustomConfig(config: ConfigFile) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                localDb.deleteCustomConfig(config.primaryKey)
                logger.info("Config deleted successfully from database.")
            }.onFailure {
                logger.error("Error deleting config file: ${it.message}")
            }
        }
    }
}

fun mockConfigViewmodel(): ConfigViewmodel {
    return object : ConfigViewmodel() {
        override fun loadConfigFile(context: Context, fileUri: Uri) {}
        override fun deleteCustomConfig(config: ConfigFile) {}
    }
}