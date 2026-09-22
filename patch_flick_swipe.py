import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    new_props = """
    var cursorKeySwipeMoveEnableProvider: () -> Boolean = { false }
    var deleteKeySwipeSelectionEnableProvider: () -> Boolean = { false }

    interface SelectionDeleteHandler {
        fun onStart()
        fun onUpdate(dx: Int, dy: Int)
        fun onCommit()
    }
    var selectionDeleteHandler: SelectionDeleteHandler? = null
"""

    if "interface SelectionDeleteHandler" not in content:
        # Insert inside class FlickKeyboardView, near the top
        target = "    var isComposing: Boolean = false"
        replacement = target + new_props
        content = content.replace(target, replacement)
        
    with open(file_path, 'w') as f:
        f.write(content)

modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/view/FlickKeyboardView.kt')
