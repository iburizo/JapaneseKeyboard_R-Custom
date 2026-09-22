import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    target = """private fun configureFlickKeyboardView(
        flickView: FlickKeyboardView,
        mainView: MainLayoutBinding,
        isFloatingView: Boolean
    ) {"""
    
    replacement = """private fun configureFlickKeyboardView(
        flickView: FlickKeyboardView,
        mainView: MainLayoutBinding,
        isFloatingView: Boolean
    ) {
        flickView.cursorKeySwipeMoveEnableProvider = { appPreference.cursor_key_swipe_move_preference }
        flickView.deleteKeySwipeSelectionEnableProvider = { appPreference.delete_key_swipe_selection_preference }
        com.kazumaproject.markdownhelperkeyboard.ime_service.SelectionDeleteBinding(flickView) { currentInputConnection }.bind()
"""

    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, 'w') as f:
            f.write(content)
        print("Patched IMEService.kt for configureFlickKeyboardView")
    else:
        print("Could not find configureFlickKeyboardView in IMEService.kt")

modify_file('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/ime_service/IMEService.kt')
