package com.example.platform

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log

data class ResolvedContact(
  val id: String,
  val name: String,
  val phoneNumber: String,
  val type: String
)

object ContactResolver {

  private const val TAG = "ContactResolver"

  fun searchContacts(context: Context, query: String): List<ResolvedContact> {
    if (!PermissionManager.hasContactsPermission(context)) {
      Log.w(TAG, "Contacts permission not granted")
      return emptyList()
    }

    val trimmed = query.trim()
    if (trimmed.isBlank()) return emptyList()

    val contacts = mutableListOf<ResolvedContact>()
    val contentResolver = context.contentResolver

    // Synonyms / translations for common relationships (Hindi / Nepali / English)
    val queryVariations = mutableListOf(trimmed)
    when (trimmed.lowercase()) {
      "माँ", "माँजी", "मम्मी", "आमा", "माताजी", "mom", "mother", "mum", "mummy" -> {
        queryVariations.addAll(listOf("Maa", "Mummy", "Mom", "Mother", "Mataji", "Aama", "माँ", "मम्मी", "आमा"))
      }
      "पापा", "पिताजी", "बुबा", "बा", "बाबु", "dad", "father", "daddy" -> {
        queryVariations.addAll(listOf("Papa", "Dad", "Father", "Daddy", "Pitaji", "Buba", "Ba", "पापा", "पिताजी", "बुबा"))
      }
      "भाई", "दाजु", "भाइ", "brother", "bhai" -> {
        queryVariations.addAll(listOf("Bhai", "Brother", "Bro", "Daju", "Bhaiya", "भाई", "दाजु"))
      }
      "दीदी", "बहिन", "बहिनी", "sister", "didi" -> {
        queryVariations.addAll(listOf("Didi", "Sister", "Sis", "Bahin", "Bahini", "दीदी", "बहिन"))
      }
    }

    try {
      val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
      val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone._ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.TYPE
      )

      val cursor: Cursor? = contentResolver.query(
        uri,
        projection,
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
      )

      cursor?.use {
        val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
        val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val typeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)

        val seenNumbers = mutableSetOf<String>()

        while (it.moveToNext()) {
          val id = if (idIdx >= 0) it.getString(idIdx) else ""
          val name = if (nameIdx >= 0) it.getString(nameIdx) else ""
          val rawNumber = if (numberIdx >= 0) it.getString(numberIdx) else ""
          val number = rawNumber.replace("\\s+".toRegex(), "").replace("-", "")
          val typeVal = if (typeIdx >= 0) it.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
          val typeStr = when (typeVal) {
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
            else -> "Other"
          }

          if (number.isNotBlank() && !seenNumbers.contains(number)) {
            // Match against query variations (case-insensitive substring or exact match)
            val matched = queryVariations.any { q ->
              name.contains(q, ignoreCase = true) || q.contains(name, ignoreCase = true)
            }

            if (matched) {
              seenNumbers.add(number)
              contacts.add(
                ResolvedContact(
                  id = id,
                  name = name,
                  phoneNumber = rawNumber,
                  type = typeStr
                )
              )
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error querying contacts", e)
    }

    return contacts
  }
}
