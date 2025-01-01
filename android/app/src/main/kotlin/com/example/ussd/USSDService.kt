package com.example.ussd

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * AccessibilityService object for USSD dialogs on Android mobile Telecoms.
 *
 * @author Romell Dominguez
 * @version 1.1.c 27/09/2018
 * @since 1.0.a
 */
class USSDService : AccessibilityService() {

    companion object {
        private var event: AccessibilityEvent? = null

        /**
         * Send whatever you want via USSD
         *
         * @param text any string
         */
        fun send(text: String) {
            event?.let {
                setTextIntoField(it, text)
                clickOnButton(it, 1)
            }
        }

        /**
         * Send text using specific event
         */
        fun send2(text: String, ev: AccessibilityEvent) {
            setTextIntoField(ev, text)
            clickOnButton(ev, 1)
        }

        /**
         * Dismiss dialog by using first button from USSD Dialog
         */
        fun cancel() {
            event?.let {
                clickOnButton(it, 0)
            }
        }

        /**
         * Dismiss dialog using specific event
         */
        fun cancel2(ev: AccessibilityEvent) {
            clickOnButton(ev, 0)
        }

        /**
         * Set text into input text field at USSD widget
         *
         * @param event AccessibilityEvent
         * @param data  Any String
         */
        private fun setTextIntoField(event: AccessibilityEvent, data: String) {
            val arguments = Bundle()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, data)
            }
            for (leaf in getLeaves(event)) {
                if (leaf.className == "android.widget.EditText"
                    && !leaf.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                ) {
                    val clipboardManager = (USSDController.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?)
                    clipboardManager?.setPrimaryClip(ClipData.newPlainText("text", data))
                    leaf.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                }
            }
        }

        /**
         * Method evaluate if USSD widget has input text
         *
         * @param event AccessibilityEvent
         * @return boolean has or not input text
         */
        private fun notInputText(event: AccessibilityEvent): Boolean {
            for (leaf in getLeaves(event)) if (leaf.className == "android.widget.EditText") return false
            return true
        }

        /**
         * Click a button using the index
         *
         * @param event AccessibilityEvent
         * @param index button's index
         */
        private fun clickOnButton(event: AccessibilityEvent, index: Int) {
            var count = -1
            for (leaf in getLeaves(event)) {
                count++
                if (count == index) {
                    leaf.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
//                if (leaf.className.toString().lowercase(Locale.getDefault()).contains("button")) {
//                }
            }
        }

        /**
         * The AccessibilityEvent is instance of USSD Widget class
         *
         * @param event AccessibilityEvent
         * @return boolean AccessibilityEvent is USSD
         */
        private fun isUSSDWidget(event: AccessibilityEvent): Boolean {
            return event.className == "amigo.app.AmigoAlertDialog"
                    || event.className == "android.app.AlertDialog"
                    || event.className == "com.android.phone.oppo.settings.LocalAlertDialog"
                    || event.className == "com.zte.mifavor.widget.AlertDialog"
                    || event.className == "color.support.v7.app.AlertDialog"
        }

        /**
         * The View has a login message into USSD Widget
         *
         * @param event AccessibilityEvent
         * @return boolean USSD Widget has login message
         */
        private fun loginView(event: AccessibilityEvent): Boolean {
            return isUSSDWidget(event) && USSDController.map[USSDController.KEY_LOGIN]
                ?.contains(event.text[0].toString()) == true
        }

        /**
         * The View has a problem message into USSD Widget
         *
         * @param event AccessibilityEvent
         * @return boolean USSD Widget has problem message
         */
        private fun problemView(event: AccessibilityEvent): Boolean {
            return isUSSDWidget(event) && USSDController.map[USSDController.KEY_ERROR]
                ?.contains(event.text[0].toString()) == true
        }

        /**
         * Get all leaf nodes from the event
         */
        private fun getLeaves(event: AccessibilityEvent): List<AccessibilityNodeInfo> {
            val leaves = mutableListOf<AccessibilityNodeInfo>()
            event.source?.let { getLeaves(leaves, it) }
            return leaves
        }

        private fun getLeaves(leaves: MutableList<AccessibilityNodeInfo>, node: AccessibilityNodeInfo) {
            if (node.childCount == 0) {
                leaves.add(node)
            } else {
                for (i in 0 until node.childCount) {
                    getLeaves(leaves, node.getChild(i))
                }
            }
        }
    }

    /**
     * Catch widget by Accessibility when it is showing on the mobile display
     *
     * @param event AccessibilityEvent
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        USSDService.event = event
        val ussd = USSDController

        if (ussd.isRunning != true) return

        var response: String? = null
        if (event.text.isNotEmpty()) {
            val res = event.text.toMutableList()
            res.remove("SEND")
            res.remove("CANCEL")
            response = res.joinToString("\n")
        }

        when {
            loginView(event) && notInputText(event) -> {
                // First view or logView, do nothing, pass / FIRST MESSAGE
                clickOnButton(event, 0)
                ussd.stopRunning()
                USSDController.callbackInvoke.over(response ?: "")
            }
            problemView(event) || loginView(event) -> {
                // Deal down
                clickOnButton(event, 1)
                USSDController.callbackInvoke.over(response ?: "")
            }
            isUSSDWidget(event) -> {
                if (notInputText(event)) {
                    // Not more input panels / LAST MESSAGE
                    // Sent 'OK' button
                    clickOnButton(event, 0)
                    ussd.stopRunning()
                    USSDController.callbackInvoke.over(response ?: "")
                } else {
                    // Sent option 1
                    if (ussd.sendType == true) {
                        ussd.callbackMessage?.invoke(event)
                    } else {
                        USSDController.callbackInvoke.responseInvoke(event)
                    }
                }
            }
        }
    }

    /**
     * Active when OS interrupts the application
     */
    override fun onInterrupt() {
        // Handle interrupt logic here if necessary
    }

    /**
     * Configure accessibility server from Android Operating System
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        // Handle service connection if needed
    }
}
