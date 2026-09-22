import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    new_prefs = """
        <SwitchPreferenceCompat
            android:key="cursor_key_swipe_move_preference"
            android:title="[CUSTOM] 矢印キースワイプ"
            app:defaultValue="true" />
            
        <SwitchPreferenceCompat
            android:key="delete_key_swipe_selection_preference"
            android:title="[CUSTOM] 削除キースワイプ"
            app:defaultValue="true" />
    """
    
    # insert before </PreferenceCategory>
    content = content.replace("</PreferenceScreen>", new_prefs + "\n</PreferenceScreen>")
    
    with open(file_path, 'w') as f:
        f.write(content)

modify_file('app/src/main/res/xml/pref_common_legacy.xml')
