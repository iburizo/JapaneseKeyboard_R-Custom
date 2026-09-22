import re

def fix(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    target = """            SumireSpecialKeyDirection.TAP -> listOf(FlickDirection.TAP)
            SumireSpecialKeyDirection.UP -> listOf(FlickDirection.UP)
            SumireSpecialKeyDirection.RIGHT -> listOf(
                FlickDirection.UP_RIGHT_FAR,
                FlickDirection.UP_RIGHT
            )

            SumireSpecialKeyDirection.DOWN -> listOf(FlickDirection.DOWN)
            SumireSpecialKeyDirection.LEFT -> listOf(
                FlickDirection.UP_LEFT_FAR,
                FlickDirection.UP_LEFT
            )"""
    replacement = """            SumireSpecialKeyDirection.TAP, SumireSpecialKeyDirection.TAP_COMPOSING -> listOf(FlickDirection.TAP)
            SumireSpecialKeyDirection.UP, SumireSpecialKeyDirection.UP_COMPOSING -> listOf(FlickDirection.UP)
            SumireSpecialKeyDirection.RIGHT, SumireSpecialKeyDirection.RIGHT_COMPOSING -> listOf(
                FlickDirection.UP_RIGHT_FAR,
                FlickDirection.UP_RIGHT
            )

            SumireSpecialKeyDirection.DOWN, SumireSpecialKeyDirection.DOWN_COMPOSING -> listOf(FlickDirection.DOWN)
            SumireSpecialKeyDirection.LEFT, SumireSpecialKeyDirection.LEFT_COMPOSING -> listOf(
                FlickDirection.UP_LEFT_FAR,
                FlickDirection.UP_LEFT
            )"""
    content = content.replace(target, replacement)
    with open(file_path, 'w') as f:
        f.write(content)

fix('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/sumire_special_key/SumireSpecialKeyDefaultActionResolver.kt')
fix('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/sumire_special_key/SumireSpecialKeyPopupDisplayMapBuilder.kt')
