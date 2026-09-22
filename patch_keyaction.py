import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Find sealed class KeyAction
    if "data object UndoCommit : KeyAction()" not in content:
        target = "    data class InputText(val text: String) : KeyAction()"
        replacement = target + "\n    data object UndoCommit : KeyAction()\n    data object CommitDialNumbers : KeyAction()"
        content = content.replace(target, replacement)
        
    with open(file_path, 'w') as f:
        f.write(content)

modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/data/KeyModels.kt')
