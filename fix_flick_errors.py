import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # 1. line 1662 buildSumireSpecialKeyDisplayActionMap -> pass this@FlickKeyboardView.isComposing
    target1 = """buildSumireSpecialKeyDisplayActionMap(
                                                keyData,
                                                actionMap,
                                                isComposing,
                                                this@FlickKeyboardView.resolveSumireSpecialKeyOverride
                                            )"""
    replacement1 = """buildSumireSpecialKeyDisplayActionMap(
                                                keyData,
                                                actionMap,
                                                this@FlickKeyboardView.isComposing,
                                                this@FlickKeyboardView.resolveSumireSpecialKeyOverride
                                            )"""
    content = content.replace(target1, replacement1)

    target1_1 = """buildSumireSpecialKeyDisplayActionMap(keyData, flickActionMap, isComposing) {"""
    replacement1_1 = """buildSumireSpecialKeyDisplayActionMap(keyData, flickActionMap, this@FlickKeyboardView.isComposing) {"""
    content = content.replace(target1_1, replacement1_1)

    # 2. line 1684 direction.toSumireSpecialKeyDirectionOrNull(isComposing) -> this@FlickKeyboardView.isComposing
    target2 = "direction.toSumireSpecialKeyDirectionOrNull(isComposing)"
    replacement2 = "direction.toSumireSpecialKeyDirectionOrNull(this@FlickKeyboardView.isComposing)"
    content = content.replace(target2, replacement2)

    # 3. line 1728
    target3 = """isComposing = isComposing,"""
    replacement3 = """isComposing = this@FlickKeyboardView.isComposing,"""
    content = content.replace(target3, replacement3)

    with open(file_path, 'w') as f:
        f.write(content)

modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/view/FlickKeyboardView.kt')
