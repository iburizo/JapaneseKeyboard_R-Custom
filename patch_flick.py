import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Add var isComposing: Boolean = false
    if "var isComposing: Boolean = false" not in content:
        content = content.replace("class FlickKeyboardView", "class FlickKeyboardView") # just find where to put
        # Actually find `var listener: KeyboardListener? = null`
        target = "var listener: KeyboardListener? = null"
        replacement = target + "\n    var isComposing: Boolean = false"
        content = content.replace(target, replacement)
        
    # Replace direction.toSumireSpecialKeyDirectionOrNull()
    content = content.replace("direction.toSumireSpecialKeyDirectionOrNull()", "direction.toSumireSpecialKeyDirectionOrNull(isComposing)")
    
    with open(file_path, 'w') as f:
        f.write(content)

modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/view/FlickKeyboardView.kt')
modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/data/SumireSpecialKeyDisplayActionMap.kt')
modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/data/SumireSpecialKeyRuntimeActionDispatcher.kt')

