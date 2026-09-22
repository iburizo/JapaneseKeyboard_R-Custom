import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # We need to add branches for TAP_COMPOSING, etc.
    # Usually, if it's fallback or default, it maps to the same action as the non-composing ones.
    target1 = """SumireSpecialKeyDirection.TAP -> DefaultActions.Tap
            SumireSpecialKeyDirection.UP -> DefaultActions.Up
            SumireSpecialKeyDirection.RIGHT -> DefaultActions.Right
            SumireSpecialKeyDirection.DOWN -> DefaultActions.Down
            SumireSpecialKeyDirection.LEFT -> DefaultActions.Left"""
    replacement1 = """SumireSpecialKeyDirection.TAP, SumireSpecialKeyDirection.TAP_COMPOSING -> DefaultActions.Tap
            SumireSpecialKeyDirection.UP, SumireSpecialKeyDirection.UP_COMPOSING -> DefaultActions.Up
            SumireSpecialKeyDirection.RIGHT, SumireSpecialKeyDirection.RIGHT_COMPOSING -> DefaultActions.Right
            SumireSpecialKeyDirection.DOWN, SumireSpecialKeyDirection.DOWN_COMPOSING -> DefaultActions.Down
            SumireSpecialKeyDirection.LEFT, SumireSpecialKeyDirection.LEFT_COMPOSING -> DefaultActions.Left"""
    
    if target1 in content:
        content = content.replace(target1, replacement1)

    target2 = """SumireSpecialKeyDirection.TAP -> FlickDirection.TAP
            SumireSpecialKeyDirection.UP -> FlickDirection.UP
            SumireSpecialKeyDirection.RIGHT -> FlickDirection.UP_RIGHT_FAR
            SumireSpecialKeyDirection.DOWN -> FlickDirection.DOWN
            SumireSpecialKeyDirection.LEFT -> FlickDirection.UP_LEFT_FAR"""
    replacement2 = """SumireSpecialKeyDirection.TAP, SumireSpecialKeyDirection.TAP_COMPOSING -> FlickDirection.TAP
            SumireSpecialKeyDirection.UP, SumireSpecialKeyDirection.UP_COMPOSING -> FlickDirection.UP
            SumireSpecialKeyDirection.RIGHT, SumireSpecialKeyDirection.RIGHT_COMPOSING -> FlickDirection.UP_RIGHT_FAR
            SumireSpecialKeyDirection.DOWN, SumireSpecialKeyDirection.DOWN_COMPOSING -> FlickDirection.DOWN
            SumireSpecialKeyDirection.LEFT, SumireSpecialKeyDirection.LEFT_COMPOSING -> FlickDirection.UP_LEFT_FAR"""

    if target2 in content:
        content = content.replace(target2, replacement2)
        
    with open(file_path, 'w') as f:
        f.write(content)

modify_file('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/sumire_special_key/SumireSpecialKeyDefaultActionResolver.kt')
modify_file('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/sumire_special_key/SumireSpecialKeyPopupDisplayMapBuilder.kt')
