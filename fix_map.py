import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    target = """private fun SumireSpecialKeyDirection.toDisplayFlickDirection(): FlickDirection {
    return when (this) {
        SumireSpecialKeyDirection.TAP -> FlickDirection.TAP
        SumireSpecialKeyDirection.UP -> FlickDirection.UP
        SumireSpecialKeyDirection.RIGHT -> FlickDirection.UP_RIGHT_FAR
        SumireSpecialKeyDirection.DOWN -> FlickDirection.DOWN
        SumireSpecialKeyDirection.LEFT -> FlickDirection.UP_LEFT_FAR
    }
}"""
    
    replacement = """private fun SumireSpecialKeyDirection.toDisplayFlickDirection(): FlickDirection {
    return when (this) {
        SumireSpecialKeyDirection.TAP, SumireSpecialKeyDirection.TAP_COMPOSING -> FlickDirection.TAP
        SumireSpecialKeyDirection.UP, SumireSpecialKeyDirection.UP_COMPOSING -> FlickDirection.UP
        SumireSpecialKeyDirection.RIGHT, SumireSpecialKeyDirection.RIGHT_COMPOSING -> FlickDirection.UP_RIGHT_FAR
        SumireSpecialKeyDirection.DOWN, SumireSpecialKeyDirection.DOWN_COMPOSING -> FlickDirection.DOWN
        SumireSpecialKeyDirection.LEFT, SumireSpecialKeyDirection.LEFT_COMPOSING -> FlickDirection.UP_LEFT_FAR
    }
}"""
    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, 'w') as f:
            f.write(content)
        print("Fixed toDisplayFlickDirection")

modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/data/SumireSpecialKeyDisplayActionMap.kt')

def fix_view(file_path):
    with open(file_path, 'r') as f:
        content = f.read()
    
    target1 = """direction.toSumireSpecialKeyDirectionOrNull(isComposing)"""
    # Look for calls missing isComposing. Let's just fix it.
    
    import re
    # We replaced all, but wait, there was a compile error
    # FlickKeyboardView.kt:1662:72 No value passed for parameter 'isComposing'
    # FlickKeyboardView.kt:1684:85 Unresolved reference: isComposing
    # FlickKeyboardView.kt:1728:37 No value passed for parameter 'isComposing'
    pass

fix_view('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/view/FlickKeyboardView.kt')
