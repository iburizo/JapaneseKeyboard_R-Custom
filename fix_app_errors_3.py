import re

def fix(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    target = """        return when (this) {
            SumireSpecialKeyDirection.TAP -> listOf(FlickDirection.TAP)
            SumireSpecialKeyDirection.UP -> listOf(FlickDirection.UP)
            SumireSpecialKeyDirection.RIGHT -> buildList {
                add(FlickDirection.UP_RIGHT_FAR)
                if (FlickDirection.UP_RIGHT in existingKeys) add(FlickDirection.UP_RIGHT)
            }

            SumireSpecialKeyDirection.DOWN -> listOf(FlickDirection.DOWN)
            SumireSpecialKeyDirection.LEFT -> buildList {
                add(FlickDirection.UP_LEFT_FAR)
                if (FlickDirection.UP_LEFT in existingKeys) add(FlickDirection.UP_LEFT)
            }
        }"""
        
    replacement = """        return when (this) {
            SumireSpecialKeyDirection.TAP, SumireSpecialKeyDirection.TAP_COMPOSING -> listOf(FlickDirection.TAP)
            SumireSpecialKeyDirection.UP, SumireSpecialKeyDirection.UP_COMPOSING -> listOf(FlickDirection.UP)
            SumireSpecialKeyDirection.RIGHT, SumireSpecialKeyDirection.RIGHT_COMPOSING -> buildList {
                add(FlickDirection.UP_RIGHT_FAR)
                if (FlickDirection.UP_RIGHT in existingKeys) add(FlickDirection.UP_RIGHT)
            }

            SumireSpecialKeyDirection.DOWN, SumireSpecialKeyDirection.DOWN_COMPOSING -> listOf(FlickDirection.DOWN)
            SumireSpecialKeyDirection.LEFT, SumireSpecialKeyDirection.LEFT_COMPOSING -> buildList {
                add(FlickDirection.UP_LEFT_FAR)
                if (FlickDirection.UP_LEFT in existingKeys) add(FlickDirection.UP_LEFT)
            }
        }"""
        
    content = content.replace(target, replacement)
    with open(file_path, 'w') as f:
        f.write(content)

fix('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/sumire_special_key/SumireSpecialKeyPopupDisplayMapBuilder.kt')
