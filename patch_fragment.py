import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # 1. Update displayLabel
    target_label = """private fun SumireSpecialKeyDirection.displayLabel(): String {
        return when (this) {
            SumireSpecialKeyDirection.TAP -> "Tap"
            SumireSpecialKeyDirection.UP -> "上フリック"
            SumireSpecialKeyDirection.RIGHT -> "右フリック"
            SumireSpecialKeyDirection.DOWN -> "下フリック"
            SumireSpecialKeyDirection.LEFT -> "左フリック"
        }
    }"""
    replacement_label = """private fun SumireSpecialKeyDirection.displayLabel(): String {
        return when (this) {
            SumireSpecialKeyDirection.TAP -> "Tap"
            SumireSpecialKeyDirection.UP -> "上フリック"
            SumireSpecialKeyDirection.RIGHT -> "右フリック"
            SumireSpecialKeyDirection.DOWN -> "下フリック"
            SumireSpecialKeyDirection.LEFT -> "左フリック"
            SumireSpecialKeyDirection.TAP_COMPOSING -> "Tap (入力中)"
            SumireSpecialKeyDirection.UP_COMPOSING -> "上フリック (入力中)"
            SumireSpecialKeyDirection.RIGHT_COMPOSING -> "右フリック (入力中)"
            SumireSpecialKeyDirection.DOWN_COMPOSING -> "下フリック (入力中)"
            SumireSpecialKeyDirection.LEFT_COMPOSING -> "左フリック (入力中)"
        }
    }"""
    if target_label in content:
        content = content.replace(target_label, replacement_label)

    # 2. Update dialogTitle
    target_title = """private fun SumireSpecialKeyDirection.dialogTitle(): String {
        return when (this) {
            SumireSpecialKeyDirection.TAP -> getString(R.string.sumire_special_key_dialog_title_tap)
            SumireSpecialKeyDirection.UP -> getString(R.string.sumire_special_key_dialog_title_up)
            SumireSpecialKeyDirection.RIGHT -> getString(R.string.sumire_special_key_dialog_title_right)
            SumireSpecialKeyDirection.DOWN -> getString(R.string.sumire_special_key_dialog_title_down)
            SumireSpecialKeyDirection.LEFT -> getString(R.string.sumire_special_key_dialog_title_left)
        }
    }"""
    replacement_title = """private fun SumireSpecialKeyDirection.dialogTitle(): String {
        return when (this) {
            SumireSpecialKeyDirection.TAP -> getString(R.string.sumire_special_key_dialog_title_tap)
            SumireSpecialKeyDirection.UP -> getString(R.string.sumire_special_key_dialog_title_up)
            SumireSpecialKeyDirection.RIGHT -> getString(R.string.sumire_special_key_dialog_title_right)
            SumireSpecialKeyDirection.DOWN -> getString(R.string.sumire_special_key_dialog_title_down)
            SumireSpecialKeyDirection.LEFT -> getString(R.string.sumire_special_key_dialog_title_left)
            SumireSpecialKeyDirection.TAP_COMPOSING -> getString(R.string.sumire_special_key_dialog_title_tap) + " (入力中)"
            SumireSpecialKeyDirection.UP_COMPOSING -> getString(R.string.sumire_special_key_dialog_title_up) + " (入力中)"
            SumireSpecialKeyDirection.RIGHT_COMPOSING -> getString(R.string.sumire_special_key_dialog_title_right) + " (入力中)"
            SumireSpecialKeyDirection.DOWN_COMPOSING -> getString(R.string.sumire_special_key_dialog_title_down) + " (入力中)"
            SumireSpecialKeyDirection.LEFT_COMPOSING -> getString(R.string.sumire_special_key_dialog_title_left) + " (入力中)"
        }
    }"""
    if target_title in content:
        content = content.replace(target_title, replacement_title)

    with open(file_path, 'w') as f:
        f.write(content)

modify_file('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/sumire_special_key/ui/SumireSpecialKeyActionEditorFragment.kt')
