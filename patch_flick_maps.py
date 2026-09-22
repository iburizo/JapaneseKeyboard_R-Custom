import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Find the twoStepFlickMaps initialization
    # We want to replace the `TfbiFlickDirection.UP` map for "あ" dynamically
    
    replacement = """
                        TfbiFlickDirection.UP to buildMap {
                            put(TfbiFlickDirection.TAP, "あ")
                            put(TfbiFlickDirection.UP, "う")
                            put(TfbiFlickDirection.UP_LEFT, "ぅ")
                            when (uExtensionModeProvider()) {
                                "up_right" -> put(TfbiFlickDirection.UP_RIGHT, "うう")
                                "right" -> {
                                    put(TfbiFlickDirection.RIGHT, "うう")
                                    put(TfbiFlickDirection.UP_RIGHT, "ゔ")
                                }
                                else -> put(TfbiFlickDirection.UP_RIGHT, "ゔ")
                            }
                        },
"""

    # There are three occurrences of this block (lines 5277, 6959, 8992)
    
    # Let's replace:
    target = """                        TfbiFlickDirection.UP to mapOf(
                            TfbiFlickDirection.TAP to "あ",
                            TfbiFlickDirection.UP to "う",
                            TfbiFlickDirection.UP_LEFT to "ぅ",
                            TfbiFlickDirection.UP_RIGHT to "ゔ"
                        ),"""

    content = content.replace(target, replacement)
    
    with open(file_path, 'w') as f:
        f.write(content)

modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/layout/KeyboardDefaultLayouts.kt')
