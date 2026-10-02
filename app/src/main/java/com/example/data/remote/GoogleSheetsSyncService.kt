package com.example.data.remote

import com.example.data.local.dao.IngredientDao
import com.example.data.local.dao.SettingDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class SyncResult {
    data class Success(val message: String, val syncedCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

class GoogleSheetsSyncService(
    private val transactionDao: TransactionDao,
    private val ingredientDao: IngredientDao,
    private val settingDao: SettingDao
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun syncPendingTransactions(): SyncResult = withContext(Dispatchers.IO) {
        val webhookUrl = settingDao.getSetting("google_sheets_url")?.value.orEmpty()
        val spreadsheetId = settingDao.getSetting("google_sheets_id")?.value.orEmpty()
        val apiKey = settingDao.getSetting("google_sheets_api_key")?.value.orEmpty()

        if (webhookUrl.isBlank() && (spreadsheetId.isBlank() || apiKey.isBlank())) {
            return@withContext SyncResult.Error(
                "Konfigurasi Google Sheets belum diisi. Masukkan URL Webhook Apps Script atau Spreadsheet ID + API Key di Pengaturan."
            )
        }

        val unsyncedList = transactionDao.getUnsyncedTransactions()
        if (unsyncedList.isEmpty()) {
            return@withContext SyncResult.Success("Semua data transaksi sudah tersinkronisasi.", 0)
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("id", "ID"))
        var successCount = 0

        try {
            if (webhookUrl.isNotBlank()) {
                // Post via Google Apps Script Webhook (Highly recommended for zero-friction POS sync)
                val payloadArray = JSONArray()
                for (trx in unsyncedList) {
                    val items = transactionDao.getTransactionItems(trx.id)
                    val itemsSummary = items.joinToString(", ") { "${it.quantity}x ${it.menuName}" }

                    val itemObj = JSONObject().apply {
                        put("transactionId", trx.id)
                        put("timestamp", dateFormat.format(Date(trx.timestamp)))
                        put("cashierName", trx.cashierName)
                        put("customerName", trx.customerName.orEmpty())
                        put("items", itemsSummary)
                        put("subtotal", trx.subtotal)
                        put("tax", trx.taxAmount)
                        put("serviceCharge", trx.serviceChargeAmount)
                        put("totalAmount", trx.totalAmount)
                        put("paymentMethod", trx.paymentMethod)
                    }
                    payloadArray.put(itemObj)
                }

                val rootJson = JSONObject().apply {
                    put("action", "SYNC_TRANSACTIONS")
                    put("transactions", payloadArray)
                    put("syncTime", dateFormat.format(Date()))
                }

                val request = Request.Builder()
                    .url(webhookUrl)
                    .post(rootJson.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        for (trx in unsyncedList) {
                            transactionDao.markAsSynced(trx.id)
                            successCount++
                        }
                    } else {
                        return@withContext SyncResult.Error("HTTP ${response.code}: Gagal mengirim ke Google Sheets webhook.")
                    }
                }
            } else {
                // Direct Google Sheets API v4 append
                // POST https://sheets.googleapis.com/v4/spreadsheets/{spreadsheetId}/values/Transactions!A1:append?valueInputOption=USER_ENTERED&key={apiKey}
                val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Transactions!A1:append?valueInputOption=USER_ENTERED&key=$apiKey"

                val rowsArray = JSONArray()
                for (trx in unsyncedList) {
                    val items = transactionDao.getTransactionItems(trx.id)
                    val itemsSummary = items.joinToString(", ") { "${it.quantity}x ${it.menuName}" }

                    val row = JSONArray().apply {
                        put(trx.id)
                        put(dateFormat.format(Date(trx.timestamp)))
                        put(trx.cashierName)
                        put(trx.customerName.orEmpty())
                        put(itemsSummary)
                        put(trx.subtotal)
                        put(trx.taxAmount)
                        put(trx.totalAmount)
                        put(trx.paymentMethod)
                    }
                    rowsArray.put(row)
                }

                val bodyJson = JSONObject().apply {
                    put("range", "Transactions!A1")
                    put("majorDimension", "ROWS")
                    put("values", rowsArray)
                }

                val request = Request.Builder()
                    .url(url)
                    .post(bodyJson.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        for (trx in unsyncedList) {
                            transactionDao.markAsSynced(trx.id)
                            successCount++
                        }
                    } else {
                        val errorBody = response.body?.string().orEmpty()
                        return@withContext SyncResult.Error("Google Sheets API Error (${response.code}): $errorBody")
                    }
                }
            }

            // Also record last sync timestamp in settings
            settingDao.setSetting(
                com.example.data.local.entity.SettingEntity(
                    "last_sync_time",
                    dateFormat.format(Date())
                )
            )

            SyncResult.Success("Berhasil sinkronisasi $successCount transaksi ke Google Sheets!", successCount)
        } catch (e: Exception) {
            SyncResult.Error("Gagal menghubungi server Google Sheets: ${e.localizedMessage}")
        }
    }
}
