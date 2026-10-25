package com.kazumaproject.markdownhelperkeyboard.setting_activity

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kazumaproject.markdownhelperkeyboard.R
import com.kazumaproject.markdownhelperkeyboard.setting_activity.ui.setting.SettingDestinations
import com.kazumaproject.markdownhelperkeyboard.setting_activity.ui.setting.SettingSearchIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CustomFeaturesPreferenceTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun preferenceKeys(xmlRes: Int): Set<String> {
        val parser = context.resources.getXml(xmlRes)
        val keys = mutableSetOf<String>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                for (i in 0 until parser.attributeCount) {
                    if (parser.getAttributeName(i) == "key") {
                        keys.add(parser.getAttributeValue(i))
                    }
                }
            }
            eventType = parser.next()
        }
        return keys
    }

    @Test
    fun testCustomFeaturesXmlContainsAllCustomKeys() {
        val keys = preferenceKeys(R.xml.pref_custom_features)
        assertTrue(keys.contains("cursor_key_swipe_move_enable_preference"))
        assertTrue(keys.contains("delete_key_swipe_selection_enable_preference"))
        assertTrue(keys.contains("sumire_u_extension_mode_preference"))
        assertTrue(keys.contains("hierarchical_flick_transition_vibration_preference"))
        assertTrue(keys.contains("hierarchical_flick_angle_margin_preference"))
        assertTrue(keys.contains("circular_slot_action_setting_preference"))
        assertTrue(keys.contains("sumire_special_key_editor_preference"))
        assertTrue(keys.contains("gesture_two_finger_swipe_left_preference"))
        assertTrue(keys.contains("gesture_two_finger_swipe_right_preference"))
        assertTrue(keys.contains("gesture_two_finger_swipe_up_preference"))
        assertTrue(keys.contains("gesture_two_finger_swipe_down_preference"))
        assertTrue(keys.contains("gesture_two_finger_pinch_in_preference"))
        assertTrue(keys.contains("gesture_two_finger_pinch_out_preference"))
    }

    @Test
    fun testSumireXmlDoesNotContainMigratedCustomKeys() {
        val sumireKeys = preferenceKeys(R.xml.pref_sumire)
        assertFalse(sumireKeys.contains("sumire_u_extension_mode_preference"))
        assertFalse(sumireKeys.contains("hierarchical_flick_angle_margin_preference"))
        assertFalse(sumireKeys.contains("circular_slot_action_setting_preference"))
        assertFalse(sumireKeys.contains("sumire_special_key_editor_preference"))
    }

    @Test
    fun testCustomFeaturesDestinationRegisteredInTopCategories() {
        val categories = SettingDestinations.categories(context)
        val customRoute = categories.find { it.key == "setting_route_custom_features" }
        assertTrue("setting_route_custom_features must be in categories", customRoute != null)
        assertTrue(customRoute!!.title.contains("CUSTOM"))
    }

    @Test
    fun testCustomFeaturesSearchIndexRegistered() {
        val destinations = SettingSearchIndex.searchable(context)
        val customPref = destinations.find { it.key == "hierarchical_flick_transition_vibration_preference" }
        assertTrue("hierarchical_flick_transition_vibration_preference must be in searchable destinations", customPref != null)
    }

    @Test
    fun testMultiTouchGestureActionEntriesAndValuesContainEnterAndSpecialKeys() {
        val entries = context.resources.getStringArray(R.array.multitouch_gesture_action_entries)
        val values = context.resources.getStringArray(R.array.multitouch_gesture_action_values)

        assertEquals("Entries and values array length must match", entries.size, values.size)
        assertTrue("Values must contain Enter", values.contains("Enter"))
        assertTrue("Values must contain Space", values.contains("Space"))
        assertTrue("Values must contain Delete", values.contains("Delete"))
        assertTrue("Values must contain UndoCommit", values.contains("UndoCommit"))
        assertTrue("Values must contain none", values.contains("none"))
    }

    @Test
    fun testSpecialKeysCanBeMappedToKeyActionForGesture() {
        val enterAction = com.kazumaproject.custom_keyboard.data.KeyActionMapper.toKeyAction("Enter")
        assertEquals(com.kazumaproject.custom_keyboard.data.KeyAction.Enter, enterAction)

        val spaceAction = com.kazumaproject.custom_keyboard.data.KeyActionMapper.toKeyAction("Space")
        assertEquals(com.kazumaproject.custom_keyboard.data.KeyAction.Space, spaceAction)

        val undoCommitAction = com.kazumaproject.custom_keyboard.data.KeyActionMapper.toKeyAction("UndoCommit")
        assertEquals(com.kazumaproject.custom_keyboard.data.KeyAction.UndoCommit, undoCommitAction)
    }
}
