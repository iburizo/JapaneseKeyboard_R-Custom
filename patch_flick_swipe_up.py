import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Search for ACTION_UP
    target_up = """            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isCursorMode) {"""
    
    replacement_up = """            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDeleteSwipeActive && swipePointerId != -1) {
                    selectionDeleteHandler?.onCommit()
                    isDeleteSwipeActive = false
                    swipePointerId = -1
                    motionTargets.clear()
                    return true
                }
                if (isCursorSwipeActive && swipePointerId != -1) {
                    isCursorSwipeActive = false
                    swipePointerId = -1
                    motionTargets.clear()
                    return true
                }
                if (isCursorMode) {"""

    if "if (isDeleteSwipeActive" not in content and target_up in content:
        content = content.replace(target_up, replacement_up)

    with open(file_path, 'w') as f:
        f.write(content)

modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/view/FlickKeyboardView.kt')
