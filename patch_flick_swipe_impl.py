import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Variables for state
    state_vars = """
    private var isCursorSwipeActive = false
    private var isDeleteSwipeActive = false
    private var swipePointerId = -1
"""
    if "private var isCursorSwipeActive = false" not in content:
        content = content.replace("    var isComposing: Boolean = false", "    var isComposing: Boolean = false" + state_vars)

    # In ACTION_DOWN:
    target_down = """            MotionEvent.ACTION_DOWN -> {
                motionTargets.clear()
                pointerDownTime.clear()

                pointerDownTime[pointerId] = event.downTime
                val targetView = findTargetView(
                    displayX = event.displayX(pointerIndex),
                    displayY = event.displayY(pointerIndex)
                )

                targetView?.let { target ->"""
    
    replacement_down = target_down + """
                    val keyData = (target.view as? com.kazumaproject.custom_keyboard.view.AutoSizeButton)?.keyData
                    isCursorSwipeActive = false
                    isDeleteSwipeActive = false
                    if (keyData != null) {
                        val isArrow = keyData.action is KeyAction.MoveCursorLeft || keyData.action is KeyAction.MoveCursorRight || keyData.action is KeyAction.MoveCursorUp || keyData.action is KeyAction.MoveCursorDown
                        if (isArrow && cursorKeySwipeMoveEnableProvider()) {
                            isCursorSwipeActive = true
                            swipePointerId = pointerId
                            cursorInitialX = event.x
                            cursorInitialY = event.y
                        } else if (keyData.action is KeyAction.Delete && !isComposing && deleteKeySwipeSelectionEnableProvider()) {
                            isDeleteSwipeActive = true
                            swipePointerId = pointerId
                            cursorInitialX = event.x
                            cursorInitialY = event.y
                            selectionDeleteHandler?.onStart()
                        }
                    }
"""
    if "isCursorSwipeActive = false" not in content:
        content = content.replace(target_down, replacement_down)

    # In ACTION_MOVE:
    target_move = """            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pId = event.getPointerId(i)
                    val target = motionTargets[pId]
                    val downTime = pointerDownTime[pId]"""
                    
    replacement_move = """            MotionEvent.ACTION_MOVE -> {
                if (isCursorSwipeActive && swipePointerId != -1) {
                    val idx = event.findPointerIndex(swipePointerId)
                    if (idx != -1) {
                        val currentX = event.x
                        val currentY = event.y
                        val dx = currentX - cursorInitialX
                        val dy = currentY - cursorInitialY
                        val threshold = 30f
                        if (kotlin.math.abs(dx) > kotlin.math.abs(dy) && kotlin.math.abs(dx) > threshold) {
                            val action2 = if (dx < 0f) KeyAction.MoveCursorLeft else KeyAction.MoveCursorRight
                            dispatchNonTapAction(action2, false)
                            cursorInitialX = currentX
                            cursorInitialY = currentY
                        } else if (kotlin.math.abs(dy) > kotlin.math.abs(dx) && kotlin.math.abs(dy) > threshold) {
                            val action2 = if (dy < 0f) KeyAction.MoveCursorUp else KeyAction.MoveCursorDown
                            dispatchNonTapAction(action2, false)
                            cursorInitialX = currentX
                            cursorInitialY = currentY
                        }
                        return true
                    }
                }
                if (isDeleteSwipeActive && swipePointerId != -1) {
                    val idx = event.findPointerIndex(swipePointerId)
                    if (idx != -1) {
                        val currentX = event.x
                        val currentY = event.y
                        selectionDeleteHandler?.onUpdate((currentX - cursorInitialX).toInt(), (currentY - cursorInitialY).toInt())
                        return true
                    }
                }
                for (i in 0 until event.pointerCount) {
                    val pId = event.getPointerId(i)
                    val target = motionTargets[pId]
                    val downTime = pointerDownTime[pId]"""
    
    if "if (isCursorSwipeActive" not in content:
        content = content.replace(target_move, replacement_move)

    # In ACTION_UP:
    target_up = """            MotionEvent.ACTION_POINTER_UP -> {"""
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
""" + target_up
    
    # Actually, there is already ACTION_UP handling!
    # Let's put it at the very beginning of onTouchEvent instead!
    pass

    with open(file_path, 'w') as f:
        f.write(content)

modify_file('custom_keyboard/src/main/java/com/kazumaproject/custom_keyboard/view/FlickKeyboardView.kt')
