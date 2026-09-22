import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # 1. getDisplayActions
    target1 = """            DisplayAction(
                KeyAction.Text("っ"),
                "促音 (っ)",
                com.kazumaproject.core.R.drawable.sokuon_icon
            )"""
    replacement1 = """            DisplayAction(
                KeyAction.Text("っ"),
                "促音 (っ)",
                com.kazumaproject.core.R.drawable.sokuon_icon
            ),
            DisplayAction(
                KeyAction.UndoCommit,
                "確定戻し",
                com.kazumaproject.core.R.drawable.undo_24px
            ),
            DisplayAction(
                KeyAction.CommitDialNumbers,
                "ア段数字変換確定",
                com.kazumaproject.core.R.drawable.input_mode_number_select_custom
            )"""
    content = content.replace(target1, replacement1)

    # 2. iconResIdForAction
    target2 = """            KeyAction.Text("っ") -> com.kazumaproject.core.R.drawable.sokuon_icon"""
    replacement2 = """            KeyAction.Text("っ") -> com.kazumaproject.core.R.drawable.sokuon_icon
            KeyAction.UndoCommit -> com.kazumaproject.core.R.drawable.undo_24px
            KeyAction.CommitDialNumbers -> com.kazumaproject.core.R.drawable.input_mode_number_select_custom"""
    content = content.replace(target2, replacement2)

    # 3. fromKeyAction
    target3 = """            is KeyAction.ForceFullWidthSpace -> "ForceFullWidthSpace\""""
    replacement3 = """            is KeyAction.ForceFullWidthSpace -> "ForceFullWidthSpace"
            is KeyAction.UndoCommit -> "UndoCommit"
            is KeyAction.CommitDialNumbers -> "CommitDialNumbers\""""
    content = content.replace(target3, replacement3)

    # 4. toKeyAction
    target4 = """            "ForceFullWidthSpace" -> KeyAction.ForceFullWidthSpace"""
    replacement4 = """            "ForceFullWidthSpace" -> KeyAction.ForceFullWidthSpace
            "UndoCommit" -> KeyAction.UndoCommit
            "CommitDialNumbers" -> KeyAction.CommitDialNumbers"""
    content = content.replace(target4, replacement4)

    with open(file_path, 'w') as f:
        f.write(content)

modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/data/KeyActionMapper.kt')
