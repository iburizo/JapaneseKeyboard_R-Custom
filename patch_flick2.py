import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # 1. line 1662: buildSumireSpecialKeyDisplayActionMap(keyData, flickActionMap) { data, direction ->
    target1 = "buildSumireSpecialKeyDisplayActionMap(keyData, flickActionMap) {"
    replacement1 = "buildSumireSpecialKeyDisplayActionMap(keyData, flickActionMap, isComposing) {"
    content = content.replace(target1, replacement1)

    # Also there might be multi-line versions
    target2 = """buildSumireSpecialKeyDisplayActionMap(keyData, flickActionMap) { data, direction ->"""
    replacement2 = """buildSumireSpecialKeyDisplayActionMap(keyData, flickActionMap, isComposing) { data, direction ->"""
    content = content.replace(target2, replacement2)

    # 2. dispatchSumireSpecialKeyRuntimeAction
    target3 = """dispatchSumireSpecialKeyRuntimeAction(
                                    keyData = keyData,
                                    flickDirection = direction,
                                    fallbackAction = fallbackAction,
                                    isFlick = isFlick,
                                    resolve = ::resolveSumireSpecialKeyOverride
                                ) { dispatchedAction, actionIsFlick ->"""
    replacement3 = """dispatchSumireSpecialKeyRuntimeAction(
                                    keyData = keyData,
                                    flickDirection = direction,
                                    fallbackAction = fallbackAction,
                                    isFlick = isFlick,
                                    isComposing = isComposing,
                                    resolve = ::resolveSumireSpecialKeyOverride
                                ) { dispatchedAction, actionIsFlick ->"""
    content = content.replace(target3, replacement3)
    
    target4 = """dispatchSumireSpecialKeyRuntimeAction(
                                            keyData = keyData,
                                            flickDirection = direction,
                                            fallbackAction = fallbackAction,
                                            isFlick = true,
                                            resolve = ::resolveSumireSpecialKeyOverride
                                        ) { dispatchedAction, actionIsFlick ->"""
    replacement4 = """dispatchSumireSpecialKeyRuntimeAction(
                                            keyData = keyData,
                                            flickDirection = direction,
                                            fallbackAction = fallbackAction,
                                            isFlick = true,
                                            isComposing = isComposing,
                                            resolve = ::resolveSumireSpecialKeyOverride
                                        ) { dispatchedAction, actionIsFlick ->"""
    content = content.replace(target4, replacement4)
    
    target5 = """dispatchSumireSpecialKeyRuntimeAction(
                                    keyData = keyData,
                                    flickDirection = direction,
                                    fallbackAction = action,
                                    isFlick = direction != FlickDirection.TAP,
                                    resolve = ::resolveSumireSpecialKeyOverride
                                ) { dispatchedAction, actionIsFlick ->"""
    replacement5 = """dispatchSumireSpecialKeyRuntimeAction(
                                    keyData = keyData,
                                    flickDirection = direction,
                                    fallbackAction = action,
                                    isFlick = direction != FlickDirection.TAP,
                                    isComposing = isComposing,
                                    resolve = ::resolveSumireSpecialKeyOverride
                                ) { dispatchedAction, actionIsFlick ->"""
    content = content.replace(target5, replacement5)

    with open(file_path, 'w') as f:
        f.write(content)

modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/view/FlickKeyboardView.kt')
