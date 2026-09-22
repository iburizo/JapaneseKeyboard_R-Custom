import re

def modify_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Find KeyAction.ForceNewLine -> {
    # and we can insert before it
    target = "KeyAction.ForceNewLine -> {"
    replacement = """KeyAction.UndoCommit -> {
                        performPendingReconversion()
                    }
                    KeyAction.CommitDialNumbers -> {
                        val str = inputString.value
                        if (str.isNotEmpty()) {
                            val converted = com.kazumaproject.markdownhelperkeyboard.converter.utility.DialTimeDateConverter.generateCandidates(str).firstOrNull() ?: com.kazumaproject.markdownhelperkeyboard.converter.utility.DialTimeDateConverter.convertAdanToNumber(str)
                            commitAndClearInput(mainView, converted)
                        }
                    }
                    """ + target
                    
    content = content.replace(target, replacement)
    
    with open(file_path, 'w') as f:
        f.write(content)

modify_file('app/src/main/java/com/kazumaproject/markdownhelperkeyboard/ime_service/IMEService.kt')
