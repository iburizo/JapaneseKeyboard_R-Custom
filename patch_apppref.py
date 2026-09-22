import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    new_prefs = """
    var cursor_key_swipe_move_preference: Boolean
        get() = preferences.getBoolean("cursor_key_swipe_move_preference", true)
        set(value) = preferences.edit { it.putBoolean("cursor_key_swipe_move_preference", value) }

    var delete_key_swipe_selection_preference: Boolean
        get() = preferences.getBoolean("delete_key_swipe_selection_preference", true)
        set(value) = preferences.edit { it.putBoolean("delete_key_swipe_selection_preference", value) }
"""
    # Find a good place to insert, e.g. before "fun init("
    content = content.replace("    fun init(context: Context)", new_prefs + "\n    fun init(context: Context)")
    
    with open(file_path, 'w') as f:
        f.write(content)

modify_file('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/setting_activity/AppPreference.kt')
