package com.kazumaproject.markdownhelperkeyboard.setting_activity.ui.setting

import android.os.Bundle
import androidx.preference.Preference
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

        applyLegacySearchResultFilterIfNeeded()
    }
}
