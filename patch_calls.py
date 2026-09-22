import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # 1. buildSumireSpecialKeyDisplayActionMap
    # Search for buildSumireSpecialKeyDisplayActionMap(
    #    actionMap,
    #    baseMap,
    #    resolve = this@FlickKeyboardView.resolveSumireSpecialKeyOverride
    # ) or similar
    
    target_display = "buildSumireSpecialKeyDisplayActionMap(keyData, actionMap, this@FlickKeyboardView.resolveSumireSpecialKeyOverride)"
    replacement_display = "buildSumireSpecialKeyDisplayActionMap(keyData, actionMap, isComposing, this@FlickKeyboardView.resolveSumireSpecialKeyOverride)"
    content = content.replace(target_display, replacement_display)

    # If it spans multiple lines:
    target_display2 = """buildSumireSpecialKeyDisplayActionMap(
                                                keyData,
                                                actionMap,
                                                this@FlickKeyboardView.resolveSumireSpecialKeyOverride
                                            )"""
    replacement_display2 = """buildSumireSpecialKeyDisplayActionMap(
                                                keyData,
                                                actionMap,
                                                isComposing,
                                                this@FlickKeyboardView.resolveSumireSpecialKeyOverride
                                            )"""
    content = content.replace(target_display2, replacement_display2)

    # 2. dispatchSumireSpecialKeyRuntimeAction
    target_dispatch = """dispatchSumireSpecialKeyRuntimeAction(
                                                keyData,
                                                direction,
                                                fallbackAction = fallbackAction,
                                                isFlick = true,
                                                resolve = this@FlickKeyboardView.resolveSumireSpecialKeyOverride,
                                                dispatch = { dispatchedAction, dispatchedIsFlick ->
                                                    this@FlickKeyboardView.listener?.onPress(dispatchedAction)
                                                }
                                            )"""
    replacement_dispatch = """dispatchSumireSpecialKeyRuntimeAction(
                                                keyData,
                                                direction,
                                                fallbackAction = fallbackAction,
                                                isFlick = true,
                                                isComposing = isComposing,
                                                resolve = this@FlickKeyboardView.resolveSumireSpecialKeyOverride,
                                                dispatch = { dispatchedAction, dispatchedIsFlick ->
                                                    this@FlickKeyboardView.listener?.onPress(dispatchedAction)
                                                }
                                            )"""
    content = content.replace(target_dispatch, replacement_dispatch)

    # Another call in onPress
    target_dispatch2 = """dispatchSumireSpecialKeyRuntimeAction(
                            keyData,
                            direction,
                            fallbackAction = action,
                            isFlick = direction != FlickDirection.TAP,
                            resolve = this@FlickKeyboardView.resolveSumireSpecialKeyOverride,
                            dispatch = { dispatchedAction, dispatchedIsFlick ->
                                this@FlickKeyboardView.listener?.onPress(dispatchedAction)
                            }
                        )"""
    replacement_dispatch2 = """dispatchSumireSpecialKeyRuntimeAction(
                            keyData,
                            direction,
                            fallbackAction = action,
                            isFlick = direction != FlickDirection.TAP,
                            isComposing = isComposing,
                            resolve = this@FlickKeyboardView.resolveSumireSpecialKeyOverride,
                            dispatch = { dispatchedAction, dispatchedIsFlick ->
                                this@FlickKeyboardView.listener?.onPress(dispatchedAction)
                            }
                        )"""
    content = content.replace(target_dispatch2, replacement_dispatch2)

    with open(file_path, 'w') as f:
        f.write(content)

modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/view/FlickKeyboardView.kt')
