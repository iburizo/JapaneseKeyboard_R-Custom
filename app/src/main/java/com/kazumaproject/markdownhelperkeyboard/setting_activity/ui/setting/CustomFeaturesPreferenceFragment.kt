package com.kazumaproject.markdownhelperkeyboard.setting_activity.ui.setting

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.kazumaproject.custom_keyboard.data.KeyAction
import com.kazumaproject.custom_keyboard.data.KeyActionMapper
import com.kazumaproject.markdownhelperkeyboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomFeaturesPreferenceFragment : AsyncPreferenceFragment() {
    override val preferencesXmlRes: Int = R.xml.pref_custom_features

    override fun onPreferencesReady(savedInstanceState: Bundle?, rootKey: String?) {
        findPreference<Preference>("hierarchical_flick_angle_margin_preference")?.apply {
            setOnPreferenceClickListener {
                navigateSafely(R.id.hierarchicalFlickAngleMarginFragment)
                true
            }
        }

        findPreference<Preference>("circular_slot_action_setting_preference")?.apply {
            setOnPreferenceClickListener {
                navigateSafely(R.id.circularSlotActionSettingFragment)
                true
            }
        }

        findPreference<Preference>("sumire_special_key_editor_preference")?.apply {
            setOnPreferenceClickListener {
                navigateSafely(R.id.sumireSpecialKeyEditorFragment)
                true
            }
        }

        setupMultiTouchGesturePreferences()

        applyLegacySearchResultFilterIfNeeded()
    }

    private fun setupMultiTouchGesturePreferences() {
        val ctx = context ?: return
        val rawActions = KeyActionMapper.getDisplayActions(ctx)

        val gestureActions = mutableListOf<Pair<String, String>>()
        val noneLabel = ctx.getString(R.string.multitouch_gesture_action_none)
        gestureActions.add(noneLabel to "none")

        val seenActionValues = mutableSetOf("none", "DoNothing")
        for (item in rawActions) {
            val action = item.action
            if (action is KeyAction.DoNothing) continue
            // 通常の文字入力（Text / InputText）はジェスチャとして不適切なため除外
            if (action is KeyAction.Text || action is KeyAction.InputText) continue
            // カスタムキーボード移動（空ID）も除外
            if (action is KeyAction.MoveToCustomKeyboard && action.stableId.isEmpty()) continue

            val actionValue = KeyActionMapper.fromKeyAction(action) ?: continue
            if (seenActionValues.add(actionValue)) {
                gestureActions.add(item.displayName to actionValue)
            }
        }

        val entries = gestureActions.map { it.first }.toTypedArray()
        val entryValues = gestureActions.map { it.second }.toTypedArray()

        val gesturePrefKeys = listOf(
            "gesture_two_finger_swipe_left_preference",
            "gesture_two_finger_swipe_right_preference",
            "gesture_two_finger_swipe_up_preference",
            "gesture_two_finger_swipe_down_preference",
            "gesture_two_finger_pinch_in_preference",
            "gesture_two_finger_pinch_out_preference"
        )

        for (prefKey in gesturePrefKeys) {
            val pref = findPreference<ListPreference>(prefKey) ?: continue
            pref.entries = entries
            pref.entryValues = entryValues

            val currentVal = pref.value
            val normalized = normalizeGestureActionValue(currentVal)
            if (normalized != null && normalized != currentVal && entryValues.contains(normalized)) {
                pref.value = normalized
            }
        }
    }

    private fun normalizeGestureActionValue(value: String?): String? = when (value) {
        "undo_commit" -> "UndoCommit"
        "undo" -> "Undo"
        "redo" -> "Redo"
        "select_all" -> "SelectAll"
        "copy" -> "Copy"
        "cut" -> "Cut"
        "paste" -> "Paste"
        "hide_keyboard" -> "HideKeyboard"
        "enter" -> "Enter"
        "space" -> "Space"
        else -> value
    }
}
